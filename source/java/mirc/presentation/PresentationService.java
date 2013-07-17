/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.presentation;

import java.io.File;
import java.net.URL;
import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mirc.activity.ActivityDB;
import mirc.MircConfig;
import mirc.ssadmin.StorageServiceAdmin;
import mirc.storage.AccessLog;
import mirc.util.MircDocument;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

/**
 * A Servlet to export local MIRCdocuments as a presentation.
 */
public class PresentationService extends Servlet {

	static final Logger logger = Logger.getLogger(PresentationService.class);

	int idCount = 0;

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a PresentationService.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public PresentationService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a presentation containing the MIRCdocuments corresponding to a list of URLs.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		String urlsParam = req.getParameter("urls");

		if (urlsParam != null) {

			MircConfig mc = MircConfig.getInstance();
			File mircRoot = mc.getRootDirectory();
			User user = req.getUser();
			String username = (user != null) ? user.getUsername() : null;

			//Make a directory in which to play
			File dir = mc.createTempDirectory();

			//Get a file for the presentation (in its own directory)
			File odpFileDir = mc.createTempDirectory();
			File odpFile = File.createTempFile("Presentation-", ".odp", odpFileDir);

			//Set up an XML document for capture the images
			File pictures = new File(dir, "Pictures");
			pictures.mkdirs();
			Document imagesDoc = XmlUtil.getDocument();
			Element images = imagesDoc.createElement("images");
			imagesDoc.appendChild(images);

			//Set up an XML document to capture the MIRCdocuments
			Document mdsDoc = XmlUtil.getDocument();
			Element mds = mdsDoc.createElement("MIRCdocuments");
			mdsDoc.appendChild(mds);

			//Process the MIRCdocuments and add in the exportable ones.
			String[] urls = urlsParam.split("\\|");
			for (String url : urls) {
				if (mc.isLocal(url)) {
					int k = url.indexOf("/storage/");
					if (k != -1) {
						url = url.substring(k+1);
						Path path = new Path(url);
						String ssid = path.element(1);
						url.replace("/", File.separator);
						int q = url.indexOf("?");
						if (q >= 0) url = url.substring(0, q);
						File mdFile = new File(mircRoot, url);
						MircDocument md = new MircDocument(mdFile);
						if (md.authorizes("export", user)) {
							addMircDocument(md, images, mds, pictures);
							AccessLog.logAccess(req, md.getXML());
							ActivityDB.getInstance().increment(ssid, "slides", username);
						}
					}
				}
			}

			//Add in the little png that OO needs for one of its styles
			String pngName = "10000000000000200000002000309F1C.png";
			File png = new File(pictures, pngName);
			FileUtil.getFile(png, "/odp/"+pngName);

			//Copy in the styles
			FileUtil.getFile(new File(dir, "styles.xml"), "/odp/styles.xml");

			//Make the META-INF directory and create the manifest.xml file
			File metaInf = new File(dir, "META-INF");
			metaInf.mkdirs();
			File manifestFile = new File(metaInf, "manifest.xml");
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/odp/manifest.xsl" ) );
			Document manifest = XmlUtil.getTransformedDocument( imagesDoc, xsl, null );
			FileUtil.setText(manifestFile, XmlUtil.toString(manifest));

			//Process the Document and create the slides file
			File contentFile = new File(dir, "content.xml");
			xsl = XmlUtil.getDocument( FileUtil.getStream( "/odp/multi-document-content.xsl" ) );
			Object[] params = {
				"images", imagesDoc,
				"username", ((user!=null) ? user.getUsername() : "")
			};
			Document content = XmlUtil.getTransformedDocument( mdsDoc, xsl, params );
			FileUtil.setText(contentFile, XmlUtil.toString(content));

			//Now zip it all up. Note that we suppress the name of the dir.
			//If we didn't, neither OO nor PPT would open the file.
			FileUtil.zipDirectory(dir, odpFile, true);

			res.write(odpFile);
			res.setContentType("pptx");
			res.setContentDisposition(odpFile);
			//NOTE: Do not disable caching; otherwise, the download will
			//fail because the browser won't be able to store the file;

			res.send();
			odpFile.delete();
			FileUtil.deleteAll(dir);
			FileUtil.deleteAll(odpFileDir);
			return;
		}

		//Unable.
		res.setResponseCode( res.forbidden );
		res.send();
	}

	public void addMircDocument(MircDocument md, Element images, Element mds, File pictures) throws Exception {

		Document doc = md.getXML();
		NodeList nl = doc.getDocumentElement().getElementsByTagName("image");
		for (int i=0; i<nl.getLength(); i++) {
			Element img = (Element)nl.item(i);
			String src = img.getAttribute("src");
			if (!src.startsWith("/") && !src.toLowerCase().startsWith("http://")) {

				String id = getID();
				img.setAttribute("id", id);

				//Check whether there is an original-dimensions version
				NodeList alt = img.getElementsByTagName("alternative-image");
				for (int k=0; k<alt.getLength(); k++) {
					Element altimg = (Element)alt.item(k);
					if (altimg.getAttribute("role").equals("original-dimensions")) {
						String altsrc = altimg.getAttribute("src").toLowerCase();
						if (altsrc.endsWith(".jpg") || altsrc.endsWith("jpeg")) {
							img = altimg;
							break;
						}
					}
				}
				appendImg(md, images, img, id, pictures);

				//Check whether there is an annotated image.
				for (int k=0; k<alt.getLength(); k++) {
					Element altimg = (Element)alt.item(k);
					if (altimg.getAttribute("role").equals("annotation")) {
						String altsrc = altimg.getAttribute("src").toLowerCase();
						if (altsrc.endsWith(".jpg") || altsrc.endsWith("jpeg")) {
							appendImg(md, images, altimg, getID(), pictures);
							break;
						}
					}
				}

			}
		}
		//Now append the MIRCdocument XML to mds.
		//Note that at this point, the image source attributes
		//have been changed to the names of the files in the pictures directory.
		//This is necessary so the XSL can insert the correct references in the content XML.
		Node importedMD = mds.getOwnerDocument().importNode(md.getXML().getDocumentElement(), true);
		mds.appendChild(importedMD);
	}

	//Append an image. Constructing the parameters that allow it
	//to be scaled to the slide. Copy the image to the pictures directory.
	private void appendImg(MircDocument md, Element images, Element img, String id, File pictures) throws Exception {
		int w = StringUtil.getInt(img.getAttribute("w"));
		int h = StringUtil.getInt(img.getAttribute("h"));

		if ((w != 0) && (h != 0)) {

			//Copy the selected image to the Pictures directory
			File inFile = new File(md.getDirectory(), img.getAttribute("src"));
			File outFile = File.createTempFile("IMG-", ".jpg", pictures);
			FileUtil.copy(inFile, outFile);
			String src = outFile.getName();
			img.setAttribute("src", src);
			img.setAttribute("id", id);

			//Now figure out how to place and scale the image on the slide.
			//This has to be a lot easier done in Java than in XSL.
			float slideWidth = 28;
			float slideHeight = 21;
			float marginX = 1;
			float marginY = 1;
			float areaWidth = slideWidth - 2*marginX;
			float areaHeight = slideHeight - 2*marginY;
			float areaAspectRatio = areaHeight / areaWidth;;
			float imageAspectRatio = (float)h / (float)w;

			float xcm, ycm, wcm, hcm;

			if (areaAspectRatio < imageAspectRatio) {
				//fit the image to the height of the area
				float scale = areaHeight / (float)h;
				ycm = marginY;
				hcm = areaHeight;
				wcm = (float)w * scale;
				xcm = marginX + (areaWidth - wcm)/2;
			}
			else {
				//fit the image to the width of the area
				xcm = marginX;
				wcm = areaWidth;
				float scale = areaWidth / (float)w;
				hcm = (float)h * scale;
				ycm = marginY + (areaHeight - hcm)/2;
			}

			//Finallu, create the element
			Element image = images.getOwnerDocument().createElement("image");
			images.appendChild(image);
			image.setAttribute("id", id); //this is the value that indexes the image
			image.setAttribute("src", src); //this is the value that points to the version to use
			image.setAttribute("x", String.format(Locale.US, "%.3fcm", xcm));
			image.setAttribute("y", String.format(Locale.US, "%.3fcm", ycm));
			image.setAttribute("w", String.format(Locale.US, "%.3fcm", wcm));
			image.setAttribute("h", String.format(Locale.US, "%.3fcm", hcm));
		}
	}

	private String getID() {
		idCount++;
		return Integer.toString(idCount);
	}

}

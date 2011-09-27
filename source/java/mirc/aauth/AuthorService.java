/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.aauth;

import java.awt.Dimension;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.zip.ZipEntry;

import mirc.MircConfig;
import mirc.files.FileService;
import mirc.prefs.Preferences;
import mirc.storage.Index;
import mirc.util.MircDocument;
import mirc.util.MircImage;
import mirc.util.MyRsnaSession;
import mirc.util.SvgUtil;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.multipart.UploadedFile;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.objects.ZipObject;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Advanced Author Service.
 * <p>
 * This service allows users to create new MIRCdocuments and edit existing ones.
 */
public class AuthorService extends Servlet {

	static final Logger logger = Logger.getLogger(AuthorService.class);

	static String[] defaultTemplates = new String[] {
		"doc-template-mstf.xml",
		"doc-template-tab.xml",
		"doc-template-page.xml"
	};

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) {
		File mircRoot = MircConfig.getInstance().getRootDirectory();

		//Make sure the default templates are in place
		File aauth = new File( mircRoot, "aauth" );
		for (String name : defaultTemplates) {
			File template = new File( aauth, name );
			FileUtil.getFile( template, "/aauth/templates/"+name );
		}
	}

	/**
	 * Construct a AuthorService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public AuthorService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a web page containing a submission form.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Only accept connections from authors
		if (!req.userHasRole("author")) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		String function = path.element(1);

		if (function.equals("token")) {
			String filename = req.getParameter("file", "");
			if (!filename.equals("")) {
				String resource = "/aauth/tokens/"+filename;
				URL url = FileUtil.getURL(resource);
				if (url != null) {
					res.write(url);
					res.setContentType(filename);
					res.send();
					return;
				}
			}
			res.setResponseCode(res.notfound);
			res.send();
		}

		else if (function.startsWith("ss")) {
			String ssid = function;

			//Make sure the author service is enabled
			MircConfig mc = MircConfig.getInstance();
			Element lib = mc.getLocalLibrary(ssid);
			boolean enabled = ((lib != null) && lib.getAttribute("authenb").equals("yes"));
			if (enabled) {

				res.disableCaching();

				//Get the user and his preferences
				User user = req.getUser();
				String username = user.getUsername();
				Document prefs = Preferences.getInstance().get( username, true ).getOwnerDocument();

				if (path.length() == 2) {
					//This is a request for the template selection page

					//Get the library name
					Element titleElement = XmlUtil.getFirstNamedChild(lib, "title");
					String title = titleElement.getTextContent();

					//Get the UI to determine whether to include the home icon.
					String ui = req.getParameter("ui", "");
					if (!ui.equals("integrated")) ui = "classic";

					//Generate the submission page.
					Object[] params = {
						"ui",			ui,
						"ssid",			ssid,
						"libraryTitle",	title,
						"templates",	getTemplates( new File(root, "aauth") )
					};
					Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/aauth/AuthorService.xsl" ) );
					res.write( XmlUtil.getTransformedText( prefs, xsl, params ) );
					res.setContentType("html");
					res.send();
				}

				else {
					//This is a request to open an existing document in the editor,
					//coming from the "Edit" link on a MIRCdocument display.
					//Get the document and verify it.

					Index index = Index.getInstance(ssid);
					String docPath = path.subpath(3).substring(1);
					File docFile = new File( index.getDocumentsDir(), docPath );
					MircDocument md = new MircDocument(docFile);

					//Make sure the user is authorized to update this document
					if (!md.authorizes("update", user)) { res.redirect("/query"); return; }

					//Get the URL path for the directory containing the MIRCdocument
					int len = path.length();
					String dirPath = "/storage" + path.subpath(1, len-2) + "/";
					String authPath = "/aauth" + path.subpath(1);

					File aauth = new File(root, "query");
					File xslFile = new File(aauth, "Editor.xsl");
					Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslFile, "/aauth/Editor.xsl" ) );

					Object[] params =
						new Object[] {
							"prefs",	prefs,
							"ssid",		ssid,
							"dirpath",	dirPath,
							"icons",	getIcon96(docFile.getParentFile()),
							"authpath",	authPath,
							"date",		"", //existing document; don't modify the date
							"mode",		mc.getMode(),
							"options",	mc.enumeratedValues,
							"species",	mc.speciesValues,
							"version",	mc.getVersion(),
							"activetab","1"
						};
					res.write( XmlUtil.getTransformedText( md.getXML(), xsl, params ) );
					res.setContentType("html");
					res.send();
				}

			}
			else res.redirect("/query");
		}
		else super.doGet(req, res);
	}

	//Get an XML document containing a list of all the
	//templates available for this service.
	private Document getTemplates(File dir) {
		Document doc;
		try { doc= XmlUtil.getDocument(); }
		catch (Exception failure) { return null; }
		Element root = doc.createElement("templates");
		doc.appendChild(root);
		File[] files = dir.listFiles();
		for (int i=0; i<files.length; i++) {
			if (files[i].getName().toLowerCase().endsWith(".xml")) {
				try {
					Document md = XmlUtil.getDocument(files[i]);
					Element mdRoot = md.getDocumentElement();
					if (mdRoot.getTagName().equals("MIRCdocument")) {
						String title = "Untitled";
						NodeList nl = mdRoot.getElementsByTagName("title");
						if (nl.getLength() > 0) title = nl.item(0).getTextContent().replaceAll("\\s+", " ");
						Element child = doc.createElement("template");
						root.appendChild(child);
						Element childFile = doc.createElement("file");
						child.appendChild(childFile);
						childFile.setTextContent(files[i].getName());
						Element childTitle = doc.createElement("title");
						child.appendChild(childTitle);
						childTitle.setTextContent(title);

						String display = mdRoot.getAttribute("display").trim().toLowerCase();
						if (display.equals("")) display = "page";
						Element childToken = doc.createElement("token");
						child.appendChild(childToken);
						childToken.setTextContent(display+".jpg");
					}
				}
				catch (Exception ignoreFile) { }
			}
		}
		return doc;
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * @param req The HttpServletRequest provided by the servlet container.
	 * @param res The HttpServletResponse provided by the servlet container.
	 * @throws ServletException if the servlet cannot handle the request.
	 * @throws IOException if any IO error occurs.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {
		//Only accept connections from authors
		if (!req.userHasRole("author")) { res.redirect("/query"); return; }

		User user = req.getUser();
		String username = user.getUsername();

		Path path = req.getParsedPath();
		int pathLength = path.length();
		String ssid = path.element(1);
		if (ssid.startsWith("ss")) {

			//Make sure the author service is enabled
			MircConfig mc = MircConfig.getInstance();
			Element lib = mc.getLocalLibrary(ssid);
			int jpegQuality = StringUtil.getInt( lib.getAttribute("jpegquality").trim(), -1);
			boolean enabled = ((lib != null) && lib.getAttribute("authenb").equals("yes"));
			if (enabled) {

				res.disableCaching();

				boolean autoindex = lib.getAttribute("autoindex").equals("yes");
				boolean canPublish = autoindex || req.userHasRole("publisher");

				String ct = req.getContentType().toLowerCase();
				File aauth = new File(root, "aauth");
				String docText = req.getParameter("doctext");

				//Figure out what kind of POST this is
				if (ct.contains("application/x-www-form-urlencoded")) {

					if ((pathLength == 2) && (docText == null)) {
						//This is a POST of the template selection form.

						//Update the user's information
						String name = req.getParameter("name", "");
						String affiliation = req.getParameter("affiliation", "");
						String contact = req.getParameter("contact", "");
						Preferences.getInstance().setAuthorInfo(username, name, affiliation, contact);
						Document prefs = Preferences.getInstance().get( username, true ).getOwnerDocument();

						//Get the template file name
						String template = req.getParameter("templatename");

						//Process the template with the editor-form transform file.
						Document templateXML = XmlUtil.getDocument( new File(aauth, template) );

						File xslFile = new File(aauth, "Editor.xsl");
						Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslFile, "/aauth/Editor.xsl" ) );

						Object[] params =
							new Object[] {
								"prefs",	prefs,
								"ssid",		ssid,
								"dirpath",	"",
								"icons",	"",
								"authpath",	"/aauth/" + ssid,
								"date",		StringUtil.getDate(""),
								"mode",		mc.getMode(),
								"options",	mc.enumeratedValues,
								"species",	mc.speciesValues,
								"version",	mc.getVersion(),
								"activetab","1"
							};
						res.write( XmlUtil.getTransformedText( templateXML, xsl, params ) );
						res.setContentType("html");
						res.send();
					}
					else if ((pathLength == 2) && (docText != null)) {
						//This is a POST of a new MIRCdocument
						String activeTab = req.getParameter("activetab");
						String docName = "MIRCdocument.xml";

						//Make sure the document parses and it's a MIRCdocument
						MircDocument md = new MircDocument(docText);

						//Make a directory to receive the document
						Index index = Index.getInstance(ssid);
						File documentsDir = index.getDocumentsDir();
						File dir = new File(documentsDir, StringUtil.makeNameFromDate());
						dir.mkdirs();
						File docFile = new File(dir, docName);
						md.setFile(docFile);

						//Insert any files referenced in the file cabinets
						insertFiles(md, user, jpegQuality);

						//Note that on an initial submission, it is not possible to have
						//annotations, so we don't bother to create the SVG here.

						//See if there is a conflict between the read
						//authorization and the user's roles.
						md.setPublicationRequest(canPublish);

						//Save and index the document
						md.save();
						String key = index.getKey( docFile );
						index.insertDocument( key );

						//Return the editor form
						Document prefs = Preferences.getInstance().get( username, true ).getOwnerDocument();
						String dirPath = "/storage/" + ssid + "/" + index.getKey(dir) + "/";
						String authPath = "/aauth/" + ssid + "/" + key;

						Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/aauth/Editor.xsl" ) );
						Object[] params =
							new Object[] {
								"prefs",	prefs,
								"ssid",		ssid,
								"dirpath",	dirPath,
								"authpath",	authPath,
								"icons",	getIcon96(dir),
								"date",		"", //existing document; don't modify the date
								"mode",		mc.getMode(),
								"options",	mc.enumeratedValues,
								"species",	mc.speciesValues,
								"version",	mc.getVersion(),
								"activetab",activeTab
							};
						res.write( XmlUtil.getTransformedText( md.getXML(), xsl, params ) );
						res.setContentType("html");
						res.send();
					}
					else if ((pathLength > 2) && (docText != null)) {
						//This is a POST of a MIRCdocument update.
						String activeTab = req.getParameter("activetab");

						//Make sure the document parses and it's a MIRCdocument
						MircDocument md = new MircDocument(docText);

						//Get the existing document
						Index index = Index.getInstance(ssid);
						String docPath = path.subpath(3).substring(1);
						File docFile = new File( index.getDocumentsDir(), docPath );
						MircDocument oldmd = new MircDocument(docFile);

						//Make sure the existing document authorizes the user to update it
						if (!oldmd.authorizes("update", user)) { res.redirect("/query"); return; }

						//Okay, we can do the update, set the update MircDocment to point to the old document
						md.setFile( docFile );

						//Insert any files referenced in the file cabinets
						insertFiles(md, user, jpegQuality);

						//Fix the SVG references and create the SVG files
						updateAnnotationFiles(md, jpegQuality);

						//See if there is a conflict between the read
						//authorization and the user's roles.
						md.setPublicationRequest(canPublish);

						//Save and index the document
						md.save();
						String key = index.getKey( docFile );
						index.insertDocument( key );

						//See if this is a preview
						String preview = req.getParameter("preview", "");
						if (preview.equals("true")) {
							res.redirect( "/storage" + path.subpath(1) + "?preview" );
							return;
						}

						//No, return the editor form
						Document prefs = Preferences.getInstance().get( username, true ).getOwnerDocument();
						File dir = docFile.getParentFile();
						String dirPath = "/storage/" + ssid + "/" + index.getKey(dir) + "/";
						String authPath = "/aauth/" + ssid + "/" + key;

						Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/aauth/Editor.xsl" ) );
						Object[] params =
							new Object[] {
								"prefs",	prefs,
								"ssid",		ssid,
								"dirpath",	dirPath,
								"authpath",	authPath,
								"icons",	getIcon96(dir),
								"date",		"", //existing document; don't modify the date
								"mode",		mc.getMode(),
								"options",	mc.enumeratedValues,
								"species",	mc.speciesValues,
								"version",	mc.getVersion(),
								"activetab",activeTab
							};
						res.write( XmlUtil.getTransformedText( md.getXML(), xsl, params ) );
						res.setContentType("html");
						res.send();
					}
				}
			}
			else res.redirect("/query");
		}
		else super.doGet(req, res);
	}

	//Find all the files whose names end in the suffix "_icon96.jpeg"
	//in a directory and return an XML Document containing elements
	//with their names (without the suffix)
	private Document getIcon96(File dir) {
		String target = "_icon96.jpeg";
		int targetLength = target.length();

		try {
			Document doc = XmlUtil.getDocument();

			//Create the root node and add it
			Element root = doc.createElement("icons");
			doc.appendChild(root);

			//Get the list of Files in the directory
			File[] files = dir.listFiles();

			//Make elements for all the icon96.jpeg files
			String name;
			for (File file: files) {
				if (!file.isDirectory() && file.getName().endsWith(target)) {
					name = file.getName();
					name = name.substring(0, name.length()-targetLength);
					checkForFile(new File(dir, name+".jpeg"), file, root);
					checkForFile(new File(dir, name+".jpg"), file, root);
					checkForFile(new File(dir, name+".gif"), file, root);
					checkForFile(new File(dir, name+".png"), file, root);
					checkForFile(new File(dir, name+"_base.jpeg"), file, root);
					checkForFile(new File(dir, name+"_full.jpeg"), file, root);
					checkForFile(new File(dir, name+"_full.jpg"), file, root);
					checkForFile(new File(dir, name+"_full.gif"), file, root);
					checkForFile(new File(dir, name+"_full.png"), file, root);
				}
			}
			return doc;
		}
		catch (Exception e) { }
		return null;
	}

	//Add a file element child if a specific file exists.
	private void checkForFile(File imageFile, File iconFile, Element parent) {
		if (imageFile.exists()) {
			Element child = parent.getOwnerDocument().createElement("file");
			parent.appendChild(child);
			child.setAttribute( "name", imageFile.getName());
			child.setAttribute( "icon", iconFile.getName());
		}
	}

	//Get any files referenced in the user's file cabinet.
	private void insertFiles(MircDocument md, User user, int jpegQuality) {
		try {
			//Get a File pointing to the directory where the document is stored.
			File parentDir = md.getDirectory();

			//Look for attributes pointing to files in the cabinets.
			//First, process the image elements.
			Document doc = md.getXML();
			Element root = doc.getDocumentElement();
			NodeList nl = root.getElementsByTagName("image");
			String username = user.getUsername();

			for (int i=0; i<nl.getLength(); i++) {
				Element img = (Element)nl.item(i);
				String src = img.getAttribute("src");
				if (src.startsWith("[")) {
					int k = src.indexOf("]");
					if (k != -1) {
						String path = src.substring(1,k);
						String inName = src.substring(k+1);
						File outFile = null;

						//Get the file
						if (!path.startsWith("myRSNA|")) {
							//This is a file in a MIRC file cabinet; path points to
							//the cabinet and name is the name of the file.
							File inFile = FileService.getFile(path, inName, username);
							String outName = inName.replaceAll("\\s++","_");
							outFile = new File(parentDir, outName);
							FileUtil.copy(inFile, outFile);
						}
						else {
							//This is a file in the user's myRSNA files.
							//path = "myRSNA|" followed by the title of the file on
							//the myRSNA site. This is the name that has the proper
							//extension. The inName variable now contains the node
							//id on the myRSNA site.
							String title = path.substring(7);
							//Remove the spaces in the title, if any
							title = title.replaceAll("\\s+", "_");
							outFile = new File(parentDir, title);

							//Get the file from the myRSNA site.
							//Note that the MyRsnaSession.getFile(File, String)
							//method is static. We don't actually require a
							//MyRsnaSession instance because file accesses are
							//not authenticated (which seems odd, but it's true).
							//The getFile method is in MyRsnaSession because it
							//seems to be a nice home for it in case the myRSNA
							//developers change the authentication requirements
							//some day.
							if (!MyRsnaSession.getFile(outFile, inName)) {
								//The RSNA site is not available, or somebody is doing
								//something funny, skip this image element, even though
								//it may cause problems in viewing the document.
								break;
							}
						}
						try {
							MircImage image = new MircImage(outFile);
							int maxWidth = getMaxWidth(img);
							int minWidth = getMinWidth(img);
							insertImage(img, image, maxWidth, minWidth, jpegQuality);
						}
						catch (Exception ignore) {
							logger.warn("Exception processing image: "+src,ignore);
						}
					}
				}
			}
			//Fix any other file cabinet references and get the files
			handleCabinetFileRefs(root, parentDir, username);
		}
		catch (Exception e) { }
	}

	//Walk the tree under the current element, finding any elements
	//with href or src attributes starting with "[", and copying
	//the referenced files from the file cabinet to parentDir.
	private void handleCabinetFileRefs(Element element, File parentDir, String username) {
		String href = element.getAttribute("href");
		String src = element.getAttribute("src");
		String docref = element.getAttribute("docref");

		if (href.startsWith("[")) getReferencedFile( element, "href", href, parentDir, username );
		if (src.startsWith("[")) getReferencedFile( element, "src", src, parentDir, username );
		if (docref.startsWith("[")) getReferencedFile( element, "docref", docref, parentDir, username );

		Node child = element.getFirstChild();
		while (child != null) {
			if (child instanceof Element) {
				handleCabinetFileRefs( (Element)child, parentDir, username);
			}
			child = child.getNextSibling();
		}
	}

	//Get one referenced file and fix the associated attribute
	private void getReferencedFile( Element element, String attrname, String ref, File parentDir, String username ) {
		int k = ref.indexOf("]");
		if (k != -1) {
			String path = ref.substring(1,k);
			String inName = ref.substring(k+1);
			File inFile = FileService.getFile(path, inName, username);
			String outName = inName.replaceAll("\\s++","_");
			File outFile = new File(parentDir, outName);
			boolean result = FileUtil.copy(inFile, outFile);
			element.setAttribute( attrname, outName );
		}
	}

	//Figure out the maxWidth for a specific image.
	private int getMaxWidth(Element image) {
		Element parent = (Element)image.getParentNode();
		String imageWidth = parent.getAttribute("image-width").trim();
		String parentName = parent.getNodeName();

		if (parentName.equals("section")) {
			return StringUtil.getInt(imageWidth, 256);
		}
		else if (parentName.equals("image-section")) {
			//If the image-width attribute is present, use it.
			//If it is not present, try the image-pane-width attribute and use it.
			//If all else fails, use 700.
			int width = StringUtil.getInt(imageWidth, -1);
			if (width >= 0) return width;
			String ipw = parent.getAttribute("image-pane-width").trim();
			return StringUtil.getInt(ipw, 700);
		}
		return 512;
	}

	//Figure out the minWidth for a specific image.
	private int getMinWidth(Element image) {
		Element parent = (Element)image.getParentNode();
		String minWidth = parent.getAttribute("min-width").trim();
		return StringUtil.getInt( minWidth, 0 );
	}

	//Create the child files for an image and return the XML elements for them.
	private void insertImage(Element img, MircImage image, int maxWidth, int minWidth, int jpegQuality) {
		int imageWidth = image.getColumns();
		int imageHeight = image.getRows();

		//Get a width for the base image.
		if (minWidth > maxWidth) minWidth = maxWidth;
		int width = Math.min( maxWidth, Math.max( imageWidth, minWidth ) );

		//Set up the image element
		String name = image.getFile().getName();
		String nameNoExt = name.substring(0,name.lastIndexOf("."));
		File docDir = image.getFile().getParentFile();

		//Make the base image. Use the original if you can; otherwise, make one.
		if (image.isDicomImage() ||
				image.hasNonStandardImageExtension() ||
					(width != imageWidth)) {
			Dimension d_base = image.saveAsJPEG(new File(docDir, nameNoExt+"_base.jpeg"), 0, width, minWidth, jpegQuality);
			img.setAttribute( "src", nameNoExt+"_base.jpeg" );
			img.setAttribute( "width", Integer.toString(width) );
			img.setAttribute( "w", Integer.toString(d_base.width) );
			img.setAttribute( "h", Integer.toString(d_base.height) );
		}
		else {
			img.setAttribute( "src", name );
			img.setAttribute( "width", Integer.toString(width) );
			img.setAttribute( "w", Integer.toString(imageWidth) );
			img.setAttribute( "h", Integer.toString(imageHeight) );
		}

		//Make the icons for the author service and the mstf display
		Dimension d_icon = image.saveAsJPEG( new File(docDir, nameNoExt+"_icon.jpeg"), 0, 64, 0, jpegQuality );
		Dimension d_icon96 = image.saveAsJPEG( new File(docDir, nameNoExt+"_icon96.jpeg"), 0, 96, 0, jpegQuality ); //for the author service
		Document doc = img.getOwnerDocument();
		Element alt = doc.createElement( "alternative-image" );
		alt.setAttribute( "role", "icon" );
		alt.setAttribute( "src", nameNoExt+"_icon.jpeg" );
		alt.setAttribute( "w", Integer.toString(d_icon.width) );
		alt.setAttribute( "h", Integer.toString(d_icon.height) );
		img.appendChild(alt);

		//Make the full image if necessary
		if (imageWidth > maxWidth) {
			alt = doc.createElement( "alternative-image" );
			alt.setAttribute( "role", "original-dimensions" );
			if (image.isDicomImage() || image.hasNonStandardImageExtension()) {
				Dimension d_full = image.saveAsJPEG( new File(docDir, nameNoExt+"_full.jpeg"), 0, imageWidth, 0, jpegQuality);
				alt.setAttribute( "src", nameNoExt+"_full.jpeg" );
				alt.setAttribute( "w", Integer.toString(d_full.width) );
				alt.setAttribute( "h", Integer.toString(d_full.height) );
			}
			else {
				alt.setAttribute( "src", name );
				alt.setAttribute( "w", Integer.toString(imageWidth) );
				alt.setAttribute( "h", Integer.toString(imageHeight) );
			}
			img.appendChild(alt);
		}

		//Finally, put in the original format image if necessary
		if (image.isDicomImage() || image.hasNonStandardImageExtension()) {
			alt = doc.createElement( "alternative-image" );
			alt.setAttribute( "role", "original-format" );
			alt.setAttribute( "src", name );
			img.appendChild(alt);
		}
	}

	//Create or update SVG files for embedded annotations
	//and create the corresponding annotated JPEGs.
	private void updateAnnotationFiles(MircDocument md, int jpegQuality) {
		String svgHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

		File docFile = md.getFile();
		File parentDir = docFile.getParentFile();
		Document doc = md.getXML();
		Element root = doc.getDocumentElement();

		//Find the svg elements
		NodeList nodeList = root.getElementsByTagName("svg");
		for (int i=0; i<nodeList.getLength(); i++) {

			Element svg = (Element)nodeList.item(i);

			NodeList svgList = svg.getElementsByTagName("image");
			Element svgImage = (Element)svgList.item(0);

			Element altimg = (Element)svg.getParentNode();
			Element image = (Element)altimg.getParentNode();

			//Get the name of the image file on which the annotated
			//image will be based and get a file pointing to it.
			String src = image.getAttribute("src");
			File srcFile = new File(parentDir, src);

			//Get the MircImage for the source
			MircImage srcImage = null;
			try { srcImage = new MircImage(srcFile); }
			catch (Exception quit) { return; }

			//Make sure that the image is not 8-bit
			//to avoid the brightness bug in Java.
			try {
				if (srcImage.getPixelSize() == 8) {
					//8-bit pixels. Convert the image to a 24-bit JPEG
					//and update the SVG and MIRCdocument XML.

					//Get the appropriate name for the converted image
					String name = src;
					int k = name.lastIndexOf(".");
					if (k != -1) name = name.substring(0,k);
					name += "_base.jpeg";
					File convertedFile = new File(parentDir, name);

					//Convert the image
					int width = srcImage.getWidth();
					Dimension d = srcImage.saveAsJPEG(convertedFile, 0, width, width, jpegQuality);

					//Update the SVG
					svgImage.setAttribute("xlink:href", name);

					//Update the image element in the MIRCdocument
					image.setAttribute("src", name);
					image.setAttribute("w", Integer.toString(d.width));
					image.setAttribute("h", Integer.toString(d.height));

					//There should be no alternative-image element with
					//the role "original-format", so create one and set
					//it to point to the unconverted image.
					Element ofElement = doc.createElement("alternative-image");
					ofElement.setAttribute("role", "original-format");
					ofElement.setAttribute("src", src);
					image.appendChild(ofElement);
				}
			}
			catch (Exception ignore) { }

			//Get the appropriate name for the annotated file
			int k = src.lastIndexOf(".");
			if (k != -1) src = src.substring(0,k);
			if (src.endsWith("_base")) src = src.substring(0,src.length()-5);
			src += "_an";

			//Save the SVG file
			File svgFile = new File(parentDir, src+".svg");
			String svgText = XmlUtil.toString(svg);
			FileUtil.setText(svgFile, svgHeader + svgText);

			//Fix up the alternative-image that points to the SVG file
			altimg.setAttribute("src",src+".svg");
			altimg.setAttribute("type","svg");
			while (altimg.hasChildNodes()) altimg.removeChild(altimg.getFirstChild());

			//Now make the annotated jpeg.
			//Important note: The XML text generator in editor.js will not return an
			//<alternative-image type="image"> element for the annotated jpeg if it
			//returns an <svg> child of the <alternative-image type="svg"> element.
			//This means that we can create the type="image" element secure in the
			//knowledge that we are not creating a duplicate.
			try {
				SvgUtil.saveAsJPEG(svgFile, parentDir);
				//It worked, create the element to reference it.
				Element jpegElement = doc.createElement("alternative-image");
				jpegElement.setAttribute("role","annotation");
				jpegElement.setAttribute("type","image");
				jpegElement.setAttribute("src",src+".jpg");
				jpegElement.setAttribute("w", Integer.toString(srcImage.getWidth()));
				jpegElement.setAttribute("h", Integer.toString(srcImage.getHeight()));
				image.appendChild(jpegElement);
			}
			catch (Exception ex) {
				logger.warn("Unable to create the annotated JPEG file for "+docFile, ex);
			}
		}
		return;
	}
}
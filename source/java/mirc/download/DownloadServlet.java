/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.download;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.servlets.Servlet;
import org.rsna.util.FileUtil;
import org.rsna.util.JarUtil;
import org.rsna.util.JdbmUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A servlet to track downloads from a <code>downloads</code> directory.
 */
public class DownloadServlet extends Servlet {

	/**
	 * Static init method. Nothing is required; the empty
	 * method here is just to prevent the superclass' method
	 * from creating an unnecessary index.html file.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a DownloadServlet.
	 * @param root the root directory of the server (note: this servlet
	 * does not override the root directory, so its root points to the
	 * root it is provided [CTP/ROOT]).
	 * @param context the path identifying the servlet.
	 */
	public DownloadServlet(File root, String context) {
		super(root, context);
	}

	/**
	 * Handle requests for files in the context, logging the requests.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {
		Path path = req.getParsedPath();
		int length = path.length();

		boolean admin = req.userHasRole("admin");

		if (length == 1) {
			//This is a request for the download page
			res.write( getPage() );
			res.setContentType("html");
			res.disableCaching();
			res.send();
		}
		else if (admin && (length == 2) && path.element(1).equals("report")) {
			//This is a request for the download summary report
			res.setContentType("xml");
			Document doc = DownloadDB.getInstance().getXML();
			res.write( XmlUtil.toPrettyString( doc.getDocumentElement() ) );
			res.disableCaching();
			res.send();
		}
		else {
			//This is a file download request. Log downloads from the
			//disk directory. Other files are served from the jar
			//without logging.
			String p = req.path;
			if (p.startsWith("/")) p = p.substring(1);
			File file = new File(root, p);
			if (file.exists() && !file.isDirectory()) {
				String name = file.getName();

				String build = "";
				if (name.toLowerCase().endsWith(".jar")) {
					Hashtable<String,String> manifest = JarUtil.getManifestAttributes(file);
					if (manifest != null) {
						String date = manifest.get("Date");
						if (date != null) build = date;
						String version = manifest.get("Version");
						if (version != null) {
							if (!build.equals("")) build += " ";
							build += "["+version+"]";
						}
					}
				}

				String email = req.getParameter("email", "").trim();
				String ip = req.getRemoteAddress();
				DownloadDB.getInstance().insert(file, build, ip, email);

				res.write(file);
				long fileLMDate = file.lastModified();
				res.setLastModified(fileLMDate);
				res.setETag(fileLMDate);
				res.setContentType(file);
				res.setContentDisposition(file);
				res.disableCaching();
				res.send();
			}
			//The requested file doesn't exist in the download
			//directory tree, handle the request in the superclass
			//so we can serve the files from the jar (like the CSS file).
			else super.doGet(req, res);
		}
	}

	private String getPage() {
		try {
			Document doc = XmlUtil.getDocument();
			Element filesElement = doc.createElement("files");
			doc.appendChild(filesElement);
			File dir = new File(root, context);
			if (!dir.exists()) dir.mkdirs();
			File[] files = dir.listFiles();
			Arrays.sort(files);
			for (File file : files) {
				Element fileElement = doc.createElement("file");
				filesElement.appendChild(fileElement);
				String name = file.getName();
				fileElement.setAttribute("name", name);
				String lm = StringUtil.getDateTime( file.lastModified(), " at " );
				fileElement.setAttribute("lastModified", lm);
				if (name.toLowerCase().endsWith(".jar")) {
					Hashtable<String,String> manifest = JarUtil.getManifestAttributes(file);
					if (manifest != null) {
						String build = "";
						String date = manifest.get("Date");
						if (date != null) build = date;
						String version = manifest.get("Version");
						if (version != null) {
							if (!build.equals("")) build += " ";
							build += "["+version+"]";
						}
						fileElement.setAttribute("build", build);
					}
				}
			}
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/download/DownloadServlet.xsl" ) );
			return XmlUtil.getTransformedText( doc, xsl, null );
		}
		catch (Exception ex) { return "Unable to create the download page."; }
	}
}

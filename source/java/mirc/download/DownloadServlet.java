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
import java.util.LinkedList;
import mirc.MircConfig;
import org.rsna.ctp.objects.ZipObject;
import org.rsna.multipart.UploadedFile;
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
		boolean upload = req.userHasRole("update");

		String ui = req.getParameter("ui", "");
		if (!ui.equals("integrated")) ui = "classic";

		if (length == 1) {
			//This is a request for the download page
			res.write( getPage(admin, upload, ui) );
			res.setContentType("html");
			res.disableCaching();
			res.send();
		}
		else if ((admin || upload) && (length == 2) && path.element(1).equals("report")) {
			//This is a request for the download summary report
			res.setContentType("xml");
			Document doc = DownloadDB.getInstance().getXML();
			res.write( XmlUtil.toPrettyString( doc.getDocumentElement() ) );
			res.disableCaching();
			res.send();
		}
		else if (upload && (length == 2) && path.element(1).equals("upload")) {
			//This is a request for the upload submission page.
			//This is only available to users with the update privilege.
			if (req.userHasRole("update")) {
				res.write( getUploadPage(ui) );
				res.setContentType("html");
				res.disableCaching();
				res.send();
			}
			else res.redirect("/query");
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
				String nameLC = name.toLowerCase();
				String build = getBuild(file);
				String ip = req.getRemoteAddress();
				DownloadDB.getInstance().insert(file, build, ip, "", "", "", "", "", "");

				res.write(file);
				long fileLMDate = file.lastModified();
				res.setLastModified(fileLMDate);
				res.setETag(fileLMDate);
				res.setContentType(file);
				res.setContentDisposition(file);
				res.send();
			}
			//The requested file doesn't exist in the download
			//directory tree, handle the request in the superclass
			//so we can serve the files from the jar (like the CSS file).
			else super.doGet(req, res);
		}
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		//Only accept connections from users with the update privilege
		if (!req.userHasRole("update")) { res.redirect("/query"); return; }

		MircConfig mc = MircConfig.getInstance();

		//Make a temporary directory to receive the files
		File dir = mc.createTempDirectory();

		//Get the posted files.
		int maxsize = 75 * 1024 * 1024;
		LinkedList<UploadedFile> files = req.getParts(dir, maxsize);

		//Get the UI (must be done after getting the parts)
		String ui = req.getParameter("ui", "");
		if (!ui.equals("integrated")) ui = "classic";

		//Now copy them over to the context directory,
		//overwriting any files that already exist.
		File ctx = new File(root, context);
		if (!ctx.exists()) ctx.mkdirs();

		for ( UploadedFile file: files.toArray( new UploadedFile[files.size()] ) ) {
			File in = file.getFile();
			File out = new File( ctx, in.getName() );
			FileUtil.copy(in, out);
		}

		//Now delete the temp directory
		FileUtil.deleteAll(dir);

		//Send the user to the download page so he can see what he did.
		res.redirect("/download?ui="+ui);
	}

	private String getBuild(File file) {
		String build = "";
		String nameLC = file.getName().toLowerCase();
		if (nameLC.endsWith(".jar")) {
			Hashtable<String,String> manifest = JarUtil.getManifestAttributes(file);
			build = getJarBuild(manifest);
		}
		else if (nameLC.endsWith(".zip")) {
			try {
				ZipObject zobj = new ZipObject(file);
				Document manifest = zobj.getManifestDocument();
				build = getZipBuild(manifest);
			}
			catch (Exception nobuild) { }
		}
		return build;
	}

	private String getJarBuild(Hashtable<String,String> manifest) {
		String build = "";
		if (manifest != null) {
			String date = manifest.get("Date");
			if (date != null) build = date;
			String version = manifest.get("Version");
			if (version != null) {
				if (!build.equals("")) build += " ";
				build += "["+version+"]";
			}
		}
		return build;
	}

	private String getZipBuild(Document manifest) {
		String build = "";
		if (manifest != null) {
			Element root = manifest.getDocumentElement();
			String date = root.getAttribute("date").trim();
			if (!date.equals("")) build = date;
			String version = root.getAttribute("version").trim();
			if (!version.equals("")) {
				if (!build.equals("")) build += " ";
				build += "["+version+"]";
			}
		}
		return build;
	}

	private String getPage(boolean admin, boolean upload, String ui) {
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
				String nameLC = name.toLowerCase();
				fileElement.setAttribute("name", name);
				String lm = StringUtil.getDateTime( file.lastModified(), " at " );
				fileElement.setAttribute("lastModified", lm);
				fileElement.setAttribute("size", Long.toString( file.length() ));
				if (nameLC.endsWith(".jar")) {
					Hashtable<String,String> manifest = JarUtil.getManifestAttributes(file);
					if (manifest != null) {
						String build = getJarBuild(manifest);
						fileElement.setAttribute("build", build);
						String desc = manifest.get("Description");
						if (desc != null) fileElement.setAttribute("desc", desc);
					}
				}
				else if (nameLC.endsWith(".zip")) {
					try {
						ZipObject zobj = new ZipObject(file);
						Document manifest = zobj.getManifestDocument();
						if (manifest != null) {
							String build = getZipBuild(manifest);
							fileElement.setAttribute("build", build);
							Element root = manifest.getDocumentElement();
							String desc = root.getAttribute("description").trim();
							if (!desc.equals("")) fileElement.setAttribute("desc", desc);
						}
					}
					catch (Exception skip) { }
				}
			}
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/download/DownloadServlet.xsl" ) );
			String[] params = new String[] {
				"admin", (admin ? "yes" : "no"),
				"upload", (upload ? "yes" : "no"),
				"ui", ui};
			return XmlUtil.getTransformedText( doc, xsl, params );
		}
		catch (Exception ex) { return "Unable to create the download page."; }
	}

	private String getUploadPage(String ui) {
		try {
			MircConfig mc = MircConfig.getInstance();
			Document doc = mc.getXML();
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/download/UploadPage.xsl" ) );
			String[] params = new String[] {"ui", ui};
			return XmlUtil.getTransformedText( doc, xsl, params );
		}
		catch (Exception ex) { return "Unable to create the page."; }
	}
}

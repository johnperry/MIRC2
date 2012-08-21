/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.myrsna;

import java.io.File;
import java.util.LinkedList;

import mirc.MircConfig;
import mirc.activity.ActivityDB;
import mirc.prefs.Preferences;
import mirc.storage.StorageService;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.server.Users;

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A Servlet to export local cases to a user's myRSNA account.
 */
public class MyRSNAServlet extends Servlet {

	static final Logger logger = Logger.getLogger(MyRSNAServlet.class);

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available. This method is only
	 * called once as the server starts.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a MyRSNAServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public MyRSNAServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Start a background thread to export a list of cases to the user's
	 * myRSNA account. Only local cases for which the user has the export
	 * privilege are exported. All other cases are ignored. The response
	 * is sent in plain text for display by the caller in some kind of UI.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {
		String response = "";

		//Require authentication
		if (req.isFromAuthenticatedUser()) {
			User user = req.getUser();
			Preferences prefs = Preferences.getInstance();
			Element acct = prefs.getMyRsnaAccount(user.getUsername());
			if (acct != null) {
				String urlsParam = req.getParameter("urls");
				if (urlsParam != null) {
					MircConfig mc = MircConfig.getInstance();
					File mircRoot = mc.getRootDirectory();
					String[] urls = urlsParam.split("\\|");
					LinkedList<File> files = new LinkedList<File>();
					for (String url : urls) {
						if (mc.isLocal(url)) {
							int k = url.indexOf("/storage/");
							if (k != -1) {
								url = url.substring(k+1);
								url.replace("/", File.separator);
								files.add( new File(mircRoot, url) );
							}
						}
					}
					if (files.size() > 0) {
						(new MyRSNAExporter(req, files)).start();
						if (files.size() == 1) {
							response = "One local document was submitted for export.\n\n";
						}
						else {
							response = files.size() + " local documents were submitted for export.\n\n";
						}
						response += "Exports are performed in a background task.\n\n"
								  + "Note: only those documents for which the user\n"
								  + "has the export privilege will be transmitted.";
					}
					else {
						response = "No local documents were found in the request.";
					}
				}
			}
			else {
				response = "Unable to export the cases because\n"
						 + "the user's myRSNA credentials are not\n"
						 + "available in the user's TFS account.";
			}
		}

		else {
			response = "Unable to export the cases because\nthe user is not authenticated.";
		}
		res.write(response);
		res.setContentType("txt");
		res.send();
	}

	class MyRSNAExporter extends Thread {

		HttpRequest req;
		LinkedList<File> files;

		public MyRSNAExporter(HttpRequest req, LinkedList<File> files) {
			this.req = req;
			this.files = files;
		}

		public void run() {
			User user = req.getUser();
			for (File file : files) {
				try {
					Document doc = XmlUtil.getDocument(file);
					if (StorageService.userIsAuthorizedTo("export", doc, req)) {
						File zipFile = StorageService.getFileForZip(doc, file, null);
						String[] filenames = StorageService.getFilenames(doc, file);
						boolean ok = FileUtil.zipFiles(filenames, file.getParentFile(), zipFile);
						if (ok) {
							if (StorageService.exportToMyRsna(user, zipFile)) {
								String ssid = StorageService.getSSID(file);;
								ActivityDB.getInstance().increment(ssid, "myrsna");
							}
						}
						zipFile.delete();
					}
				}
				catch (Exception skip) { }
			}
		}
	}

}
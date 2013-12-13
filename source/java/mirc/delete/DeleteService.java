/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.delete;

import java.io.File;
import java.net.URL;

import mirc.MircConfig;
import mirc.ssadmin.StorageServiceAdmin;
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
 * A Servlet to delete local MIRCdocuments.
 */
public class DeleteService extends Servlet {

	static final Logger logger = Logger.getLogger(DeleteService.class);

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a DeleteService.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public DeleteService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Delete the MIRCdocuments corresponding to a list of URLs,
	 * and then return the query page.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		String urlsParam = req.getParameter("urls");

		if (req.isFromAuthenticatedUser() && (urlsParam != null)) {

			MircConfig mc = MircConfig.getInstance();
			File mircRoot = mc.getRootDirectory();
			User user = req.getUser();

			String[] urls = urlsParam.split("\\|");
			for (String url : urls) {
				if (mc.isLocal(url)) {
					int k = url.indexOf("/storage/");
					if (k != -1) {
						url = url.substring(k+1);
						int kk = url.indexOf("?");
						if (kk > 0) url = url.substring(0, kk);

						Path path = new Path(url);
						String ssid = path.element(1);
						String docref = path.subpath(2).substring(1);

						url.replace("/", File.separator);
						File mdFile = new File(mircRoot, url);

						MircDocument md = new MircDocument(mdFile);
						boolean ok = md.authorizes("delete", user);

						if (ok) StorageServiceAdmin.deleteDocument(ssid, docref);
					}
				}
			}
		}
		res.redirect("/query");
	}
}

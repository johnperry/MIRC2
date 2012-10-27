/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.reset;

import java.io.File;

import mirc.MircConfig;
import mirc.storage.Index;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.servlets.Servlet;

import org.rsna.util.Cache;
import org.rsna.util.StringUtil;

import org.apache.log4j.Logger;

/**
 * Servlet to reset the DICOM Service and TCE Service templates and clear the cache.
 * This service only accepts a GET and returns the main page of the server.
 */
public class ResetService extends Servlet {

	static final Logger logger = Logger.getLogger(ResetService.class);

	/**
	 * Construct a ResetService.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public ResetService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();

	}

	/**
	 * Reset the templates and clear the cache.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws ServletException if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		if (req.userHasRole("admin")) {

			//Remove the standard templates from all the libraries
			MircConfig mc = MircConfig.getInstance();
			for (String id : mc.getLocalLibraryIDs()) {
				Index index = Index.getInstance(id);
				File docs = index.getDocumentsDir();
				docs = docs.getAbsoluteFile();
				File parent = docs.getParentFile();

				backupTemplate(parent, "DicomServiceTemplate.xml");
				backupTemplate(parent, "TCEServiceTemplate.xml");
			}

			//Clear the cache so the templates will be refreshed from the classpath
			Cache.getInstance().clear();
		}

		//Now return the main page.
		res.redirect("/query");
	}

	//Backup a template, effectively deleting it.
	private void backupTemplate(File parent, String name) {

		if (parent.exists() && parent.isDirectory()) {
			File template = new File(parent, name);
			if (template.exists()) {
				int k = name.lastIndexOf(".xml");
				String target = name.substring(0,k) + "[";
				int tlen = target.length();

				int n = 0;
				File[] files = parent.listFiles();
				if (files != null) {
					for (File file : files) {
						String fname = file.getName();
						if (fname.startsWith(target)) {
							int kk = fname.indexOf("]", tlen);
							if (kk > tlen) {
								int nn = StringUtil.getInt(fname.substring(tlen, kk));
								if (nn > n) n = nn;
							}
						}
					}
				}
				n++;
				File backup = new File(parent, name.substring(0,k) + "["+n+"]" + ".xml");
				backup.delete(); //shouldn't be there, but just in case.
				template.renameTo(backup);
			}
		}
	}
}
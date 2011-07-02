/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.sort;

import java.io.File;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.util.MircDocument;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;

import org.w3c.dom.Element;

/**
 * Class to sort the images in the image-section of a MIRCdocument..
 * This service accepts only a GET and returns the modified MIRCdocument.
 * containing files for insertion in MIRCdocuments.
 */
public class SortImagesService extends Servlet {

	static final Logger logger = Logger.getLogger(SortImagesService.class);

	/**
	 * Construct a SortImagesService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public SortImagesService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Sort the imagge-section and return the modified MIRCdocument.
	 * @param req The HttpServletRequest provided by the servlet container.
	 * @param res The HttpServletResponse provided by the servlet container.
	 * @throws ServletException if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Only accept connections from authenticated users
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		String ssid = path.element(1);
		if (ssid.startsWith("ss")) {

			//Make sure the document authorizes updates by this user
			MircConfig mc = MircConfig.getInstance();
			Element lib = mc.getLocalLibrary(ssid);
			boolean enabled = ((lib != null) && lib.getAttribute("authenb").equals("yes"));
			if (enabled) {

				//Get the document
				Index index = Index.getInstance(ssid);
				String docPath = path.subpath(3).substring(1);
				File docFile = new File( index.getDocumentsDir(), docPath );
				MircDocument md = new MircDocument(docFile);

				//See if we can update this document
				if (md.authorizes( "update", req.getUser() )) {

					//Sort and save the document
					md.sortImageSection();
					md.save();

					//Redirect to the document
					res.redirect("/storage" + path.subpath(1));
					return;

				}
			}
		}
		//If we get here, either the user isn't allowed to modify the document
		//or the POST didn't come from a MIRC site and somebody is hacking us.
		res.redirect("/query");
	}

}
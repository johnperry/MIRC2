/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.publish;

import java.io.File;
import java.util.LinkedList;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.util.MircDocument;
import mirc.util.MircImage;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.multipart.UploadedFile;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class to publish a MircDocument.
 */
public class PublishService extends Servlet {

	static final Logger logger = Logger.getLogger(PublishService.class);

	/**
	 * Construct a PublishService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public PublishService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Publish a MircDocument.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Only accept connections from publishers
		if (!req.userHasRole("publisher")) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		String ssid = path.element(1);
		if (ssid.startsWith("ss")) {

			//Get the document
			Index index = Index.getInstance(ssid);
			String docPath = path.subpath(3).substring(1);
			File docFile = new File( index.getDocumentsDir(), docPath );
			MircDocument md = new MircDocument(docFile);

			//Publish it
			md.makePublic();
			md.clearPublicationRequest();
			md.save();

			//Index the MIRC document
			index.insertDocument( index.getKey(docFile) );

			//Display it
			res.redirect("/storage" + path.subpath(1));
		}

		//If we get here, the request was faulty; go to the query page.
		res.redirect("/query");
	}

}
/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.comment;

import java.io.File;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.storage.IndexEntry;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * A servlet to return a summary of documents produced by authors.
 */
public class CommentService extends Servlet {

	static final Logger logger = Logger.getLogger(CommentService.class);

	/**
	 * Construct a CommentService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public CommentService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	public void doGet( HttpRequest req, HttpResponse res ) throws Exception {

		logger.info("doGet: "+req.toString());
 }

	/**
	 * The servlet method that responds to an HTTP POST.
	 * <p>
	 * Return content depending on the parameters, which are the same as
	 * the query parameters specified for a GET.
	 */
	public void doPost( HttpRequest req, HttpResponse res ) throws Exception {

		logger.info("doPost: "+req.toString());

		Path path = req.getParsedPath();
		String command = path.element(1);
		String filePath = path.subpath(2);

		if (req.userHasRole("author")) {

			if (command.equals("newthread")) {
				logger.info("newthread called");
			}

			else if (command.equals("newpost")) {
				logger.info("newpost called");
			}
		}
		logger.warn("redirecting to /storage"+filePath);
		res.redirect("/storage"+filePath);
	}

}


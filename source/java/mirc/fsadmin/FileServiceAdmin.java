/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.fsadmin;

import java.io.*;
import java.net.*;
import java.util.*;

import mirc.MircConfig;
import org.apache.log4j.Logger;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.servlets.Servlet;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The File Service Admin servlet.
 * This servlet provides a browser-accessible user interface for
 * configuring the file service.
 */
public class FileServiceAdmin extends Servlet {

	static final Logger logger = Logger.getLogger(FileServiceAdmin.class);

	static SharedFileCabinetManager sfcManager;

	/**
	 * Construct a FileServiceAdmin.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public FileServiceAdmin(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) {

		//Find the base directory of the file service and
		//start the SharedFileCabinetManager.
		MircConfig mc = MircConfig.getInstance();
		File base = new File( mc.getRootDirectory(), "files" );
		sfcManager = new SharedFileCabinetManager(base);
		sfcManager.start();
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * This method returns an HTML page containing a form for
	 * changing the parameters of the File Service.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		if (req.userHasRole("admin")) {
			Path path = req.getParsedPath();
			if (path.length() == 1) {
				//This is a request for the admin page
				res.disableCaching();
				res.setContentType("html");
				res.write( getPage() );
				res.send();
			}
			else super.doGet(req, res);
		}
		else res.redirect("/query");
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * This method uses the posted parameters to update
	 * the FileService configuration.
	 * It then returns an HTML page containing a new form
	 * constructed from the new contents of the file.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		if (req.userHasRole("admin")) {
			String timeout = Integer.toString(StringUtil.getInt( req.getParameter("timeout"), 0));
			String maxsize = Integer.toString(StringUtil.getInt( req.getParameter("maxsize"), 75));

			MircConfig mc = MircConfig.getInstance();
			Element fs = mc.getFileService();

			fs.setAttribute("timeout", timeout);
			fs.setAttribute("maxsize", maxsize);
			mc.setFileService(fs);

			//Return a new form
			res.disableCaching();
			res.setContentType("html");
			res.write( getPage() );
			res.send();
		}
		else res.redirect("/query");
	}

	private String getPage() throws Exception {
		Document xml = MircConfig.getInstance().getXML();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/fsadmin/FileServiceAdmin.xsl" ) );
		return XmlUtil.getTransformedText(xml, xsl, null);
	}

}

/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.activity;

import java.io.File;
import java.net.URLEncoder;

import mirc.MircConfig;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.HtmlUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * A servlet to return a summary of recent activity on the site.
 */
public class ActivityReport extends Servlet {

	static final Logger logger = Logger.getLogger(ActivityReport.class);

	/**
	 * Construct an ActivityReport servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public ActivityReport(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * This servlet returns a web page containing the recent activity report.
	 * The format query parameter specifies the format of the report: html, xml, or csv (default = html)
	 */
	public void doGet( HttpRequest req, HttpResponse res ) throws Exception {

		//Require authentication
		if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		if (path.length() == 1) {
			Document doc = ActivityDB.getInstance().getXML();
			String format = req.getParameter("format", "html");

			if (format.equals("xml")) {
				res.setContentType("xml");
				res.write( XmlUtil.toString(doc) );
			}

			else {
				String report = XmlUtil.toString(doc.getDocumentElement());
				try { report = URLEncoder.encode(report, "UTF-8"); }
				catch (Exception ex) { report = ""; }

				String[] params = new String[] {
					"report",		report
				};
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/activity/ActivityReport.xsl" ) );
				res.write( XmlUtil.getTransformedText( doc, xsl, params) );
				res.setContentType("html");
			}
			res.disableCaching();
			res.send();
		}
		else super.doGet(req, res);
	}

}


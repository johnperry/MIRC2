/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.activity;

import java.io.File;
import java.net.URLEncoder;

import mirc.MircConfig;
import mirc.prefs.Preferences;

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

	static long aWeek = 7 * 24 * 3600 * 1000;

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

		Path path = req.getParsedPath();
		if (path.length() == 1) {

			//Require authentication as an admin user
			if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

			//Get the full report
			Document doc = ActivityDB.getInstance().getXML();

			//Now get the summary report
			Document summaryXSL = XmlUtil.getDocument( FileUtil.getStream( "/activity/ActivitySummaryReport.xsl" ) );
			String report = XmlUtil.getTransformedText( doc, summaryXSL, null );

			String format = req.getParameter("format", "html");
			if (format.equals("xml")) {
				res.setContentType("xml");
				if (req.getParameter("type", "").equals("summary")) {
					res.write( report );
				}
				else {
					res.write( XmlUtil.toString(doc) );
				}
			}

			else {
				res.setContentType("html");

				try { report = URLEncoder.encode(report, "UTF-8"); }
				catch (Exception ex) { report = ""; }

				String[] params = new String[] { "report", report };
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/activity/ActivityReport.xsl" ) );
				res.write( XmlUtil.getTransformedText( doc, xsl, params) );
			}
			res.disableCaching();
			res.send();
		}

		else if ((path.length() == 2) && path.element(1).equals("check")) {
			//Return text indicating whether the last report was within the last week.
			long lastReport = ActivityDB.getInstance().getLastReportTime();
			long now = System.currentTimeMillis();
			long age = now - lastReport;
			if (age > aWeek) res.write("old");
			else res.write("recent");
			res.setContentType("txt");
			res.send();
		}

		else if ((path.length() == 2) && path.element(1).equals("update")) {
			//Update the lastReportTime with the current time.
			if (req.userHasRole("admin")) {
				ActivityDB.getInstance().setLastReportTime(System.currentTimeMillis());
				logger.info("Summary report sent from client.");
			}
			res.write("ok");
			res.setContentType("txt");
			res.send();
		}

		else if ((path.length() == 2) && path.element(1).equals("submit")) {

			//Do not require authentication so we can receive reports from remote sites.
			//The only protection is provided by a sanity check in the SummariesDBEntry
			//constructor, which throws an exception if the report doesn't parse, or if
			//its root element has the wrong tag name, or if the site ID is invalid.

			String report = req.getParameter("report");
			try {
				SummariesDBEntry entry = new SummariesDBEntry(report, req.getRemoteAddress());
				ActivityDB.getInstance().put(entry);
				res.write("Thank you for submitting the activity summary report.");
			}
			catch (Exception unable) {
				res.write("Unable to accept the activity summary report.");
			}
			res.setContentType("txt");
			res.send();
		}

		else if ((path.length() == 2) && path.element(1).equals("users")) {

			//Require authentication as an admin user
			if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

			String date = req.getParameter("date");
			String ssid = req.getParameter("ssid");
			try {
				LibraryActivity libact = ActivityDB.getInstance().get(date).getLibraryActivity(ssid);
				Document doc = libact.getUsersDocumentDisplayXML();
				Element prefs = Preferences.getInstance().get("*", true);
				prefs = (Element)doc.importNode(prefs, true);
				doc.getDocumentElement().appendChild(prefs);
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/activity/UsersDocumentDisplayReport.xsl" ) );
				res.write( XmlUtil.getTransformedText(doc, xsl, null) );
				res.setContentType("html");
			}
			catch (Exception unable) {
				res.write("Unable to list the users.");
				res.setContentType("txt");
			}
			res.send();
		}

		else if ((path.length() == 2) && path.element(1).equals("user")) {

			//Require authentication as an admin user
			if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

			String date = req.getParameter("date");
			String ssid = req.getParameter("ssid");
			String username = req.getParameter("username");
			try {
				LibraryActivity libact = ActivityDB.getInstance().get(date).getLibraryActivity(ssid);
				Document doc = libact.getUserDocumentDisplayXML(username);
				Element prefs = Preferences.getInstance().get(username, true);
				prefs = (Element)doc.importNode(prefs, true);
				doc.getDocumentElement().appendChild(prefs);
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/activity/UserDocumentDisplayReport.xsl" ) );
				res.write( XmlUtil.getTransformedText(doc, xsl, null) );
				res.setContentType("html");
			}
			catch (Exception unable) {
				res.write("Unable to list the documents.");
				res.setContentType("txt");
			}
			res.send();
		}

		else if ((path.length() >= 2) && path.element(1).equals("documents")) {

			//Require authentication as an admin user
			if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

			String date = req.getParameter("date");
			String ssid = req.getParameter("ssid");

			String format = path.element(2);
			try {
				LibraryActivity libact = ActivityDB.getInstance().get(date).getLibraryActivity(ssid);
				Document doc = libact.getDocumentsXML();

				if (format.equals("xml")) {
					res.setContentType("xml");
					res.write(XmlUtil.toString(doc));
				}
				else {
					Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/activity/DocumentDisplayReport.xsl" ) );
					String[] params = {
						"date", date,
						"ssid", ssid
					};
					res.write( XmlUtil.getTransformedText(doc, xsl, params) );
					res.setContentType("html");
				}
			}
			catch (Exception unable) {
				res.write("Unable to list the documents report.");
				res.setContentType("txt");
			}
			res.send();
		}

		else if ((path.length() >= 2) && path.element(1).equals("summary")) {

			//This path is only intended for the admin user (at RSNA HQ).
			if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

			Document doc = ActivityDB.getInstance().getSummariesXML();

			String format = path.element(2);
			if (format.equals("xml")) {
				res.setContentType("xml");
				res.write( XmlUtil.toString(doc) );
			}

			else if (format.equals("html")) {
				res.setContentType("html");
				Document summaryXSL = XmlUtil.getDocument( FileUtil.getStream( "/activity/ActivitySummaryReportHTML.xsl" ) );
				res.write( XmlUtil.getTransformedText(doc, summaryXSL, null) );
			}

			else {
				res.setContentType("csv");
				res.setContentDisposition( new File("ActivitySummary.csv") );
				Document summaryXSL = XmlUtil.getDocument( FileUtil.getStream( "/activity/ActivitySummaryReportCSV.xsl" ) );
				res.write( XmlUtil.getTransformedText(doc, summaryXSL, null) );
			}
			res.send();
		}

		else super.doGet(req, res);
	}

}


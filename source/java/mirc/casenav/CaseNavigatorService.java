/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.casenav;

import java.io.File;

import mirc.MircConfig;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Case Navigator Service.
 * This service accepts a GET containing a list of URLs and returns a Case Navigator page.
 */
public class CaseNavigatorService extends Servlet {

	static final Logger logger = Logger.getLogger(CaseNavigatorService.class);

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a CaseNavigatorService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public CaseNavigatorService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a web page containing a Case Navigator for a list of URLs.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		String suppressHome = req.getParameter("suppressHome", "no");
		String urlsParam = req.getParameter("urls");

		if (urlsParam != null) {

			String[] urls = urlsParam.split("\\|");

			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Results");
			doc.appendChild(root);

			boolean ok = false;
			for (String url : urls) {
				url = url.trim();
				if (!url.equals("")) {
					Element md = doc.createElement("MIRCdocument");
					md.setAttribute("docref", url);
					root.appendChild(md);
					ok = true;
				}
			}

			if (ok) {
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/query/CaseNavigatorResult.xsl" ) );

				String[] cnrParms = {
							"suppressHome", suppressHome,
							"homeURL", "/query",
							"nextURL", "",
							"prevURL", "",
							"randomize", "no"};

				res.write( XmlUtil.getTransformedText( doc, xsl, cnrParms ) );
				res.disableCaching();
				res.setContentType("html");
				res.send();
				return;
			}
		}
		res.redirect("/query");
	}
}

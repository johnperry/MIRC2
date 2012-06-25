/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.login;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import mirc.MircConfig;
import org.apache.log4j.Logger;
import org.rsna.server.Authenticator;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.servlets.Servlet;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;

/**
 * The ChallengeServlet, a Servlet to provide a challenge if the
 * user attempts to access a protected resource.
 */
public class ChallengeServlet extends Servlet {

	static final Logger logger = Logger.getLogger(ChallengeServlet.class);

	/**
	 * Construct a MircLoginServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public ChallengeServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * The GET handler: display a page identifying the resource
	 * being requested and containing a form allowing the
	 * user to log in.
	 * @param req the request object
	 * @param res the response object
	 */
	public void doGet(HttpRequest req, HttpResponse res) {
		MircConfig mc = MircConfig.getInstance();
		String[] params = {
			"url",			req.getParameter("url", ""),
			"ssid",			req.getParameter("ssid", "")
		};
		InputStream in = FileUtil.getStream("/login/Challenge.xsl");
		try {
			Document xsl = XmlUtil.getDocument(in);
			res.write( XmlUtil.getTransformedText( mc.getXML(), xsl, params ) );
		}
		catch (Exception ex) { res.setResponseCode(res.notfound); }
		res.setContentType("html");
		res.disableCaching();
		res.send();
	}

	/**
	 * The POST handler: authenticate the user from the form parameters
	 * and redirect to the originally requested resource.
	 * @param req the request object
	 * @param res the response object
	 */
	public void doPost(HttpRequest req, HttpResponse res) {
		String username = req.getParameter("username");
		String password = req.getParameter("password");
		login(req, res, username, password);
		String url = req.getParameter("url", "");
		if (url.equals("")) url = "/";
		res.redirect(url);
	}

	//Attempt a login and return true if it succeeded.
	private boolean login(
						HttpRequest req, HttpResponse res,
						String username, String password) {
		Authenticator authenticator = Authenticator.getInstance();

		boolean passed = false;
		if ((username != null) && (password != null)) {
			User user = Users.getInstance().authenticate(username, password);
			if (user != null) {
				passed = authenticator.createSession(user, req, res);
			}
		}
		if (!passed) authenticator.closeSession(req, res);
		return passed;
	}

}

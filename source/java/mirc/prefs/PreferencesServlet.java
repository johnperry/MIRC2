/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.prefs;

import mirc.MircConfig;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.server.UsersXmlFileImpl;

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The MIRC Preferences Servlet.
 * The Preferences Servlet provides a UI for users to enter
 * their preferences. It also provides a RESTful service for
 * client-side applications to obtain preference information.
 */
public class PreferencesServlet extends Servlet {

	static final Logger logger = Logger.getLogger(PreferencesServlet.class);

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available. This method is only
	 * called once as the server starts.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a PreferencesServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public PreferencesServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a web page containing a submission form for the preferences.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Require authentication
		if (req.isFromAuthenticatedUser()) {

			Path path = req.getParsedPath();

			MircConfig mc = MircConfig.getInstance();
			String username = req.getUser().getUsername();
			Preferences prefs = Preferences.getInstance();
			boolean userIsAdmin = req.userHasRole("admin");

			Element pref = null;

			if (path.length() == 1) {

				//This is a request for the user's preferences page.
				//Get the UI to determine whether to include the home icon.
				String pageUI = req.getParameter("pageui", "classic");

				//Get the default UI for the site
				String defUI = mc.getUI();
				String[] params = new String[] { "pageUI", pageUI, "defUI", defUI };

				pref = prefs.get(username, false);
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/prefs/PreferencesServlet.xsl" ) );
				res.setContentType("html");
				res.disableCaching();
				res.write( XmlUtil.getTransformedText( pref.getOwnerDocument(), xsl, params ) );
				res.send();
				return;
			}

			else if (path.element(1).equals("xml")) {
				String path2 = path.element(2);
				String path3 = path.element(3);

				prefs.syncToUsers();

				//This is a request for an XML element
				//If the user is an admin, allow a request for the full preferences
				if (path2.equals("allusers")) {
					pref = prefs.get("*", !userIsAdmin || !path3.equals("full"));
				}
				else if (path2.equals("user") && userIsAdmin && !path3.equals("")) {
					pref = prefs.get(path3, true);
				}
				else {
					pref = prefs.get(username, true);
				}
				res.setContentType("xml");
				if (pref != null) res.write( XmlUtil.toString( pref ) );
				else res.write( "<User/>" );
				res.disableCaching();
				res.send();
				return;
			}
		}
		super.doGet( req, res ); //handle other requests in the superclass
	}

	/**
	 * Handle a preferences submission.
	 */
	public void doPost(HttpRequest req, HttpResponse res ) throws Exception {

		//Require authentication
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		MircConfig mc = MircConfig.getInstance();
		Preferences prefs = Preferences.getInstance();

		if (path.length() == 1) {
			//This is an update from the user

			String username = req.getUser().getUsername();

			//Set the UI preference
			String ui = req.getParameter("UI", "classic");
			prefs.setUI(username, ui);

			//Set the author preferences
			String name = req.getParameter("name", "");
			String affiliation = req.getParameter("affiliation", "");
			String contact = req.getParameter("contact", "");
			prefs.setAuthorInfo(username, name, affiliation, contact);

			//Set the myRSNA preferences
			String myrsnaEnabled = req.getParameter("myrsna", "no");
			String myrsnaUsername = req.getParameter("myrsnaUsername", "");
			String myrsnaPassword = req.getParameter("myrsnaPassword", "");
			prefs.setMyRsnaInfo(username, myrsnaEnabled, myrsnaUsername, myrsnaPassword);

			//Set the export site preferences
			LinkedList<ExportSite> list = new LinkedList<ExportSite>();
			int n = 0;
			String exportName;
			while ( (exportName=req.getParameter("export-name["+n+"]")) != null ) {
				String exportURL = req.getParameter("export-url["+n+"]", "").trim();
				String exportUN = req.getParameter("export-un["+n+"]", "").trim();
				String exportPW = req.getParameter("export-pw["+n+"]", "").trim();
				exportName = exportName.trim();
				if (!exportName.equals("") && !exportURL.equals("")) {
					list.add( new ExportSite(exportName, exportURL, exportUN, exportPW) );
				}
				n++;
			}
			ExportSite[] sites = list.toArray( new ExportSite[list.size()] );
			Arrays.sort(sites);
			prefs.setExportInfo(username, sites);

			//Handle password changes
			String pw1 = req.getParameter("password1", "").trim();
			String pw2 = req.getParameter("password2", "").trim();
			if ( pw1.equals(pw2) && !pw1.equals("")) {
				Users users = Users.getInstance();
				if (users instanceof UsersXmlFileImpl) {
					User user = req.getUser();
					user.setPassword( users.convertPassword(pw1) );
					((UsersXmlFileImpl)users).addUser(user);
				}
			}

			//Now return the page again
			doGet( req, res );
		}

		else if (path.element(1).equals("admin")) {
			//This is an update of an individual user by the User Manager.
			//All we handle are the name, affiliation, and contact.
			String username = req.getParameter("username","").trim();

			String name = req.getParameter("namePref","").trim();
			String affiliation = req.getParameter("affiliationPref","").trim();
			String contact = req.getParameter("contactPref","").trim();

			if (!username.equals("")) {
				Element userPref = prefs.get(username, true);
				if (userPref != null) {
					//If the user already exists in the preferences
					//and a new one is not specified in this submission,
					//then keep the old value.
					if (name.equals("")) name = userPref.getAttribute("name");
					if (affiliation.equals("")) affiliation = userPref.getAttribute("affiliation");
					if (contact.equals("")) contact = userPref.getAttribute("contact");
				}
				prefs.setAuthorInfo(username, name, affiliation, contact);
				res.write("<ok/>");
			}
			else res.write("<notok/>");
			res.setContentType("xml");
			res.send();
		}
	}

}
/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.users;

import java.io.File;
import java.util.*;
import mirc.prefs.Preferences;
import org.apache.log4j.Logger;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.server.UsersXmlFileImpl;
import org.rsna.servlets.Servlet;
import org.rsna.util.FileUtil;
import org.rsna.util.HtmlUtil;
import org.w3c.dom.Element;

/**
 * The User Manager Servlet for MIRC.
 * This servlet provides a browser-accessible user interface for
 * editing the users.xml file.
 */
public class MircUserManagerServlet extends Servlet {

	static final Logger logger = Logger.getLogger(MircUserManagerServlet.class);

	/**
	 * Construct a MircUserManagerServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public MircUserManagerServlet(File root, String context) {
		super(root, context);
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * This method returns an HTML page containing a form for
	 * adding, removing, and changing users, roles and their
	 * relationships. The initial contents of the form are
	 * constructed from the contents of the Tomcat/conf/tomcat-users.xml
	 * file.
	 * @param req the request object.
	 * @param res the response object.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Make sure the user is authorized to do this.
		String home = req.getParameter("home", "/");
		if (!req.userHasRole("admin")) { res.redirect(home); return; }

		//Get the Users object.
		Users users = Users.getInstance();

		//Make sure that this system is using the XML implementation.
		if (!(users instanceof UsersXmlFileImpl)) {
			res.setResponseCode(404);
			res.send();
			return;
		}

		if (req.getParsedPath().length() == 1) {
			//Make the page and return it.
			res.write( getPage( (UsersXmlFileImpl)users, home ) );
			res.setContentType("html");
			res.disableCaching();
			res.send();
		}
		else {
			//Service a file request
			super.doGet(req, res);
		}
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * This method interprets the posted parameters as a new set
	 * of users and roles and constructs a new users.xml
	 * file. It then returns an HTML page containing a new form
	 * constructed from the new contents of the file.
	 * @param req the request object.
	 * @param res the response object.
	 */
	public void doPost(HttpRequest req, HttpResponse res) {

		//Make sure the user is authorized to do this.
		String home = req.getParameter("home", "/");
		if (!req.userHasRole("admin")) { res.redirect(home); return; }

		boolean canShutdown = req.userHasRole("shutdown") || req.isFromLocalHost();

		//Get the Users object.
		Users users = Users.getInstance();

		//Make sure that this system is using the XML implementation.
		if (!(users instanceof UsersXmlFileImpl)) {;
			res.setResponseCode(404);
			res.send();
			return;
		}
		UsersXmlFileImpl usersXmlFileImpl = (UsersXmlFileImpl)users;

		//Get the parameter names and values
		String[] params = req.getParameterNames();
		String[] values = new String[params.length];
		for (int i=0; i<params.length; i++) {
			values[i] = req.getParameter(params[i]);
		}

		//Get the number of users and the number of roles
		int nUsers = getMaxIndex(params,"u") + 1;
		int nRoles = getMaxIndex(params,"r") + 1;

		//Get the names in a convenient array.
		String[] roleNames = new String[nRoles];
		for (int i=0; i<nRoles; i++) {
			roleNames[i] = getValue(params,values,"r",i);
		}

		//Make a new table to store the users we are now creating.
		Hashtable<String,User> newUserTable = new Hashtable<String,User>();

		//If the current user does not have the shutdown role, then he
		//cannot modify users with the shutdown role, so copy all the
		//shutdown users into the newUserTable in order to prevent the
		//current user from deleting them.
		if (!canShutdown) {
			String[] usernames = usersXmlFileImpl.getUsernames();
			for (int i=0; i<usernames.length; i++) {
				User user = usersXmlFileImpl.getUser(usernames[i]);
				if (user.hasRole("shutdown")) newUserTable.put(usernames[i],user);
			}
		}

		//Process all the input.
		for (int i=0; i<nUsers; i++) {
			String username = getValue(params,values,"u",i);
			if (!username.equals("")) {
				//Get the old user or create a new one if the old one doesn't exist.
				User user = usersXmlFileImpl.getUser(username);
				if (user == null) user = new User(username, "");

				//(Only process existing users with the shutdown
				//role if the current user has the shutdown role.)
				if (canShutdown || !user.hasRole("shutdown")) {
					//Update the password and roles.
					String pw = getValue(params,values,"p",i).trim();
					if (!pw.equals("")) user.setPassword( usersXmlFileImpl.convertPassword(pw) );
					for (int j=0; j<nRoles; j++) {
						String role = getValue(params,values,"cb",i,j);
						if (canShutdown || !roleNames[j].equals("shutdown")) {
							if (!role.equals("")) user.addRole(roleNames[j]);
							else user.removeRole(roleNames[j]);
						}
					}
					newUserTable.put(username,user);
				}
			}
		}

		//Reset the users database from the hashtable.
		usersXmlFileImpl.resetUsers(newUserTable);

		//Make a new page from the new data and return it.
		res.write(getPage(usersXmlFileImpl, home));
		res.setContentType("html");
		res.disableCaching();
		res.send();
	}

	//Get the value of named parameter [i]
	private String getValue(String[] params, String[] values, String prefix, int i) {
		String name = prefix+i;
		return getValueFromName(params,values,name);
	}

	//Get the value of named parameter [i,j]
	private String getValue(String[] params, String[] values, String prefix, int i, int j) {
		String name = prefix + "u" + i + "r" + j;
		return getValueFromName(params,values,name);
	}

	//Get the value of the named parameter.
	private String getValueFromName(String[] params, String[] values, String name) {
		for (int i=0; i<params.length; i++) {
			if (params[i].equals(name)) {
				String value = values[i];
				if (value == null) return "";
				return filter(value.trim());
			}
		}
		return "";
	}

	//Filter a string for cross-site scripting characters (<>)
	private String filter(String s) {
		return s.replaceAll("<[^>]*>","");
	}

	//Find the maximum index value of a named parameter
	private int getMaxIndex(String[] params, String prefix) {
		int max = 0;
		int v;
		for (int i=0; i<params.length; i++) {
			if (params[i].startsWith(prefix)) {
				try {
					String rest = params[i].substring(prefix.length());
					v = Integer.parseInt(rest);
					if (v > max) max = v;
				}
				catch (Exception skip) {
					logger.debug("Unparsable param value: \""+params[i]+"\"");
				}
			}
		}
		return max;
	}

	//Create an HTML page containing the form for managing
	//the users and roles.
	private String getPage(UsersXmlFileImpl users, String home) {
		String[] usernames = users.getUsernames();
		String[] rolenames = users.getRoleNames();

		StringBuffer sb = new StringBuffer();
		responseHead(sb, home);
		makeTableHeader(sb, rolenames);
		makeTableRows(sb, users, usernames, rolenames);
		responseTail(sb);

		return sb.toString();
	}

	private void makeTableHeader(StringBuffer sb, String[] rolenames) {
		sb.append( "<thead>\n"
					+ " <tr>\n"
					+ "  <th/>\n"
					+ "  <th/>\n" );
		for (int i=0; i<rolenames.length; i++) {
			sb.append( "  <th class=\"thv\"><nobr>"+"<input type=\"checkbox\" onclick=\"toggleRoles("+i+",event)\"/>&nbsp;"+rolenames[i]+"</nobr>"
				  	+  "<input name=\"r"+i+"\" type=\"hidden\" value=\""+rolenames[i]+"\"/></th>\n" );
		}
		sb.append( " </tr>\n" );
		sb.append( "</thead>\n" );
	}

	private void makeTableRows(
					StringBuffer sb,
					UsersXmlFileImpl users,
					String[] usernames,
					String[] rolenames) {
		Preferences prefs = Preferences.getInstance();
		for (int i=0; i<usernames.length; i++) {

			String realName = "";
			if (prefs != null) {
				Element info = prefs.get(usernames[i], true);
				if (info != null) realName = info.getAttribute("name");
			}

			sb.append( "<tr>\n" );
			sb.append( " <td class=\"tdtext\">" + realName + "</td>\n" );
			sb.append( " <td class=\"tdl\">"
					 	+  "<input name=\"u"+i+"\" value=\""+usernames[i]+"\"/>"
					 	+  "</td>\n" );
			for (int j=0; j<rolenames.length; j++) {
				sb.append( "<td><input name=\"cbu"+i+"r"+j+"\" type=\"checkbox\"" );
				if ((users.getUser(usernames[i]).hasRole(rolenames[j]))) sb.append( " checked=\"true\"" );
				sb.append( "/></td>\n" );
			}
			sb.append( " <td class=\"tdl\">"
					 +  "<input name=\"p"+i+"\" type=\"password\" value=\"\"/>"
					 +  "</td>\n" );
			sb.append( " <td><input type=\"button\" value=\" + \" onclick=\"setPrefs();\" title=\"Set user info\"/>\n" );
			sb.append( " </tr>\n" );
		}
		sb.append( "<tr>\n" );
		sb.append( "<td/>\n" );
		sb.append( "<td class=\"tdl\"><input name=\"u"+usernames.length+"\"/></td>\n" );
		for (int j=0; j<rolenames.length; j++) {
			sb.append( "<td><input name=\"cbu"+usernames.length+"r"+j+"\" type=\"checkbox\"/></td>\n" );
		}
		sb.append( " <td class=\"tdl\"><input name=\"p"+usernames.length+"\"/></td>\n" );
		sb.append( " </tr>\n" );
	}

	private void responseHead(StringBuffer sb, String home) {
		sb.append(
				"<html>\n"
			+	" <head>\n"
			+	"  <title>User Manager</title>\n"
			+	"  <link rel=\"Stylesheet\" type=\"text/css\" media=\"all\" href=\"/JSPopup.css\"></link>\n"
			+	"  <link rel=\"Stylesheet\" type=\"text/css\" media=\"all\" href=\"/UserManagerServlet.css\"></link>\n"
			+	"  <link rel=\"Stylesheet\" type=\"text/css\" media=\"all\" href=\"/users/MircUserManagerServlet.css\"></link>\n"
			+	"  <script> var home = \""+home+"\";</script>\n"
			+	"  <script language=\"JavaScript\" type=\"text/javascript\" src=\"/JSUtil.js\">;</script>\n"
			+	"  <script language=\"JavaScript\" type=\"text/javascript\" src=\"/JSPopup.js\">;</script>\n"
			+	"  <script language=\"JavaScript\" type=\"text/javascript\" src=\"/UserManagerServlet.js\">;</script>\n"
			+	"  <script language=\"JavaScript\" type=\"text/javascript\" src=\"/users/MircUserManagerServlet.js\">;</script>\n"
			+	" </head>\n"
			+	" <body>\n"

			+	"  <div style=\"float:right;\">\n"
			+	"   <img src=\"/icons/home.png\"\n"
			+	"    onclick=\"window.open('"+home+"','_self');\"\n"
			+	"    style=\"margin-right:2px;\"\n"
			+	"    title=\"Return to the home page\"/>\n"
			+	"   <br>\n"
			+	"   <img src=\"/icons/save.png\"\n"
			+	"    onclick=\"save();\"\n"
			+	"    style=\"margin-right:2px;\"\n"
			+	"    title=\"Save\"/>\n"
			+	"  </div>\n"

			+	"  <center>\n"
			+	"   <h1>User Manager</h1>\n"
			+	"   <input type=\"button\" onclick=\"showHideColumns()\" id=\"shRoles\" value=\"Hide Unused Roles\"/>\n"
			+	"   <form id=\"formID\" action=\"/users\" method=\"post\" accept-charset=\"UTF-8\" action=\"\">\n"
			+	"    <input type=\"hidden\" name=\"home\" value=\""+home+"\">\n"
			+	"    <table id=\"userTable\" border=\"1\">\n"
		);
	}

	private void responseTail(StringBuffer sb) {
		sb.append(
				"    </table>\n"
			+	"   </form>\n"
			+	"  </center>\n"
			+	" </body>\n"
			+	"</html>\n"
		);
	}

}












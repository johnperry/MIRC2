/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.qsadmin;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import mirc.MircConfig;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.Users;
import org.rsna.servlets.Servlet;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The Query Service Admin servlet. This servlet provides
 * a browser-accessible user interface for managing the
 * MIRC configuration. This servlet responds to both
 * HTTP GET and POST.
 */
public class QueryServiceAdmin extends Servlet {

	static final Logger logger = Logger.getLogger(QueryServiceAdmin.class);

	/**
	 * Construct a QueryServiceAdmin servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public QueryServiceAdmin(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Close down the servlet.
	 */
	public void destroy() {
		//do nothing for now
	}

	/**
	 * Get MIRC configuration information.
	 * It the path is /qsadmin, an HTML page for updating the MircConfig
	 * object is returned.
	 * If the path is /qsadmin/config, XML text of the MIRC config file is
	 * returned.
	 * For any other path, the request is delegated to the superclass.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
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
			else if ((path.length() == 2) && path.element(1).equals("config")) {
				//This is a request for the config file
				Element root = MircConfig.getInstance().getXML().getDocumentElement();
				res.setContentType("xml");
				res.write( XmlUtil.toPrettyString(root) );
				res.send();
			}
			else { super.doGet(req, res); return; }
		}
		else res.redirect("/query");
	}

	/**
	 * Update the MircConfig object.
	 * If the path is /qsadmin/setcod, the case of the day field is
	 * updated from the title, image, and link parameters.
	 * For any other path, the MIRC config file is updated from
	 * the parameters supplied by the admin page, and a new admin
	 * page is returned allowing further changes.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		res.disableCaching();
		if (!req.userHasRole("admin")) { res.redirect("/query"); return; }

		MircConfig mc = MircConfig.getInstance();

		//See if this is a case of the day submission
		Path path = req.getParsedPath();
		if ((path.length() == 2) && path.element(1).equals("setcod")) {
			String title = req.getParameter("title", "");
			String link = req.getParameter("link", "");
			String image = req.getParameter("image", "");
			mc.setNews(title, image, link);
			res.setContentType("txt");
			res.write("OK");
			res.send();
			return;
		}

		String oldLocalAddress = mc.getLocalAddress();

		//Get the primary configuration parameters
		mc.setPrimarySystemParameters(
			req.getParameter("mode"),
			req.getParameter("sitename"),
			req.getParameter("showsitename"),
			req.getParameter("masthead"),
			req.getParameter("showptids"),
			req.getParameter("siteurl"),
			req.getParameter("addresstype"),
			req.getParameter("disclaimerurl"),
			req.getParameter("timeout"),
			req.getParameter("roles"),
			req.getParameter("UI")
		);

		//Install any newly defined roles
		mc.setDefinedRoles();

		//See if we should remove the case of the day
		if (req.getParameter("removecod") != null) {
			mc.deleteNews();
		}

		String newLocalAddress = mc.getLocalAddress();

		//Update the servers
		int i = 0;
		Server server;
		while ((server = getServer(req, i++, newLocalAddress, oldLocalAddress)) != null) {
			if (!server.address.equals("")) {
				if (!server.name.equals("")) {
					Element lib = mc.getLibrary(server.prevadrs);
					if (lib != null) {
						//This is an update of an existing library.
						lib.setAttribute("address", server.address);
						lib.setAttribute("enabled", server.enabled);
						Element title = XmlUtil.getFirstNamedChild(lib, "title");
						title.setTextContent(server.name);
						mc.removeLibrary(server.prevadrs);
						mc.insertLibrary(lib);
					}
					else {
						//This is a new library.
						//Note: the user is not allowed to create local libraries;
						//they must be created on the storage service admin page
						//so the address is set correctly.
						if (!server.isLocal) {
							mc.insertLibrary( server.createLibrary() );
						}
					}
				}
				else {
					//The server name was blanked; remove the library.
					//NOTE: users are not allowed to remove local libraries
					//through the query service admin page - only through
					//the storage service admin page.
					if (!server.prevadrs.equals("") && !server.isLocal) {
						mc.removeLibrary(server.prevadrs);
					}
				}
			}
			else {
				//The server address was blanked; remove the library.
				if (!server.prevadrs.equals("")) {
					mc.removeLibrary(server.prevadrs);
				}
			}
		}
		mc.sortLibraries(); //Note: this does an automatic save and load

		//Return the updated configuration page
		doGet(req, res);
	}

	private String getPage() throws Exception {
		Document xml = MircConfig.getInstance().getXML();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/qsadmin/QueryServiceAdmin.xsl" ) );
		return XmlUtil.getTransformedText(xml, xsl, null);
	}

	private Server getServer(HttpRequest req, int i, String newLocalAddress, String oldLocalAddress) {
		String enb = req.getParameter("enb"+i, "no");
		enb = ((enb!=null) && enb.trim().equals("yes")) ? "yes" : "no";
		String adrs = req.getParameter("adrs"+i);
		String prevadrs = req.getParameter("prevadrs"+i);
		String name = req.getParameter("name"+i);
		if ((adrs==null) || (name==null)) return null;
		return new Server(name, adrs, prevadrs, enb, newLocalAddress, oldLocalAddress);
	}

	class Server {
		public String name;
		public String address;
		public String prevadrs;
		public String enabled;
		public boolean isLocal = false;

		public Server(String name,
					  String address,
					  String prevadrs,
					  String enabled,
					  String newLocalAddress,
					  String oldLocalAddress) {
			this.name = name.trim();
			this.address = address.trim();
			this.prevadrs = (prevadrs != null) ? prevadrs.trim() : address.trim();
			this.enabled = enabled.trim();
			if (this.address.startsWith(newLocalAddress)) {
				this.address = this.address.substring(newLocalAddress.length());
				isLocal = true;
			}
			else if (this.address.startsWith("/")) {
				isLocal = true;
			}
			else if (this.address.startsWith(oldLocalAddress)) {
				this.address = this.address.substring(oldLocalAddress.length());
				isLocal = true;
			}
		}

		public Element createLibrary() throws Exception {
			return MircConfig.getInstance().createLibrary(name, address, enabled);
		}

	}
}

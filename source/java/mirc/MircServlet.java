/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc;

import java.io.File;
import mirc.MircConfig;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.Users;
import org.rsna.server.UsersXmlFileImpl;
import org.rsna.servlets.Servlet;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A base servlet to provide access to files used in many MIRC servlets.
 * This servlet also provides a redirector to the query service if it
 * is called with no additional path information.
 */
public class MircServlet extends Servlet {

	/**
	 * Construct a MircServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public MircServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Handle requests for MIRC files (mostly icon images and buttons) for web pages.
	 * If the path is /mirc, redirect the request to the query page.
	 * For any other path, the request is delegated to the superclass.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		Path path = req.getParsedPath();
		int length = path.length();

		if (length == 1) { res.redirect("/query"); return; }

		MircConfig mc = MircConfig.getInstance();
		String function = path.element(1);

		if (function.equals("roles")) {
			res.disableCaching();
			String[] defRoles = mc.getDefinedRoles();
			if (path.element(2).equals("xml")) {

				Document doc = XmlUtil.getDocument();
				Element root = doc.createElement("MIRC");
				Element dRoles = doc.createElement("DefinedRoles");
				root.appendChild(dRoles);
				appendRoles(defRoles, dRoles);

				Users users = Users.getInstance();
				if (users instanceof UsersXmlFileImpl) {
					String[] allRoles = ((UsersXmlFileImpl)users).getRoleNames();
					Element aRoles = doc.createElement("AllRoles");
					root.appendChild(aRoles);
					appendRoles(allRoles, aRoles);
				}

				res.write(XmlUtil.toPrettyString(root));
				res.setContentType("xml");
			}
			else {
				StringBuffer sb = new StringBuffer();
				for (int i=0; i<defRoles.length; i++) {
					if (i != 0) sb.append(",");
					sb.append(defRoles[i]);
				}
				res.write(sb.toString());
				res.setContentType("txt");
			}
			res.send();
		}
		else if (function.equals("version")) {
			Element mircRoot = mc.getXML().getDocumentElement();
			if (req.getParsedPath().element(2).equals("xml")) {
				Document xml = XmlUtil.getDocument();
				Element mirc = xml.createElement("mirc");
				xml.appendChild(mirc);
				mirc.setAttribute("version", mircRoot.getAttribute("version"));
				mirc.setAttribute("date", mircRoot.getAttribute("date"));
				res.write( XmlUtil.toPrettyString( mirc ) );
				res.setContentType("xml");
			}
			else {
				res.write( mircRoot.getAttribute("version") );
				res.setContentType("txt");
			}
			res.send();
		}
		else if (function.equals("libraries")) {
			Element libs = mc.getLibraries(true);
			if (libs != null) res.write( XmlUtil.toPrettyString(libs) );
			else res.setResponseCode(res.notfound);
			res.setContentType("xml");
			res.send();
		}
		else if (function.equals("find")) {
			String id = req.getParameter("id");
			Element libs = mc.getEnabledLibraries();
			NodeList nl = libs.getElementsByTagName("Library");
			for (int i=0; i<nl.getLength(); i++) {
				Element lib = (Element)nl.item(i);
				if (lib.getAttribute("id").equals(id)) {
					res.write( XmlUtil.toString(lib) );
					res.setContentType("xml");
					res.send();
					return;
				}
			}
			res.setResponseCode( res.notfound );
			res.send();
		}
		else super.doGet(req, res);
	}

	private void appendRoles(String[] roles, Element parent) {
		Document doc = parent.getOwnerDocument();
		for (String role : roles) {
			role = role.trim();
			if (!role.equals("")) {
				Element r = doc.createElement(role);
				parent.appendChild(r);
			}
		}
	}
}

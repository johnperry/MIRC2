/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.confs;

import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;

import mirc.MircConfig;

import org.apache.log4j.Logger;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The MIRC Conference Service servlet.
 * The Conference Service provides personal and shared conferences.
 * This servlet responds to both HTTP GET and POST.
 */
public class ConferenceService extends Servlet {

	static final Logger logger = Logger.getLogger(ConferenceService.class);

	/**
	 * Construct a ConferenceService.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public ConferenceService(File root, String context) {
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
	 * been created and is available. This method is only
	 * called once as the server starts.
	 */
	public static void init(File root, String context) {
		Conferences.load( MircConfig.getInstance().getRootDirectory() );
	}

	/**
	 * Clean up as the server shuts down.
	 */
	public void destroy() {
		Conferences.close();
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//This servlet is for authenticated users only
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		//OK, the user is authenticated; get the name.
		String username = req.getUser().getUsername();

		Path path = req.getParsedPath();

		if (path.length() == 1) {
			//This is a request for the conferences page.
			//Get the HTML file from either the disk or the jar.
			File confs = new File(root, "confs");
			File htmlFile = new File(confs, "ConferenceService.html");
			InputStream is = FileUtil.getStream(htmlFile, "/confs/ConferenceService.html");
			String page = FileUtil.getText(is);

			//Put in the parameters
			Properties props = new Properties();
			props.setProperty("username", username);
			String openpath = req.getParameter("openpath", "");
			props.setProperty("openpath", openpath);
			String suppressHome = req.getParameter("suppressHome", "no");
			props.setProperty("suppressHome", suppressHome);
			page = StringUtil.replace(page, props);

			//Send the page
			res.disableCaching();
			res.setContentType("html");
			res.write(page);
			res.send();
			return;
		}

		String function = path.element(1).toLowerCase();

		if (function.equals("tree")) {
			//This is a request for the tree structure for the left pane.
			Conferences confs = Conferences.getInstance();
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("tree");
			doc.appendChild(root);
			appendConferences(root, confs.getRootConference(null));
			appendConferences(root, confs.getRootConference(username));
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString(root) );
			res.send();
			return;
		}

		if (function.equals("createconference")) {
			//This a request to create a conference and return the tree of its parent
			String id = req.getParameter("id"); //the id of the parent of the created conference
			String name = req.getParameter("name");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.createConference(username, name, id);
			if (conf != null) {
				Document doc = XmlUtil.getDocument();
				Element root = doc.createElement("tree");
				doc.appendChild(root);
				appendConferences(root, confs.getConference(id));
				res.disableCaching();
				res.setContentType("xml");
				res.write( XmlUtil.toString(root) );
				res.send();
				return;
			}
			return;
		}

		if (function.equals("deleteconference")) {
			//This a request to delete a conference and return the tree of its parent.
			String id = req.getParameter("id");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(id);
			String parentID = conf.pid;
			confs.deleteConference(id);
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("tree");
			doc.appendChild(root);
			appendConferences(root, confs.getConference(parentID));
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString(root) );
			res.send();
			return;
		}

		if (function.equals("renameconference")) {
			//This a request to rename a conference
			String id = req.getParameter("id");
			String name = req.getParameter("name");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(id);
			conf.title = name;
			String result = confs.setConference(conf) ? "<ok/>" : "<notok/>";
			res.disableCaching();
			res.setContentType("xml");
			res.write( result );
			res.send();
			return;
		}

		if (function.equals("appendagendaitem")) {
			//This a request to append a new agenda item to a conference
			String nodeID = req.getParameter("nodeID");
			String url = req.getParameter("url");
			MircConfig mc = MircConfig.getInstance();
			String title = req.getParameter("title", "");
			String alturl = req.getParameter("alturl", url);
			String alttitle = req.getParameter("alttitle", "");
			String subtitle = req.getParameter("subtitle", "");

			if (title.trim().equals("")) title = url;
			if (alttitle.trim().equals("")) alttitle = title;

			//If the URL is local, remove the protocol and host information
			//so agenda items continue to work if the IP address changes.
			String siteurl = mc.getLocalAddress();
			if (url.startsWith( siteurl+"/" )) url = url.substring( siteurl.length() );
			if (alturl.startsWith( siteurl+"/" )) alturl = alturl.substring( siteurl.length() );

			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(nodeID);
			AgendaItem item = new AgendaItem(url, title, alturl, alttitle, subtitle);
			conf.appendAgendaItem(item);
			String result = confs.setConference(conf) ? "<ok/>" : "<notok/>";
			res.disableCaching();
			res.setContentType("xml");
			res.write( result );
			res.send();
			return;
		}

		if (function.equals("moveagendaitem")) {
			//This a request to move an agenda item within a conference
			String nodeID = req.getParameter("nodeID");
			String sourceURL = req.getParameter("sourceURL");
			String targetURL = req.getParameter("targetURL");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(nodeID);
			conf.moveAgendaItem(sourceURL, targetURL);
			confs.setConference(conf);
			String result = confs.setConference(conf) ? "<ok/>" : "<notok/>";
			res.disableCaching();
			res.setContentType("xml");
			res.write( result );
			res.send();
			return;
		}

		if (function.equals("transferagendaitem")) {
			//This a request to move an agenda item from one conference to another
			String sourceID = req.getParameter("sourceID");
			String targetID = req.getParameter("targetID");
			String url = req.getParameter("url");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(sourceID);
			AgendaItem aItem = conf.removeAgendaItem(url);
			confs.setConference(conf);
			String result = "<notok/>";
			if (aItem != null) {
				conf = confs.getConference(targetID);
				conf.appendAgendaItem(aItem);
				result = confs.setConference(conf) ? "<ok/>" : "<notok/>";
			}
			res.disableCaching();
			res.setContentType("xml");
			res.write( result );
			res.send();
			return;
		}

		if (function.equals("deleteagendaitems")) {
			//This a request to delete a list of agenda items
			//from a conference. Note: only an error code is
			//returned; the client will make another call to
			//get the modified contents of the conference.
			String nodeID = req.getParameter("nodeID");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(nodeID);

			//Get the list of agenda items.
			String list = req.getParameter("list");
			String[] urls = list.split("\\|");
			//Do everything we can, and ignore errors.
			for (int i=0; i<urls.length; i++) {
				conf.removeAgendaItem(urls[i].trim());
			}
			confs.setConference(conf);
			String result = confs.setConference(conf) ? "<ok/>" : "<notok/>";
			res.disableCaching();
			res.setContentType("xml");
			res.write( result );
			res.send();
			return;
		}

		if (function.equals("getagenda")) {
			//This is a request for an XML structure containing
			//the agenda items of the specified conference
			String nodeID = req.getParameter("nodeID");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(nodeID);

			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("agenda");
			doc.appendChild(root);
			Iterator<AgendaItem> it = conf.agenda.iterator();
			while (it.hasNext()) {
				AgendaItem item = it.next();
				Element el = doc.createElement("item");
				el.setAttribute("url", item.url);
				el.setAttribute("title", item.title);
				el.setAttribute("alturl", item.alturl);
				el.setAttribute("alttitle", item.alttitle);
				el.setAttribute("subtitle", item.subtitle);
				root.appendChild(el);
			}
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString(root) );
			res.send();
			return;
		}

		if (function.equals("casenavigator")) {
			//This is a request to view the current conference
			//in the Case Navigator
			String nodeID = req.getParameter("nodeID");
			Conferences confs = Conferences.getInstance();
			Conference conf = confs.getConference(nodeID);
			Document doc = conf.getCaseNavigatorURLs();
			File query = new File(root, "query");
			File xslFile = new File(query, "CaseNavigatorResult.xsl");
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslFile, "/query/CaseNavigatorResult.xsl" ) );

			String[] cnrParms = {
						"homeURL", "/confs",
						"nextURL", "",
						"prevURL", "",
						"randomize", "no"};

			res.write( XmlUtil.getTransformedText( doc, xsl, cnrParms ) );
			res.disableCaching();
			res.setContentType("html");
			res.send();
			return;
		}

		//This must be a file request; handle it in the superclass.
		super.doGet(req, res);
	}

	//Add a conference and its child conferences to a tree
	private void appendConferences(Node parent, Conference conf) {
		if (conf == null) logger.warn("call to appendConferences(null); ["+((Element)parent).getAttribute("name")+"]");
		if (conf == null) return;
		Element el = parent.getOwnerDocument().createElement("node");
		el.setAttribute("name", conf.title);
		el.setAttribute("nodeID", conf.id);
		el.setAttribute("sclickHandler", "showConferenceContents");
		parent.appendChild(el);
		Iterator<String> it = conf.children.iterator();
		while (it.hasNext()) {
			String id = it.next();
			Conference child = Conferences.getInstance().getConference(id);
			appendConferences(el, child);
		}
	}

}

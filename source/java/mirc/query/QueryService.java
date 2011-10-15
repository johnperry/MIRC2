/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import java.io.File;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import mirc.MircConfig;
import mirc.prefs.Preferences;

import org.apache.log4j.Logger;

import mirc.MircConfig;

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
import org.w3c.dom.NodeList;

/**
 * The MIRC Query Service servlet.
 * The Query Service is the interface into MIRC for users.
 * This servlet responds to both HTTP GET and POST.
 */
public class QueryService extends Servlet {

	static final Logger logger = Logger.getLogger(QueryService.class);

	Set<MircServer> serverThreads = null;
	Document results = null;

	/**
	 * Construct a QueryService that delegates to other Query Services
	 * that provide different types of user interfaces.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public QueryService(File root, String context) {
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
	public static void init(File root, String context) { }

	/**
	 * Dispatch GETs to a QueryService implementing the user's preferred UI.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		MircConfig mc = MircConfig.getInstance();
		Path path = req.getParsedPath();
		int length = path.length();

		//Redirect requests that have Host headers that are different from
		//the URL known to the site. To minimize the danger of collateral
		//damage, only do this for requests that have no extra path or query
		//information.
		if ((length == 1) && req.getQueryString().trim().equals("")) {
			String host = req.getHeader("host");
			if (host != null) {
				String siteurl = mc.getLocalAddress();
				String sitehost = siteurl.substring(7);
				sitehost = sitehost.substring(0, sitehost.indexOf(":"));
				if (!host.startsWith(sitehost)) {
					res.redirect(siteurl + "/" + context);
					return;
				}
			}
		}

		if (length != 1) {
			//This is a file request.
			super.doGet(req, res);
			return;
		}

		//See what kind of GET this is by checking the query string.
		String queryString = req.getQueryString();
		if ((queryString == null) ||
				(queryString.trim().length() == 0) ||
					(req.getParameter("queryUID") == null)) {

			//This is a GET for the query page.
			UI ui = getUI(req);
			res.setContentType("html");
			res.write( ui.getPage() );
			res.send();
		}
		else {
			//The query string is not blank or missing, so this is a GET from
			//the Next or Prev links on the query results page. Service it as
			//a POST of the query (which is encoded in the URL).
			serviceQuery(req, res, "");
		}
	}

	//Get the selected UI from:
	// 1. the request UI parameter, if present
	// 2. the user's preferences, if the user is authenticated
	// 3. the default UI for the site, if specified
	// 4. the Integrated UI, if no other is available.
	private UI getUI(HttpRequest req) {

		Preferences prefs = Preferences.getInstance();
		User user = req.getUser();
		String username = (user != null) ? user.getUsername() : null;

		String ui = req.getParameter("UI");
		if (ui != null) {
			if (ui.equals("integrated")) {
				if (username != null) prefs.setUI( username, ui );
				return new IntegratedUI();
			}
			if (ui.equals("classic")) {
				if (username != null) prefs.setUI( username, ui );
				return new ClassicUI();
			}
		}

		if (req.isFromAuthenticatedUser()) {
			ui = prefs.getUI( req.getUser().getUsername() );
			if (ui.equals("integrated")) return new IntegratedUI();
			if (ui.equals("classic")) return new ClassicUI();
		}

		ui = MircConfig.getInstance().getUI();
		if (ui.equals("classic")) return new ClassicUI();
		return new IntegratedUI();
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * This method interprets the form parameters as a query generated by the
	 * form on the query page and services it accordingly.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		logger.debug("Query received:\n"+req.toString());

		String requestContentType = req.getContentType();
		if (requestContentType.toLowerCase().indexOf("application/x-www-form-urlencoded") >= 0) {

			//Create a UID for this query to help storage services that cache query results.
			String queryUID = req.getRemoteAddress() + "." + (new Date()).getTime();

			//Service the Query
			serviceQuery(req, res, queryUID);
		}
		else {
			res.setContentType("html");
			res.write(getHTMLMessage("Unsupported Content-Type: "+requestContentType));
			res.send();
		}

		logger.debug("...query serviced");
	}

	// This function actually services the query for the doGet and doPost methods.
	// It transforms the form parameters into a MIRCquery XML object,
	// sends the MIRCquery to all the selected MIRC storage services, collates
	// the responses into a single HTML page, and returns the page to the user
	// in the response text.
	//
	// queryUID is a unique identifier of this query. It is used to assist
	// storage services that can cache their results. The queryUID is constructed
	// by the serviceQuery method when a new query is received (as opposed to one
	// that is a next page or prev page query from the query results page).
	//
	void serviceQuery(HttpRequest req, HttpResponse res, String queryUID) throws Exception {

		//The cache control line below is removed. In principle, it would be nice
		//to disallow caching of query results in order to prevent the browser
		//from displaying incorrect results (when the results of the same
		//query would be different because the underlying documents have been
		//changed by some external process). Unfortunately, this causes the
		//browser to give a "Page has expired" error when hitting the Back
		//button from the display of a document. Since this is an important
		//capability, the lesser evil is to allow caching of query results;
		//thus, the following line is commented out:
		//res.disableCaching();

		long requestTime = System.currentTimeMillis();

		//Get the configuration.
		MircConfig mc = MircConfig.getInstance();

		//Get the form contents as an XML DOM object.
		Document formXML = getFormInput(req);

		//If there was no separate queryUID, try to get one from the form.
		if (queryUID.equals("")) {
			try { queryUID = XmlUtil.getValueViaPath(formXML, "formdata/queryUID"); }
			catch (Exception useEmptyQueryUID) { }
		}

		//Create the MIRCquery XML object from the formXML object.
		String[] mircQueryParams = { "queryUID", queryUID };
		String mircQueryString =
			XmlUtil.getTransformedText(
				formXML,
				mc.mircqueryXSL,
				mircQueryParams);

		//Get the configuration
		Document mircXML = MircConfig.getInstance().getXML();

		//Get the timeout and convert to milliseconds.
		int timeout = mc.getQueryTimeout() * 1000;

		//Get the address of the local site
		String siteurl = mc.getLocalAddress();

		//Set up the queryResult Document
		results = XmlUtil.getDocument();
		Element resultsRoot = results.createElement("Results");
		results.appendChild(resultsRoot);

		//Get the session cookie value, it present
		String sessionCookie = req.getCookie("RSNASESSION");

		//Send the mircquery to all the selected servers
		Element[] servers = getSelectedServers(formXML, mircXML);
		serverThreads = new HashSet<MircServer>();
		for (Element server : servers) {
			String address = server.getAttribute("address").trim();
			if (address.startsWith("/")) address = siteurl + address;
			String cookie = (mc.isLocal(address) ? sessionCookie : null);
			String serverName = server.getTextContent().trim();
			synchronized (this) {
				MircServer thread = new MircServer( address, cookie, serverName, mircQueryString, this);
				serverThreads.add( thread );
				thread.start();
			}
		}

		//Wait for the results to come in.
		//The MircServer threads call the acceptQueryResult method,
		//which appends them to the results document.
		long startTime = System.currentTimeMillis();
		int size;
		while ((System.currentTimeMillis()-startTime) < timeout) {
			synchronized (this) { size = serverThreads.size(); }
			if (size > 0) {
				try { Thread.sleep(100); }
				catch (Exception maybeDone) { }
			}
			else break;
		}

		//Shut down any remaining serverThreads
		synchronized (this) {
			for (MircServer server: serverThreads) {
				logger.warn("Aborting "+server.getServerURL());
				server.interrupt();
			}
			serverThreads.removeAll(serverThreads);
		}

		//Return the results document in the requested format
		//There are three formats:
		//	IntegratedUI: the XML document for processing on the client
		//	CaseNavigator: an HTML page containing a frame for viewing result MIRCdocuments directly
		//	ClassicUI: a query results HTML page consisting of a list of links

		if (req.hasParameter("xml")) {
			res.setContentType("xml");
			res.write( XmlUtil.toString( results.getDocumentElement() ) );
			res.send();
			return;
		}

		//The other two formats require transformations; set up the parameters.
		String homeURL = "/query";
		int firstresult = StringUtil.getInt( XmlUtil.getValueViaPath(formXML, "formdata/firstresult") );
		if (firstresult < 1) firstresult = 1;
		int maxresults = StringUtil.getInt( XmlUtil.getValueViaPath(formXML, "formdata/maxresults") );
		String showimages = XmlUtil.getValueViaPath( formXML, "formdata/showimages" );
		String nextURL = makeAnchorURL( formXML, homeURL, queryUID, firstresult+maxresults, showimages );
		String prevURL = makeAnchorURL( formXML, homeURL, queryUID, Math.max(1,firstresult-maxresults), showimages );
		String randomize = XmlUtil.getValueViaPath(formXML, "formdata/randomize");
		String responseTime = getResponseTime( requestTime );
		String mastheadheight = mc.getXML().getDocumentElement().getAttribute("mastheadheight");

		String[] params = new String[] {
			"homeURL", homeURL,
			"nextURL", nextURL,
			"prevURL", prevURL,
			"firstresult", Integer.toString(firstresult),
			"maxresults", Integer.toString(maxresults),
			"showimages", showimages,
			"randomize", randomize,
			"responseTime", responseTime,
			"mastheadheight", mastheadheight
		};

		res.setContentType("html");
		Document xsl = null;
		File queryDir = new File(root, "query");
		if (XmlUtil.getValueViaPath(formXML, "formdata/casenavigator").equals("yes")) {
			xsl = XmlUtil.getDocument( FileUtil.getStream( "/query/CaseNavigatorResult.xsl" ) );
		}
		else {
			xsl = XmlUtil.getDocument( FileUtil.getStream( "/query/MIRCqueryresult.xsl" ) );
		}
		res.write( XmlUtil.getTransformedText(results, xsl, params) );
		res.send();
	}

	public synchronized void acceptQueryResult(MircServer server, Document result) {
		if (result != null) {
			Node importedResult = results.importNode( result.getDocumentElement(), true );
			results.getDocumentElement().appendChild( importedResult );
		}
		else {
			logger.warn("Null result received from "+server.getServerURL());
		}
		serverThreads.remove(server);
	}

	// Create an XML Document from the form data in the POST
	private Document getFormInput(HttpRequest req) {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("formdata");
			doc.appendChild(root);
			String[] names = req.getParameterNames();
			for (String name : names) {
				if (name.equals("server")) {
					String value = req.getParameter(name);
					String[] ids = value.split(":");
					for (String id : ids) {
						Element el = doc.createElement(name);
						el.setTextContent(id);
						root.appendChild(el);
					}
				}
				else if (name.equals("xml")) ; //don't include
				else if (name.equals("timeStamp")) ; //don't include
				else {
					List<String> values = req.getParameterValues(name);
					for (String value : values) {
						value = value.trim();
						if (value.length() != 0) {
							//Protect against oddball characters in the query.
							//Replace things that would need to be coded as entities with spaces.
							value = value.replaceAll("[&<>']"," ");
							//If there is an odd number of " chars, then replace them all with spaces.
							String x = value.replaceAll("[^\"]","");
							if ((x.length() | 1) != 0) value = value.replace("\""," ");
							Element el = doc.createElement(name);
							el.setTextContent(value);
							root.appendChild(el);
						}
					}
				}
			}
			return doc;
		}
		catch (Exception ex) { return null; }
	}

	// Create the URL for the next and prev page buttons on
	// the query results page. The URL has a query string that encodes the
	// current query, but with a different firstresult, allowing for paging.
	private String makeAnchorURL(Document formXML,
								 String serverURL,
								 String queryUID,
								 int firstresult,
								 String showimages) throws Exception {
		String anchorString = serverURL + "?firstresult=" + firstresult + "&showimages=" + showimages;
		NodeList nodeList = formXML.getDocumentElement().getElementsByTagName("server");
		String server = "";
		for (int i=0; i<nodeList.getLength(); i++) {
			if (!server.equals("")) server += ":";
			server += nodeList.item(i).getTextContent().trim();
		}
		anchorString += "&server=" + server;
		nodeList = formXML.getDocumentElement().getElementsByTagName("*");
		for (int i=0; i<nodeList.getLength(); i++) {
			String name = ((Element)(nodeList.item(i))).getTagName();
			if (!name.equals("firstresult") &&
					!name.equals("showimages") &&
					!name.equals("server") &&
					!name.equals("queryUID")) {
				anchorString += "&" + name + "=" + nodeList.item(i).getTextContent().trim();
			}
		}
		if (queryUID.length() != 0) anchorString += "&queryUID=" + queryUID;
		return anchorString;
	}

	// GetTimeString.
	private String getResponseTime(long requestTime) {
		StringBuffer sb = new StringBuffer();

		//Get the date
		Date date = new Date();
		double time = (date.getTime() - requestTime) / 1000.0;
		sb.append(String.format("Total request time = %3.1f seconds", time));

		//... and the current time in Zulu
		Calendar now = Calendar.getInstance();
		TimeZone timeZone = TimeZone.getDefault();
		int zoneOffset = now.get(Calendar.ZONE_OFFSET)/(60*60*1000);
		int hour = now.get(Calendar.HOUR_OF_DAY);
		if (timeZone.inDaylightTime(date)) hour--;
		hour -= zoneOffset;
		if (hour < 0) hour += 24;
		if (hour > 24) hour -= 24;
		int minute = now.get(Calendar.MINUTE);
		if ((hour == 24) && (minute > 0)) hour = 0;
		sb.append(" at " + String.format("%02d%02dZ", hour, minute));

		return sb.toString();
	}

	// Return an array of <server> nodes from the mirc.xml configuration
	// file corresponding to the servers that were selected in the form on the
	// query page.
	private Element[] getSelectedServers(Document formXML, Document mircXML) throws Exception {
		NodeList formNodeList = formXML.getDocumentElement().getElementsByTagName("server");
		Element svrs = MircConfig.getInstance().getEnabledLibraries();
		NodeList mircNodeList = svrs.getElementsByTagName("Library");
		//Handle the case where nothing is selected; return the first node.
		if (formNodeList.getLength() < 1) {
			Element[] s = new Element[1];
			s[0] = (Element)mircNodeList.item(0);
			return s;
		}
		//Handle the case where one or more servers are selected.
		Element[] servers = new Element[formNodeList.getLength()];
		for (int i=0; i<formNodeList.getLength(); i++) {
			servers[i] =
				(Element)mircNodeList.item(
						StringUtil.getInt(formNodeList.item(i).getTextContent().trim()) );
		}
		return servers;
	}

	// Generate an HTML page that lists a message to inform
	// a user that something uncorrectable has happened.
	private String getHTMLMessage(String messageString) {
			return "<html><head><title>Error</title></head><body>"
							+ "<p>" + messageString + "</p></body></html>";
	}
}

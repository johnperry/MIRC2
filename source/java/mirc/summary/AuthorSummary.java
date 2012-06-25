/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.summary;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.storage.IndexEntry;

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
 * A servlet to return a summary of documents produced by authors.
 */
public class AuthorSummary extends Servlet {

	static final Logger logger = Logger.getLogger(AuthorSummary.class);

	/**
	 * Construct an AuthorSummary servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public AuthorSummary(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * <p>
	 * If called with no query string, it returns a web page containing a
	 * form for requesting a summary page. If called with a query string,
	 * it returns a summary in the form specified in the query string.
	 * <p>The query parameters are:
	 * <ul>
	 * <li>start: the first date to accept (YYYYMMDD)
	 * <li>end: the last date to accept (YYYYMMDD)
	 * <li>format: html, xml, or csv (default = html)
	 * <li>title: yes or no (default = no)
	 * <li>name: yes or no (default = no)
	 * </ul>
	 */
	public void doGet( HttpRequest req, HttpResponse res ) throws Exception {

		//Require authentication
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		String ssid = path.element(1);
		if (ssid.startsWith("ss")) {
			if ((req.getParameter("start") == null) || (req.getParameter("end") == null)) {
				//No parameters, just return the page;
				MircConfig mc = MircConfig.getInstance();
				Element lib = mc.getEnabledLocalLibrary(ssid, "enabled");
				String sitename = XmlUtil.getFirstNamedChild(lib, "title").getTextContent();
				String today = StringUtil.getDate("");
				String userIsAdmin = (req.userHasRole("admin") ? "yes" : "no");

				String ui = req.getParameter("ui", "");
				if (!ui.equals("integrated")) ui = "classic";

				String[] params = new String[] {
					"ui",			ui,
					"ssid",			ssid,
					"userIsAdmin",	userIsAdmin,
					"today",		today
				};
				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/summary/AuthorSummary.xsl" ) );
				res.write( XmlUtil.getTransformedText( mc.getXML(), xsl, params) );
				res.setContentType("html");
				res.send();
				return;
			}
			else doQuery(req, res);
		}
		else super.doGet(req, res);
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * <p>
	 * Return content depending on the parameters, which are the same as
	 * the query parameters specified for a GET.
	 */
	public void doPost( HttpRequest req, HttpResponse res ) throws Exception {

		//Require authentication
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		doQuery(req, res);
	}

	private void doQuery(HttpRequest req, HttpResponse res) throws Exception {

		//Get the ssid of the selected library
		Path path = req.getParsedPath();

		//Get the sitename from the Storage Service
		MircConfig mc = MircConfig.getInstance();
		String ssid = req.getParameter("libSelect", "ss1");
		Element lib = mc.getEnabledLocalLibrary(ssid, "enabled");
		ssid = lib.getAttribute("id");
		String libname = XmlUtil.getFirstNamedChild(lib, "title").getTextContent();

		//Get the parameters from the request
		String start	= req.getParameter("start", "");
		String end		= req.getParameter("end", "30000000");
		String format	= req.getParameter("format", "html");
		String title	= req.getParameter("title", "no");
		String name		= req.getParameter("name", "no");
		String date		= req.getParameter("date", "no");
		String access	= req.getParameter("access", "no");
		String user		= req.getParameter("user","").trim();

		String ui = req.getParameter("ui", "");
		if (!ui.equals("integrated")) ui = "classic";

		//If the request does not come from an admin user,
		//only allow the user to see his documents.
		if (!req.userHasRole("admin")) user = req.getUser().getUsername();

		//Get all the documents in the index
		Index index = Index.getInstance(ssid);
		IndexEntry[] mies = index.query("");
		index.sortByPubDate(mies);

		//Now process the entries in accordance with the request
		/*
		  <IndexSummary>
			<StorageService>{$ssname}</StorageService>
			<Context>{$context}</Context>
			<StartDate>{$date1}</StartDate>
			<EndDate>{$date2}</EndDate>
			<IndexedDocs>{$totaldocs}</IndexedDocs>
			<DocsInRange>{$totalmatches}</DocsInRange>
			<UnownedDocs>{count($unowneddocs)}</UnownedDocs>
			{for $x in $ownertable return $x}
			{if ((($user="*") or ($user="")) and count($unowneddocs) > 0) then
			  <Unowned>{for $q in $unownedtable return $q}</Unowned> else ()}
		  </IndexSummary>
		*/

		//Construct the XML document containing the results.
		//Note: this has the same schema as the object which used to be
		//created by the XQuery of the eXist database in T35 and earlier.
		Document doc = XmlUtil.getDocument();
		Element root = doc.createElement("IndexSummary");
		doc.appendChild(root);
		addElement(root, "StorageService", libname);
		addElement(root, "Context", "/storage/"+ssid);
		addElement(root, "StartDate", start);
		addElement(root, "EndDate", end);
		addElement(root, "IndexedDocs", Integer.toString(mies.length));

		//Now narrow down the list to the date range
		IndexEntry[] selected = selectByDate(mies, start, end);
		addElement(root, "DocsInRange", Integer.toString(selected.length));

		//Get the unowned docs
		IndexEntry[] unowned = selectUnowned(mies);
		addElement(root, "UnownedDocs", Integer.toString(unowned.length));

		//Now put in the selected user(s) documents
		String[] owners = getOwners(selected, user);
		for (String owner : owners) {
			addOwnerResult(root, mies, selected, owner);
		}

		//Finally, put in the unowned documents, if appropriate
		if (user.equals("*") || user.equals("")) {
			addDocs(root, unowned);
		}

		//Return it in the requested format
		//XML?
		if (format.equals("xml")) {
			res.setContentType("xml");
			res.setContentDisposition( new File("summary.xml") );
			res.write( XmlUtil.toString(doc) );
			res.send();
			return;
		}

		//Make an array or parameters for the transformations
		Object[] params = {
					"ui",			ui,
					"ssid",			ssid,
					"show-titles",	title,
					"show-names",	name,
					"show-dates",	date,
					"show-access",	access };

		//CSV?
		if (format.equals("csv")) {
			res.setContentType("txt");
			res.setContentDisposition( new File("summary.csv") );
			res.disableCaching();
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/summary/summaryToCSV.xsl" ) );
			res.write(XmlUtil.getTransformedText(doc, xsl, params));
			res.send();
			return;
		}

		//None of the above formats; return HTML
		res.setContentType("html");
		res.disableCaching();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/summary/summaryToHTML.xsl" ) );
		res.write(XmlUtil.getTransformedText(doc, xsl, params));
		res.send();
		return;
	}

	private void addOwnerResult(Element parent, IndexEntry[] mies, IndexEntry[] selected, String owner) {
		Document doc = parent.getOwnerDocument();
		Element child = doc.createElement("Owner");
		parent.appendChild(child);
		addElement(child, "username", owner);
		IndexEntry[] docs = selectByOwner(mies, owner);
		addElement(child, "IndexedDocs", Integer.toString(docs.length));
		docs = selectByOwner(selected, owner);
		addElement(child, "DocsInRange", Integer.toString(docs.length));
		addElement(child, "PublicDocsInRange", Integer.toString(countPublicDocs(docs)));
		addDocs(child, docs);
	}

	private void addDocs(Element parent, IndexEntry[] mies) {
		for (IndexEntry mie : mies) {
			Element docEl = parent.getOwnerDocument().createElement("doc");
			parent.appendChild(docEl);
			addElement(docEl, "title", mie.title);
			addElement(docEl, "path", mie.md.getAttribute("path"));
			addAuthorNames(docEl, mie);
			addElement(docEl, "pubdate", mie.pubdate);
			addElement(docEl, "access", mie.access);
		}
	}

	private void addAuthorNames(Element parent, IndexEntry mie) {
		Document doc = parent.getOwnerDocument();
		NodeList nl = mie.md.getElementsByTagName("name");
		for (int i=0; i<nl.getLength(); i++) {
			Element name = doc.createElement("name");
			name.setTextContent( nl.item(i).getTextContent() );
			parent.appendChild(name);
		}
	}

	private int countPublicDocs(IndexEntry[] mies) {
		int count = 0;
		for (IndexEntry mie : mies) {
			if (mie.isPublic) count++;
		}
		return count;
	}

	private IndexEntry[] selectByOwner(IndexEntry[] mies, String owner) {
		LinkedList<IndexEntry> list = new LinkedList<IndexEntry>();
		for (IndexEntry mie : mies) {
			if (mie.owners.contains(owner)) {
				list.add(mie);
			}
		}
		return list.toArray(new IndexEntry[list.size()]);
	}

	private String[] getOwners(IndexEntry[] mies, String owner) {
		HashSet<String> owners = new HashSet<String>();
		if (owner.equals("*") || owner.equals("")) {
			for (IndexEntry mie : mies) {
				owners.addAll(mie.owners);
			}
			String[] names = owners.toArray(new String[owners.size()]);
			Arrays.sort(names);
			return names;
		}
		else return new String[] { owner };
	}

	private IndexEntry[] selectUnowned(IndexEntry[] mies) {
		LinkedList<IndexEntry> list = new LinkedList<IndexEntry>();
		for (IndexEntry mie : mies) {
			if (mie.owners.size() == 0) {
				list.add(mie);
			}
		}
		return list.toArray(new IndexEntry[list.size()]);
	}

	private IndexEntry[] selectByDate(IndexEntry[] mies, String start, String end) {
		LinkedList<IndexEntry> list = new LinkedList<IndexEntry>();
		for (IndexEntry mie : mies) {
			if ((mie.pubdate.compareTo(start) >= 0) && (mie.pubdate.compareTo(end) <= 0)) {
				list.add(mie);
			}
		}
		return list.toArray(new IndexEntry[list.size()]);
	}

	private void addElement(Element parent, String name, String text) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setTextContent(text);
		parent.appendChild(child);
	}

}


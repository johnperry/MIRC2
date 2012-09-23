/*---------------------------------------------------------------
 *  Copyright 2012 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.activity;

import java.io.Serializable;
import java.util.*;
import org.apache.log4j.Logger;
import mirc.MircConfig;
import mirc.storage.Index;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * Encapsulates all the data relating to activities on a specific library.
 */
public class LibraryActivity implements Serializable {

	public static final long serialVersionUID = 2;
	static final Logger logger = Logger.getLogger(LibraryActivity.class);

	Hashtable<String,Integer> counters;
	HashSet<String> activeUsers;
	Hashtable<DisplayedDocument,Integer> docsDisplayed;
	Hashtable<String,HashSet<DisplayedDocument>> userDisplayActivity;
	String ssid;
	String date;

	String[] services = {
		"aauth",
		"bauth",
		"sub",
		"zip",
		"tce",
		"dcm",
		"myrsna",
		"storage"
	};

	public LibraryActivity(String ssid, String date) {
		this.ssid = ssid;
		this.date = date;
		counters = new Hashtable<String,Integer>();
		docsDisplayed = new Hashtable<DisplayedDocument,Integer>();
		activeUsers = new HashSet<String>();
		userDisplayActivity = new Hashtable<String,HashSet<DisplayedDocument>>();

		//preload the standard trackers
		for (String type : services) counters.put( type, new Integer(0) );
	}

	/**
	 * Add one to the specified counter.
	 */
	public synchronized void increment(String type, String username) {
		Integer counter = counters.get(type);
		if (counter == null) {
			counter = new Integer(0);
		}
		counter = new Integer(counter.intValue() + 1);
		counters.put(type, counter);
		if (username != null) activeUsers.add(username);
	}

	/**
	 * Log access to a document.
	 */
	public synchronized void logDocumentDisplay(String username, String docKey, String title) {
		//Count the document
		DisplayedDocument dd = new DisplayedDocument(docKey, title);
		Integer count = docsDisplayed.get(dd);
		if (count == null) count = new Integer(1);
		else count = new Integer(count.intValue() + 1);
		docsDisplayed.put(dd, count);

		//Update the user, if possible
		if ( (username != null) && !username.equals("")) {
			HashSet<DisplayedDocument> dds = userDisplayActivity.get(username);
			if (dds == null) dds = new HashSet<DisplayedDocument>();
			dds.add(new DisplayedDocument(docKey, title));
			userDisplayActivity.put(username, dds);
		}
	}

	class DisplayedDocument implements Serializable{
		public static final long serialVersionUID = 1;
		public String docKey;
		public String title;
		public DisplayedDocument(String docKey, String title) {
			this.docKey = docKey;
			this.title = title;
		}
		public boolean equals(Object object) {
			if ((object  != null) && (object instanceof DisplayedDocument)) {
				return this.docKey.equals( ((DisplayedDocument)object).docKey );
			}
			return false;
		}
		public int hashCode() {
			return this.docKey.hashCode();
		}
	}

	/**
	 * Get an XML element containing the tracking information for this library.
	 */
	public synchronized Element getXML(Document doc) {
		MircConfig mc = MircConfig.getInstance();
		Element lib = mc.getLocalLibrary(ssid);
		if (lib != null) {
			Element el = doc.createElement("Library");
			el.setAttribute("ssid", ssid);
			Element titleEl = XmlUtil.getFirstNamedChild(lib, "title");
			String title = "";
			if (titleEl != null) title = titleEl.getTextContent();
			el.setAttribute("title", title);
			Index index = Index.getInstance(ssid);
			if (index != null) {
				el.setAttribute("docs", Integer.toString(index.getAllDocuments().length));
			}
			el.setAttribute("docsDisplayed", Integer.toString(docsDisplayed.size()));
			el.setAttribute("activeUsers", Integer.toString(activeUsers.size()));
			for (String type : counters.keySet()) {
				el.setAttribute(type, Integer.toString(counters.get(type)));
			}
			return el;
		}
		return null;
	}

	/**
	 * Get an XML Document containing the list of users who displayed documents from this library.
	 */
	public synchronized Document getUsersDocumentDisplayXML() {
		Document doc = null;
		try {
			doc = XmlUtil.getDocument();
			Element root = doc.createElement("UsersDocumentDisplayList");
			doc.appendChild(root);
			MircConfig mc = MircConfig.getInstance();
			Element libEl = mc.getLocalLibrary(ssid);
			if (libEl != null) {
				Element lib = doc.createElement("Library");
				lib.setAttribute("ssid", ssid);
				lib.setAttribute("date", date);
				Element titleEl = XmlUtil.getFirstNamedChild(libEl, "title");
				String title = "";
				if (titleEl != null) title = titleEl.getTextContent();
				lib.setAttribute("title", title);
				root.appendChild(lib);
				for (String username : userDisplayActivity.keySet()) {
					Element user = doc.createElement("User");
					user.setAttribute("username", username);
					HashSet hs = userDisplayActivity.get(username);
					int n = (hs != null) ? hs.size() : 0;
					user.setAttribute("n", Integer.toString(n));
					lib.appendChild(user);
				}
			}
		}
		catch (Exception unable) { logger.warn("unable to complete the UsersDocumentDisplayList", unable); }
		return doc;
	}

	/**
	 * Get an XML Document containing the list of documents displayed from this library by a specified user.
	 */
	public synchronized Document getUserDocumentDisplayXML(String username) {
		Document doc = null;
		try {
			doc = XmlUtil.getDocument();
			Element root = doc.createElement("UserDocumentDisplayList");
			doc.appendChild(root);
			MircConfig mc = MircConfig.getInstance();
			Element libEl = mc.getLocalLibrary(ssid);
			if (libEl != null) {
				Element lib = doc.createElement("Library");
				lib.setAttribute("ssid", ssid);
				lib.setAttribute("date", date);
				Element titleEl = XmlUtil.getFirstNamedChild(libEl, "title");
				String title = "";
				if (titleEl != null) title = titleEl.getTextContent();
				lib.setAttribute("title", title);
				root.appendChild(lib);
				Element user = doc.createElement("User");
				user.setAttribute("username", username);
				HashSet<DisplayedDocument> hs = userDisplayActivity.get(username);
				for (DisplayedDocument dd : hs) {
					Element docEl = doc.createElement("Document");
					docEl.setAttribute("docKey", dd.docKey);
					docEl.setAttribute("title", dd.title);
					user.appendChild(docEl);
				}
				lib.appendChild(user);
			}
		}
		catch (Exception unable) { logger.warn("unable to complete the UserDocumentDisplayList", unable); }
		return doc;
	}

	/**
	 * Get an XML Document containing the list of all documents displayed documents from this library by all users.
	 */
	public synchronized Document getDocumentsXML() {
		Document doc = null;
		try {
			doc = XmlUtil.getDocument();
			Element root = doc.createElement("DocumentDisplayList");
			doc.appendChild(root);
			MircConfig mc = MircConfig.getInstance();
			Element libEl = mc.getLocalLibrary(ssid);
			if (libEl != null) {
				Element lib = doc.createElement("Library");
				lib.setAttribute("ssid", ssid);
				lib.setAttribute("date", date);
				Element titleEl = XmlUtil.getFirstNamedChild(libEl, "title");
				String title = "";
				if (titleEl != null) title = titleEl.getTextContent();
				lib.setAttribute("title", title);
				root.appendChild(lib);
				for (DisplayedDocument dd : docsDisplayed.keySet()) {
					Element docEl = doc.createElement("Document");
					docEl.setAttribute("docKey", dd.docKey);
					docEl.setAttribute("title", dd.title);
					docEl.setAttribute("n", docsDisplayed.get(dd).toString());
					lib.appendChild(docEl);
				}
			}
		}
		catch (Exception unable) { logger.warn("unable to complete the DocumentDisplayList", unable); }
		return doc;
	}
}

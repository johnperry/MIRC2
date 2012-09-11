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
	HashSet<String> docsDisplayed;
	HashSet<String> activeUsers;
	String ssid;

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

	public LibraryActivity(String ssid) {
		this.ssid = ssid;
		counters = new Hashtable<String,Integer>();
		docsDisplayed = new HashSet<String>();
		activeUsers = new HashSet<String>();

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
	public synchronized void logDocument(String docKey) {
		docsDisplayed.add(docKey);
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
}

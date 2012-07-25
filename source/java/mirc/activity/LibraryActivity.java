/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.activity;

import java.util.*;
import org.apache.log4j.Logger;
import mirc.MircConfig;
import mirc.storage.Index;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * Encapsulates all the data relating to activities on a specific library.
 */
public class LibraryActivity {

	static final Logger logger = Logger.getLogger(LibraryActivity.class);

	static final long oneDay = 24 * 60 * 60 * 1000;

	Hashtable<String,Tracker> trackers;
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
		trackers = new Hashtable<String,Tracker>();

		//preload the standard trackers
		for (String type : services) trackers.put( type, new Tracker(type) );
	}

	/**
	 * Add one to the specified counter for the current day.
	 */
	public synchronized void increment(String type) {
		Tracker tracker = trackers.get(type);
		if (tracker == null) {
			tracker = new Tracker(type);
			trackers.put(type, tracker);
		}
		tracker.increment();
	}

	/**
	 * Get an XML element containing the tracking information for this library.
	 */
	public Element getXML(Document doc, int timeDepth) {
		MircConfig mc = MircConfig.getInstance();
		Element lib = mc.getLocalLibrary(ssid);
		Element el = doc.createElement("Library");
		el.setAttribute("ssid", ssid);
		if (lib != null) {
			Element titleEl = XmlUtil.getFirstNamedChild(lib, "title");
			String title = "";
			if (titleEl != null) title = titleEl.getTextContent();
			el.setAttribute("title", title);
			Index index = Index.getInstance(ssid);
			if (index != null) {
				el.setAttribute("docs", Integer.toString(index.getAllDocuments().length));
			}
			for (String type : trackers.keySet()) {
				el.setAttribute(type, Integer.toString(trackers.get(type).getTotal(timeDepth)));
			}
		}
		return el;
	}
}

/*---------------------------------------------------------------
 *  Copyright 2012 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.activity;

import java.io.Serializable;
import java.util.*;
import mirc.MircConfig;
import org.apache.log4j.Logger;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Encapsulates a database entry for tracking activities on the site for a single month.
 */
public class ActivityDBEntry implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(ActivityDBEntry.class);

	private String date;
	private HashSet<String> activeUsers;
	private Hashtable<String,LibraryActivity> libraries;

	/**
	 * Create an activity database entry for a single month.
	 * @param date the date string (YYYYMM) for this entry, representing the month of the activity report.
	 */
	public ActivityDBEntry(String date) {
		this.date = date;
		this.activeUsers = new HashSet<String>();
		this.libraries = new Hashtable<String,LibraryActivity>();

		//Make sure all the local libraries are in the table
		MircConfig mc = MircConfig.getInstance();
		for (String ssid : mc.getLocalLibraryIDs()) getLibrary(ssid);
	}

	//Get the LibraryActivity object for an ssid, or create
	//one with the current date if one does not exist.
	private LibraryActivity getLibrary(String ssid) {
		LibraryActivity libact = libraries.get(ssid);
		if (libact == null) {
			libact = new LibraryActivity(ssid, date);
			libraries.put(ssid, libact);
		}
		return libact;
	}

	/**
	 * Get the LibraryActivity for a specified library.
	 * @param ssid the ID of the library
	 * @return the indexed LibraryActivity object, or null if no object exists for the ssid
	 */
	public synchronized LibraryActivity getLibraryActivity(String ssid) {
		return libraries.get(ssid);
	}

	/**
	 * Update all the libraries.
	 */
	public void update() {
		for (LibraryActivity libact : libraries.values()) {
			libact.update();
		}
	}

	/**
	 * Get the date string (YYYYMM) for this entry, representing the month of the activity report.
	 */
	public synchronized String getDate() {
		return date;
	}

	/**
	 * Increment a counter and capture a username.
	 * @param ssid the ID of the library
	 * @param type the type of activity
	 * @param username the active user, or null if the user was not authenticated.
	 */
	public synchronized void increment(String ssid, String type, String username) {
		if ((username != null) && !username.trim().equals("")) activeUsers.add(username);
		LibraryActivity libact = getLibrary(ssid);
		libact.increment(type, username);
		libraries.put(ssid, libact);
	}

	/**
	 * Log the display of a document.
	 * @param ssid the ID of the library
	 * @param username the username of the user who displayed the document.
	 * @param docpath the path to the document that was displayed.
	 * @param title the title of the document that was displayed.
	 */
	public synchronized void logDocumentDisplay(String ssid, String username, String docpath, String title) {
		LibraryActivity libact = getLibrary(ssid);
		libact.logDocumentDisplay(username, docpath, title);
		libraries.put(ssid, libact);
	}

	public synchronized Element getXML(Document doc) {
		try {
			Element reportEl = doc.createElement("MonthlyReport");
			reportEl.setAttribute("date", date);
			reportEl.setAttribute("activeUsers", Integer.toString(activeUsers.size()));
			String[] keys = new String[libraries.size()];
			keys = libraries.keySet().toArray(keys);
			Arrays.sort(keys);
			for (String ssid : keys) {
				LibraryActivity libact = libraries.get(ssid);
				if (libact != null) {
					Element libEl = libact.getXML(doc);
					if (libEl != null) reportEl.appendChild(libEl);
				}
			}
			return reportEl;
		}
		catch (Exception ex) {
			return null;
		}
	}
}

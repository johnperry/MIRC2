/*---------------------------------------------------------------
 *  Copyright 2012 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.activity;

import java.io.File;
import java.util.*;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import jdbm.RecordManager;
import mirc.MircConfig;
import org.apache.log4j.Logger;
import org.rsna.server.Users;
import org.rsna.server.UsersXmlFileImpl;
import org.rsna.util.JdbmUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Encapsulates a database to track activities an all the libraries.
 */
public class ActivityDB {

	static final Logger logger = Logger.getLogger(ActivityDB.class);

	private static ActivityDB activityDB = null;

	private static File dir = null;
	private static RecordManager recman = null;
	private static final String databaseName = "activity";
	private static final String activityName = "activity";
	private static final String summariesName = "summaries";
	private static final String lastReportTimeName = "lastReportTime";
	private static HTree activity = null;
	private static HTree lastReportTime = null;
	private static HTree summaries = null;

	/**
	 * Protected constructor.
	 * @param dir the directory in which the database is located.
	 */
	protected ActivityDB(File dir) {
		this.dir = dir;
		File databaseFile = new File(dir, databaseName);
		recman = JdbmUtil.getRecordManager(databaseFile.getAbsolutePath());

		//If the database is old, then it doesn't contain the lastReportTime
		//HTree, in which case, we will delete the old activity table.
		if (!JdbmUtil.containsNamedObject(recman, lastReportTimeName)) {
			try { recman.commit(); }
			catch (Exception ignore) { }
			JdbmUtil.deleteNamedObject(recman, activityName);
		}

		//Now get or create the database tables
		activity = JdbmUtil.getHTree(recman, activityName);
		summaries = JdbmUtil.getHTree(recman, summariesName);
		lastReportTime = JdbmUtil.getHTree(recman, lastReportTimeName);
		try { recman.commit(); }
		catch (Exception ignore) { }
	}

	/**
	 * Load the singleton instance of the database.
	 * @param dir the directory in which the database is located.
	 */
	public static ActivityDB load(File dir) {
		activityDB = new ActivityDB(dir);
		return activityDB;
	}

	/**
	 * Get the singleton instance of the activity database.
	 * This method is intended for normal classes.
	 */
	public static synchronized ActivityDB getInstance() {
		return activityDB;
	}

	//Get the key for today's date
	private String thisMonth() {
		GregorianCalendar cal = new GregorianCalendar();
		return String.format( "%04d%02d", cal.get(cal.YEAR), (cal.get(cal.MONTH)+1) );
	}

	/**
	 * Get the last report time (the last time the activity report was
	 * transmitted to the RSNA TFS site).
	 */
	public synchronized long getLastReportTime() {
		try {
			Long last = (Long)lastReportTime.get("lastReportTime");
			if (last != null) return last.longValue();
		}
		catch (Exception ex) { }
		return 0;
	}

	/**
	 * Set the the last report time (the last time the activity report was
	 * transmitted to the RSNA TFS site).
	 * @param time the time (in milliseconds) to be stored.
	 */
	public synchronized void setLastReportTime(long time) {
		try {
			lastReportTime.put("lastReportTime", new Long(time));
			recman.commit();
		}
		catch (Exception ignore) { }
	}

	/**
	 * Get the database entry for the current date, creating it if necessary.
	 */
	public synchronized ActivityDBEntry get() {
		String thisMonth = thisMonth();
		ActivityDBEntry entry = null;
		try {
			entry = (ActivityDBEntry)activity.get(thisMonth);
			if (entry == null) {
				entry = new ActivityDBEntry(thisMonth);
				activity.put(thisMonth, entry);
			}
		}
		catch (Exception ignore) { }
		return entry;
	}

	/**
	 * Put an entry in the database.
	 */
	public synchronized void put(ActivityDBEntry entry) {
		try {
			activity.put(entry.getDate(), entry);
			recman.commit();
		}
		catch (Exception ignore) { }
	}

	/**
	 * Put a summary report in the database.
	 */
	public synchronized void put(SummariesDBEntry entry) {
		try {
			summaries.put(entry.getSiteID(), entry);
			recman.commit();
		}
		catch (Exception ignore) { }
	}

	/**
	 * Increment a field in a specified library.
	 * @param ssid the ID of the library
	 * @param type the field identifier whose counter is to be incremented.
	 * @param username the username of the user performing the activity, or null if unknown.
	 */
	public synchronized void increment(String ssid, String type, String username) {
		try {
			ActivityDBEntry entry = get();
			entry.increment(ssid, type, username);
			put(entry);
		}
		catch (Exception ignore) { }
	}

	/**
	 * Log access to a document.
	 * @param ssid the ID of the library
	 * @param docpath the path to the document that was accessed.
	 */
	public synchronized void logDocument(String ssid, String docpath) {
		try {
			ActivityDBEntry entry = get();
			entry.logDocument(ssid, docpath);
			put(entry);
		}
		catch (Exception ignore) { }
	}

	/**
	 * Get an XML Document containing the contents of the activities database.
	 */
	public synchronized Document getXML() {
		MircConfig mc = MircConfig.getInstance();
		Document doc = null;
		try {
			doc = XmlUtil.getDocument();
			Element root = doc.createElement("ActivityReport");
			doc.appendChild(root);

			root.setAttribute("id", mc.getSiteID());
			root.setAttribute("name", mc.getSiteName());
			root.setAttribute("url", mc.getLocalAddress());
			root.setAttribute("version", mc.getVersion());
			root.setAttribute("email", mc.getAdminEmail());

			Users users = Users.getInstance();
			if (users instanceof UsersXmlFileImpl) {
				root.setAttribute("users", Integer.toString(((UsersXmlFileImpl)users).getNumberOfUsers()));
			}

			String key;
			FastIterator fit = activity.keys();
			HashSet<String> set = new HashSet<String>();
			while ( (key=(String)fit.next()) != null) set.add(key);
			String[] keys = new String[set.size()];
			keys = set.toArray(keys);
			Arrays.sort(keys);
			for (int k=keys.length - 1; k>=0; k--) {
				try {
					ActivityDBEntry entry = (ActivityDBEntry)activity.get(keys[k]);
					if (entry != null) {
						Element el = entry.getXML(doc);
						if (el != null) root.appendChild(el);
					}
				}
				catch (Exception skip) { }
			}
		}
		catch (Exception ignore) { }
		return doc;
	}

	/**
	 * Get an XML Document containing the contents of the summaries database.
	 */
	public synchronized Document getSummariesXML() {
		Document doc = null;
		try {
			doc = XmlUtil.getDocument();
			Element root = doc.createElement("SummaryReport");
			doc.appendChild(root);

			String key;
			FastIterator fit = summaries.keys();
			while ( (key=(String)fit.next()) != null) {

				try {
					SummariesDBEntry entry = (SummariesDBEntry)summaries.get(key);
					if (entry != null) {
						Element el = entry.getXML(doc);
						if (el != null) root.appendChild(el);
					}
				}
				catch (Exception skip) { }
			}
		}

		catch (Exception ignore) { }
		return doc;
	}

	/**
	 * Commit changes and close the database.
	 * No errors are reported and no operations
	 * are available after this call.
	 */
	public static synchronized void close() {
		JdbmUtil.close(recman);
		recman = null;
		activity = null;
		summaries = null;
		lastReportTime = null;
	}

}

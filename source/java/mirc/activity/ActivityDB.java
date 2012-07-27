/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.activity;

import java.io.File;
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
	private static final int timeDepth = 30; //days

	private static File dir = null;
	private static RecordManager recman = null;
	private static final String databaseName = "activity";
	private static final String hTreeName = "libraries";
	private static HTree libraries = null;

	/**
	 * Protected constructor.
	 * @param dir the directory in which the database is located.
	 */
	protected ActivityDB(File dir) {
		this.dir = dir;
		File databaseFile = new File(dir, databaseName);
		recman = JdbmUtil.getRecordManager(databaseFile.getAbsolutePath());
		libraries = JdbmUtil.getHTree(recman, hTreeName);

		//Make sure all the local libraries are in the DB
		MircConfig mc = MircConfig.getInstance();
		for (String ssid : mc.getLocalLibraryIDs()) {
			try {
				LibraryActivity libact = (LibraryActivity)libraries.get(ssid);
				if (libact == null) {
					libact = new LibraryActivity(ssid);
					libraries.put(ssid, libact);
				}
			}
			catch (Exception ignore) { }
		}
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

	/**
	 * Increment a field in a specified library.
	 * @param ssid the ID of the library
	 * @param type the field identifier whose counter is to be incremented.
	 */
	public static synchronized void increment(String ssid, String type) {
		try {
			LibraryActivity libact = (LibraryActivity)libraries.get(ssid);
			if (libact == null) {
				libact = new LibraryActivity(ssid);
			}
			libact.increment(type);
			libraries.put(ssid, libact);
		}
		catch (Exception ignore) { }
	}

	/**
	 * Get an XML Document containing the contents of the database.
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

			Users users = Users.getInstance();
			if (users instanceof UsersXmlFileImpl) {
				root.setAttribute("users", Integer.toString(((UsersXmlFileImpl)users).getNumberOfUsers()));
			}

			//Add in the local libraries from the configuration
			for (String ssid : mc.getLocalLibraryIDs()) {
				try {
					LibraryActivity libact = (LibraryActivity)libraries.get(ssid);
					if (libact != null) {
						Element el = libact.getXML(doc, timeDepth);
						if (el != null) root.appendChild(el);
					}
				}
				catch (Exception ignore) { }
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
		libraries = null;
	}

}

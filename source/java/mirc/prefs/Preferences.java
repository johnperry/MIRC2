/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.prefs;

import java.io.File;
import java.util.Hashtable;
import java.util.Set;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import jdbm.RecordManager;
import mirc.MircConfig;
import mirc.util.MyRsnaUser;
import org.apache.log4j.Logger;
import org.rsna.util.JdbmUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A class to provide singleton access to a database of preferences.
 */
public class Preferences {

	static final Logger logger = Logger.getLogger(Preferences.class);

	private static Preferences preferences = null;

	private static File dir = null;
	private static RecordManager recman = null;
	private static final String databaseName = "preferences";
	private static final String prefsName = "prefs";
	private static HTree prefs = null;

	/**
	 * Protected constructor.
	 * @param dir the directory in which the database is located.
	 */
	protected Preferences(File dir) {
		this.dir = dir;
		File databaseFile = new File(dir, databaseName);
		recman = JdbmUtil.getRecordManager(databaseFile.getAbsolutePath());
		prefs = JdbmUtil.getHTree(recman, prefsName);
	}

	/**
	 * Load the singleton instance of the preferences database.
	 * This method is intended to be called by the init method
	 * of the PreferencesServlet for the initial loading of the database
	 * when MIRC starts.
	 * @param dir the directory in which the database is located.
	 */
	public static synchronized Preferences load(File dir) {
		preferences = new Preferences(dir);
		return preferences;
	}

	/**
	 * Get the singleton instance of the preferences database.
	 * This method is intended for normal classes.
	 */
	public static synchronized Preferences getInstance() {
		return preferences;
	}

	/**
	 * Check whether an entry exists for a specific user.
	 * @param username the username of the user.
	 * @return true if a user exists in the database; false otherwise.
	 */
	public synchronized boolean hasUser(String username) {
		try { return (prefs.get(username) != null); }
		catch (Exception ex) { return false; }
	}


	/**
	 * Get the preferences Element for a specific user or for all users.
	 * @param username the username of the user, or "*" to obtain an
	 * XML Document containing the preferences for all users.
	 * @param suppress true if only name, affiliation, and contact is
	 * to be returned.
	 * @return the preferences for the specified user.
	 */
	public synchronized Element get(String username, boolean suppress) {

		//Note: in the following, we always import Elements from the
		//HTree into a new Document in order to make it impossible for
		//the calling code to modify the object in the JDBM cache.

		try {
			Node node;
			Document doc = XmlUtil.getDocument();
			if (username.contains("*")) {
				Element root = doc.createElement("Preferences");
				doc.appendChild(root);
				FastIterator fit = prefs.values();
				Element user;
				while ( (user=(Element)fit.next()) != null) {
					Element el = (Element)root.appendChild( doc.importNode( user, true ) );
					if (suppress) {
						while ( (node=el.getFirstChild()) != null ) el.removeChild(node);
					}
				}
				return root;
			}
			else {
				Element user = (Element)prefs.get(username);
				if (user != null) {
					user = (Element)doc.appendChild( doc.importNode( user, true ) );
					if (suppress) {
						while ( (node=user.getFirstChild()) != null ) user.removeChild(node);
					}
				}
				else {
					user = doc.createElement("User");
					user.setAttribute("username", username);
					doc.appendChild(user);
				}
				return user;
			}
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Get the user's Query Service user interface preference.
	 * @return the the user's Query Service user interface preference,
	 * or the system default if no preference has been stored.
	 */
	public synchronized String getUI(String username) {
		try {
			Element user = (Element)prefs.get(username);
			if (user != null) {
				String ui = user.getAttribute("UI");
				if (!ui.equals("")) return ui;
			}
		}
		catch (Exception ex) { }
		//We couldn't find a UI for this user
		//return the system default.
		return MircConfig.getInstance().getUI();
	}

	/**
	 * Get the user's myRSNA account Element.
	 * @return the the user's myRSNA account Element or null if no
	 * element exists or if either the username or password is blank.
	 */
	public synchronized Element getMyRsnaAccount(String username) {
		try {
			Element user = (Element)prefs.get(username);
			if (user == null) return null;
			Element myrsna = XmlUtil.getFirstNamedChild(user, "myrsna");
			if (myrsna == null) return null;
			Document doc = XmlUtil.getDocument();
			myrsna = (Element)doc.importNode(myrsna, true);
			String un = myrsna.getAttribute("username").trim();
			String pw = myrsna.getAttribute("password").trim();
			boolean ok = (!un.equals("") && !pw.equals(""));
			return (ok ? myrsna : null);
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Get the user's MyRsnaUser, or null if one does not exist.
	 * @return the the user's MyRsnaUser, or null if one does not exist
	 * or if either the MyRSNA username or password is blank.
	 */
	public synchronized MyRsnaUser getMyRsnaUser(String username) {
		try {
			Element user = (Element)prefs.get(username);
			if (user == null) return null;
			Element myrsna = XmlUtil.getFirstNamedChild(user, "myrsna");
			if (myrsna == null) return null;
			String un = myrsna.getAttribute("username").trim();
			String pw = myrsna.getAttribute("password").trim();
			boolean ok = (!un.equals("") && !pw.equals(""));
			return (ok ? new MyRsnaUser(un, pw) : null);
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Get the user's myRSNA account Element.
	 * @return true if the update succeeded; false otherwise.
	 */
	public synchronized boolean setUI(String username, String ui) {
		try {
			if (prefs != null) {
				Element user = (Element)prefs.get(username);
				if (user == null) user = getNewUser(username);
				user.setAttribute("UI", ui.trim());
				prefs.put(username, user);
				recman.commit();
				return true;
			}
		}
		catch (Exception ex) { }
		return false;
	}

	/**
	 * Update the author information for a user, creating
	 * a new preferences Element if one does not already exist.
	 * @param username the username of the preference to update.
	 * @param name the author's name.
	 * @param affiliation the author's affiliation.
	 * @param contact the author's contact information.
	 * @return true if the update succeeded; false otherwise.
	 */
	public synchronized boolean setAuthorInfo(String username, String name, String affiliation, String contact) {
		try {
			if (prefs != null) {
				Element user = (Element)prefs.get(username);
				if (user == null) user = getNewUser(username);
				user.setAttribute("name", name);
				user.setAttribute("affiliation", affiliation);
				user.setAttribute("contact", contact);
				prefs.put(username, user);
				recman.commit();
				return true;
			}
		}
		catch (Exception ex) { }
		return false;
	}

	/**
	 * Update the myRSNA account information for a user, creating
	 * a new preferences Element if one does not already exist.
	 * @param username the username of the preference to update.
	 * @param enabled "yes" or "no", determining whether the File Service is
	 * to connect to the user's myRSNA account and display the contents stored there.
	 * @param myrsnaUsername the user's username on the myRSNA site.
	 * @param myrsnaPassword the user's password on the myRSNA site.
	 * @return true if the update succeeded; false otherwise.
	 */
	public synchronized boolean setMyRsnaInfo(String username, String enabled, String myrsnaUsername, String myrsnaPassword) {
		try {
			if (prefs != null) {
				Element user = (Element)prefs.get(username);
				if (user == null) user = getNewUser(username);
				Element myrsna = XmlUtil.getFirstNamedChild(user, "myrsna");
				if (myrsna == null) {
					myrsna = user.getOwnerDocument().createElement("myrsna");
					user.appendChild(myrsna);
				}
				myrsna.setAttribute("enabled", enabled);
				myrsna.setAttribute("username", myrsnaUsername);
				myrsna.setAttribute("password", myrsnaPassword);
				prefs.put(username, user);
				recman.commit();
				return true;
			}
		}
		catch (Exception ex) { }
		return false;
	}

	/**
	 * Update the export destination information for a user, creating
	 * a new preferences Element if one does not already exist.
	 * @param username the username of the preference to update.
	 * @param sites the user's export destinations.
	 * @return true if the update succeeded; false otherwise.
	 */
	public synchronized boolean setExportInfo(String username, ExportSite[] sites) {
		try {
			if (prefs != null) {
				Element user = (Element)prefs.get(username);
				if (user == null) user = getNewUser(username);
				Element export = XmlUtil.getFirstNamedChild(user, "export");
				if (export != null) export.getParentNode().removeChild(export);
				Document doc = user.getOwnerDocument();
				export = doc.createElement("export");
				user.appendChild(export);
				for (ExportSite site : sites) {
					export.appendChild( site.getSiteElement(doc) );
				}
				prefs.put(username, user);
				recman.commit();
				return true;
			}
		}
		catch (Exception ex) { }
		return false;
	}

	//Create a new user element and set the username attribute
	private Element getNewUser(String username) {
		try {
			Document doc = XmlUtil.getDocument();
			Element user = doc.createElement("User");
			doc.appendChild(user);
			user.setAttribute("username", username);
			return user;
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Commit changes and close the index.
	 * No errors are reported and no operations
	 * are available after this call.
	 */
	public static synchronized void close() {
		JdbmUtil.close(recman);
		recman = null;
		prefs = null;
	}
}
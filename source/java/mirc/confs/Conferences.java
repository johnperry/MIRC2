/*---------------------------------------------------------------
*  Copyright 2009 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.confs;

import java.io.File;
import java.util.Properties;
import org.apache.log4j.Logger;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.htree.HTree;

/**
 * A class to provide singleton access to a database of conferences.
 */
public class Conferences {

	static final Logger logger = Logger.getLogger(Conferences.class);

	private static Conferences conferences = null;

	private static File dir = null;
	private static RecordManager recman = null;
	private static final String databaseName = "conferences";
	private static final String rootsName = "roots";
	private static final String confsName = "confs";
	private static final String cfidsName = "ID";
	private static HTree roots = null;
	private static HTree confs = null;
	private static HTree cfids = null;

	/**
	 * Protected constructor.
	 * @param dir the directory in which the database is located.
	 */
	protected Conferences(File dir) {
		this.dir = dir;

		//Load the database
		loadDatabase(dir);
	}

	/**
	 * Load the singleton instance of the conferences database.
	 * This method is intended to be called by the init method
	 * of the file service for the initial loading of the database
	 * when MIRC starts.
	 * @param dir the directory in which the database is located.
	 */
	public static void load(File dir) {
		conferences = new Conferences(dir);
	}

	/**
	 * Get the singleton instance of the conferences database.
	 * This method is intended for normal classes.
	 */
	public static Conferences getInstance() {
		return conferences;
	}

	/**
	 * Get the root Conference object for a specific user. If the root
	 * conference is not available, one is created.
	 * @param mircUsername the username of the user on the MIRC site,
	 * or null if the shared root Conference object is desired.
	 * @return the Conferences object or null if unable.
	 */
	public synchronized Conference getRootConference(String mircUsername) {
		if ((roots != null) && (confs != null)) {
			String rootname = (mircUsername == null) ? "Shared" : "Personal/"+mircUsername;
			try {
				String id = (String)roots.get(rootname);
				if (id != null) {
					return (Conference)confs.get(id);
				}
				else {
					id = getNewID();
					String title = (mircUsername == null) ? "Shared" : "Personal";
					Conference root = new Conference(mircUsername, id, null, title);;
					confs.put(id, root);
					roots.put(rootname, id);
					recman.commit();
					return root;
				}
			}
			catch (Exception ex) { }
		}
		return null;
	}

	/**
	 * Get the Conference object with the specified ID.
	 * @param id the id of the Conference to fetch.
	 * @return the Conference object or null if unable.
	 */
	public synchronized Conference getConference(String id) {
		if (confs != null) {
			try { return (Conference)confs.get(id); }
			catch (Exception ex) { }
		}
		return null;
	}

	/**
	 * Update a Conference object. This method only updates
	 * the database if a conference already exists with the
	 * same id and the owner of the existing conference
	 * and the owner of the new version are the same.
	 * @param newVersion the conference to update.
	 * @return true if the update succeeded; false otherwisse.
	 */
	public synchronized boolean setConference(Conference newVersion) {
		if (confs != null) {
			try {
				Conference oldVersion = (Conference)confs.get(newVersion.id);
				if (oldVersion == null) return false;
				if ((oldVersion.owner == null) && (newVersion.owner != null)) return false;
				if ((oldVersion.owner != null) && (newVersion.owner == null)) return false;
				if ((oldVersion.owner != null) && (newVersion.owner != null)
								&& !oldVersion.owner.equals(newVersion.owner)) return false;
				confs.put(newVersion.id, newVersion);
				recman.commit();
				return true;
			}
			catch (Exception ex) { }
		}
		return false;
	}

	/**
	 * Create a new conference in the database. This method attempts to
	 * create a child conference for the specified user.
	 * <ol>
	 * <li>If the mircUsername is null, a shared conference is created.
	 * <li>If the mircUsername is not null, a personal conference is created.
	 * <li>If the parentID is null or empty, or if there is no conference with the
	 * specified parentID, or if the parent conference is not owned by the user, no
	 * new conference is created.
	 * </ol>
	 * @param mircUsername the username of the user on the MIRC site.
	 * or null if a shared Conference object is to be created.
	 * @param title the title of the conference.
	 * @param parentID the id of the conference which will be the parent
	 * of the created conference.
	 * @return the new conference, or null if the conference could not be created.
	 */
	public synchronized Conference createConference(
							String mircUsername,
							String title,
							String parentID) {
		if ((roots == null) || (confs == null)) return null;
		if ((parentID == null) || parentID.equals("")) return null;
		try {
			Conference parent = (Conference)confs.get(parentID);
			if (parent == null) return null;
			if (parent.owner == null) {
				 //We are creating a shared conference; set the owner to null.
				 mircUsername = null;
			 }
			else {
				//Not shared; only allow creation if the user owns the parent.
				if (!parent.owner.equals(mircUsername)) return null;
			}
			//OK, it looks like it is safe to create this conference
			String id = getNewID();
			Conference newConference = new Conference(mircUsername, id, parentID, title);
			confs.put(id, newConference);
			parent.appendChildConference(newConference);
			confs.put(parent.id, parent);
			recman.commit();
			return newConference;
		}
		catch (Exception ex) { }
		return null;
	}

	/**
	 * Delete an existing conference from the database and remove it from
	 * the list of children of its parent. This method also has the effect
	 * of removing all the child conferences of the conference being deleted.
	 * This method will not delete a root conference.
	 * @param id the id of the conference to delete.
	 * @return true if the deletion succeed; false otherwise.
	 */
	public synchronized boolean deleteConference(String id) {
		if ((roots == null) || (confs == null)) return false;
		try {
			Conference conf = (Conference)confs.get(id);
			if (conf == null) return false;
			//if (conf.agenda.size() != 0) return false;
			//if (conf.children.size() != 0) return false;

			 //Don't delete a root conference
			if (conf.pid == null) return false;
			Conference parent = (Conference)confs.get(conf.pid);
			if (parent == null) return false;

			//OK, it looks like we can delete the conference.
			removeAll(conf);
			parent.removeChildConference(id);
			confs.put(parent.id, parent);
			recman.commit();
			return true;
		}
		catch (Exception ex) {
			logger.warn("unable to delete conference; id="+id, ex);
			return false;
		}
	}

	//Remove a conference and all its children.
	//This method just keeps the database clean.
	private void removeAll(Conference conf) {
		try {
			for (String id : conf.children) {
				Conference c = (Conference)confs.get(id);
				removeAll(c);
			}
			confs.remove(conf.id);
		}
		catch (Exception skip) { logger.warn("Unable to remove conference "+conf.id); }
	}

	//Get the next available ID for use in identifying a conference.
	//An ID is an integer represented as a string.
	private synchronized String getNewID() {
		try {
			Integer idInteger = (Integer)cfids.get("ID");
			if (idInteger == null) idInteger = new Integer(0);
			int id = idInteger.intValue() + 1;
			cfids.put("ID", new Integer(id));
			recman.commit();
			return Integer.toString(id);
		}
		catch (Exception ex) {
			logger.warn("Unable to create id for conference.");
		}
		return null;
	}

	/**
	 * Load the database from the JDBM files,
	 * creating the JDBM files if necessary.
	 */
	private static synchronized void loadDatabase(File dir) {
		if (recman == null) {
			try {
				File databaseFile = new File(dir, databaseName);
				recman = getRecordManager(databaseFile.getAbsolutePath());
				roots = getHTree(recman, rootsName);
				confs = getHTree(recman, confsName);
				cfids = getHTree(recman, cfidsName);
			}
			catch (Exception ex) {
				logger.warn("Unable to instantiate the Conferences database.");
				roots = null;
				confs = null;
				cfids = null;
			}
		}
	}

	/**
	 * Commit changes and close the index.
	 * No errors are reported and no operations
	 * are available after this call without calling
	 * loadIndex.
	 */
	public static synchronized void close() {
		if (recman != null) {
			try {
				recman.commit();
				recman.close();
				recman = null;
				roots = null;
				confs = null;
				cfids = null;
			}
			catch (Exception ignore) { }
		}
	}

	//Get a RecordManager
	private static RecordManager getRecordManager(String filename) throws Exception {
		Properties props = new Properties();
		props.put( RecordManagerOptions.THREAD_SAFE, "true" );
		return RecordManagerFactory.createRecordManager( filename, props );
	}

	//Get a named HTree, or create it if it doesn't exist.
	private static HTree getHTree(RecordManager recman, String name) throws Exception {
		HTree index = null;
		long recid = recman.getNamedObject(name);
		if ( recid != 0 )
			index = HTree.load( recman, recid );
		else {
			index = HTree.createInstance( recman );
			recman.setNamedObject( name, index.getRecid() );
			recman.commit();
		}
		return index;
	}

}
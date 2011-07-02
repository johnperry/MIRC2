/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.File;
import java.util.Properties;
import org.apache.log4j.Logger;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.htree.HTree;

/**
 * A class to encapsulate a database of MyRsnaUsers.
 */
public class MyRsnaUsers {

	static final Logger logger = Logger.getLogger(MyRsnaUsers.class);

	private static MyRsnaUsers myRsnaUsers = null;

	private static RecordManager recman = null;
	private static final String indexName = "myrsna-users";
	private static final String indexTreeName = "myrsna";
	private static HTree index = null;

	/**
	 * Protected constructor.
	 * @param file any file under the webapps directory.
	 */
	protected MyRsnaUsers(File file) {
		//Get a File object that is guaranteed to have an absolute
		//path so we can walk it backwards.
		File dir = new File(file.getAbsolutePath());

		//Find the conf directory
		while (!dir.isDirectory() || !dir.getName().equals("webapps"))
			dir = dir.getParentFile();
		dir = new File(dir.getParentFile(),"conf");

		//Load the index
		loadIndex(dir);
	}

	/**
	 * Load the singleton instance of the MyRsnaUsers database.
	 * This method is intended to be called by the init method
	 * of the query service for the initial loading of the database
	 * when MIRC starts.
	 * @param file any file under the webapps directory.
	 */
	public static void loadMyRsnaUsers(File file) {
		myRsnaUsers = new MyRsnaUsers(file);
	}

	/**
	 * Get the singleton instance of the MyRsnaUsers database.
	 * This method is intended for normal classes.
	 */
	public static MyRsnaUsers getInstance() {
		return myRsnaUsers;
	}

	/**
	 * Get a specific MyRsnaUser user.
	 * @param mircUsername the username of the user on the MIRC site.
	 * @return the MyRsnaUser or null if unable.
	 */
	public synchronized MyRsnaUser getMyRsnaUser(String mircUsername) {
		if (index != null) {
			try { return (MyRsnaUser)index.get(mircUsername); }
			catch (Exception ex) { }
		}
		return null;
	}

	/**
	 * Add a MyRsnaUser to the database or update the user if it exists.
	 */
	public synchronized void addMyRsnaUser(String mircUsername, MyRsnaUser myRsnaUser) {
		if ((index != null) && (mircUsername != null) && (myRsnaUser != null)) {
			try {
				index.put(mircUsername, myRsnaUser);
				recman.commit();
			}
			catch (Exception ex) { }
		}
	}

	/**
	 * Load the myrsna-users index from the JDBM files,
	 * creating the JDBM files if necessary.
	 */
	private static synchronized void loadIndex(File dir) {
		if (recman == null) {
			try {
				File indexFile = new File(dir, indexName);
				recman = getRecordManager(indexFile.getAbsolutePath());
				index = getHTree(recman, indexTreeName);
			}
			catch (Exception ex) {
				logger.warn("Unable to instantiate the MyRsnaUsers database.");
				index = null;
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
				index = null;
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
/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.ssadmin;

import java.util.Set;

import mirc.MircConfig;
import mirc.storage.Index;
import org.apache.log4j.Logger;

/**
 * The singleton Storage Service index rebuilder.
 */
public class StorageServiceRebuilder extends Thread {

	static final Logger logger = Logger.getLogger(StorageServiceRebuilder.class);

	private static StorageServiceRebuilder rebuilder = null;
	private static boolean isBusy = false;

	/**
	 * Construct a StorageServiceRebuilder.
	 */
	protected StorageServiceRebuilder() {
		super();
		isBusy = false;
	}

	/**
	 * Get the singleton instance of the StorageServiceRebuilder.
	 */
	public static StorageServiceRebuilder getInstance() {
		if ((rebuilder == null) || !rebuilder.isBusy()) {
			rebuilder = new StorageServiceRebuilder();
			return rebuilder;
		}
		return null;
	}

	/**
	 * See if the StorageServiceRebuilder is busy.
	 */
	public static boolean isBusy() {
		return isBusy;
	}

	/**
	 * Start the StorageServiceRebuilder.
	 */
	public void run() {
		isBusy = true;

		//Rebuild all the indexes.
		MircConfig mc = MircConfig.getInstance();
		Set<String> ids = mc.getLocalLibraryIDs();
		for (String id : ids) {
			logger.info("Rebuilding "+id);
			Index index = Index.getInstance(id);
			index.rebuild();
		}
		logger.info("Rebuild complete");

		isBusy = false;
	}
}

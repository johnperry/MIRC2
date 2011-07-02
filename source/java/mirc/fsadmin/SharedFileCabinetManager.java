/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.fsadmin;

import java.io.*;
import mirc.MircConfig;
import org.w3c.dom.Element;

/**
 * The Thread that automatically removes files from
 * the shared file cabinet after they time out.
 */
public class SharedFileCabinetManager extends Thread {

	File root;
	File filesDir;
	File iconsDir;

	static long anHour = 60 * 60 * 1000;

	/**
	 * Create a new SharedFileCabinetManager to remove files
	 * from the shared file cabinet after they time out.
	 */
	public SharedFileCabinetManager(File root) {
		this.root = root;
		File shared = new File(root, "Shared");
		this.filesDir = new File(shared, "Files");
		this.iconsDir = new File(shared, "Icons");
	}

	/**
	 * Start the thread. Check for timed out files every hour.
	 */
	public void run() {
		try {
			while (true) {
				checkFiles();
				sleep(anHour);
			}
		}
		catch (Exception ex) { }
	}

	//Remove timed out files.
	private void checkFiles() {

		//Get the maximum age allowed.
		MircConfig mc = MircConfig.getInstance();
		Element fileService = mc.getFileService();
		String timeout = fileService.getAttribute("timeout");
		long maxAge;
		try { maxAge = Long.parseLong(timeout) * anHour; }
		catch (Exception ex) { maxAge = 0; }

		if (maxAge <= 0) return; //0 means keep forever

		//Non-zero; remove the older ones
		long timeNow = System.currentTimeMillis();
		long earliestAllowed = timeNow - maxAge;

		removeOldFiles(filesDir, earliestAllowed);

		//Keep the icons a little longer to make sure
		//we don't delete the icons for images still
		//in the Files tree.
		removeOldFiles(iconsDir, earliestAllowed - anHour);
	}

	private void removeOldFiles(File dir, long minLM) {
		if (!dir.exists() || !dir.isDirectory()) return;
		File[] files = dir.listFiles();
		for (int i=0; i<files.length; i++) {
			if (files[i].isDirectory()) {
				removeOldFiles(files[i], minLM);
			}
			else {
				long lm = files[i].lastModified();
				if (lm < minLM) files[i].delete();
			}
		}
		if (!dir.equals(filesDir) && !dir.equals(iconsDir)) {
			files = dir.listFiles();
			if (files.length == 0) dir.delete();
		}
	}
}

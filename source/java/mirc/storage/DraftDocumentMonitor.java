/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.io.*;
import java.net.*;

import mirc.MircConfig;
import mirc.ssadmin.StorageServiceAdmin;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A Thread to monitor local libraries and delete draft documents
 * older than a specific age.
 */
public class DraftDocumentMonitor extends Thread {

	static final Logger logger = Logger.getLogger(DraftDocumentMonitor.class);

	String ssid;
	int timeDepth;
	static final long anHour = 60 * 60 * 1000;
	static final long aDay = 24 * anHour;

	/**
	 * Create a new DraftDocumentMonitor to remove draft
	 * documents after they time out.
	 */
	public DraftDocumentMonitor(String ssid, int timeDepth) {
		super("DraftDocumentMonitor("+ssid+")");
		this.ssid = ssid;
		this.timeDepth = timeDepth;
	}

	/**
	 * Start the thread. Check for timed out files every hour.
	 */
	public void run() {
		if (timeDepth > 0) {
			try {
				while (true) {
					checkDocuments();
					sleep(aDay);
				}
			}
			catch (Exception ex) { return; }
		}
	}

	//Remove timed out draft documents.
	private void checkDocuments() {
		long maxAge = timeDepth * aDay;
		if (maxAge <= 0) return;
		long timeNow = System.currentTimeMillis();
		long earliestAllowed = timeNow - maxAge;

		Index index = Index.getInstance(ssid);
		if (index != null) {
			IndexEntry[] draftDocs = index.query( new Query(true), true, null );
			int count = 0;
			for (IndexEntry draftDoc : draftDocs) {
				if (draftDoc.lmdate < earliestAllowed) {
					String path = draftDoc.md.getAttribute("path");
					StorageServiceAdmin.deleteDocument(ssid, path);
					count++;
				}
			}
			if (count > 0) {
				logger.info(count+" draft document" + ((count!=1)?"s":"") + " deleted from "+ssid);
			}
		}
	}
}

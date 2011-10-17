/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import java.io.File;

import mirc.MircConfig;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A Thread to monitor remote libraries and set their enables
 * in the MIRC configuration to minimize the number of times
 * a query will time out because a remote library is down.
 * This thread runs periodically, doing minimal queries on
 * all the remote sites and disabling the ones that do not respond.
 */
public class LibraryMonitor extends Thread implements QueryServiceCallback {

	static final Logger logger = Logger.getLogger(LibraryMonitor.class);
	static final long interval = 60L * 60L * 1000L; //run hourly

	static final String mircQueryString = "";
	static final String cookie = null;

	volatile boolean success = false;

	/**
	 * Construct a LibraryMonitor.
	 */
	public LibraryMonitor() {
		super();
		this.setPriority(Thread.MIN_PRIORITY);
	}

	public void run() {
		while (true) {
			try {
				checkServers();
				sleep(interval);
			}
			catch (Exception e) { }
		}
	}

	//Query all the remote libraries in the configuration and set their enables
	//based on whether they respond.
	//
	private void checkServers() {
		MircConfig mc = MircConfig.getInstance();
		long timeout = mc.getQueryTimeout() * 1000L;
		Element servers = mc.getLibraries(false);
		NodeList serversNodeList = servers.getElementsByTagName("Library");
		boolean changed = false;
		for (int i=0; i<serversNodeList.getLength(); i++) {
			Element server = (Element)serversNodeList.item(i);
			if (!mc.isLocal( server.getAttribute("address") )) {
				boolean oldEnabled = server.getAttribute("enabled").equals("yes");
				boolean newEnabled = checkServer(server, timeout);
				if (oldEnabled != newEnabled) {
					server.setAttribute( "enabled", (newEnabled ? "yes" : "no") );
					mc.insertLibrary(server);
					changed = true;
				}
			}
		}
		if (changed) mc.sortLibraries(); //Note: this does an automatic save and load
	}

	//Query a server and return true if it responded within the timeout.
	//NOTE: this method assumes that the server is remote. If it isn't,
	//the URL will not include the full site URL, and the query will fail.
	private boolean checkServer(Element server, long timeout) {
		long requestTime = System.currentTimeMillis();
		String address = server.getAttribute("address").trim();
		String serverName = server.getTextContent().trim();
		MircServer thread;

		synchronized (this) {
			success = false;
			thread = new MircServer( address, cookie, serverName, mircQueryString, this);
			thread.start();
		}

		//Sleep for the timeout
		try { Thread.sleep(100); }
		catch (Exception ex) { }

		//Stop the MircServer and return the result;
		if (thread.isAlive()) thread.interrupt();
		return success;
	}

	public synchronized void acceptQueryResult(MircServer server, Document result) {
		success = (result != null);
	}
}

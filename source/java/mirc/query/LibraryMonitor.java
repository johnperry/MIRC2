/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import java.io.*;
import java.net.*;

import mirc.MircConfig;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

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
public class LibraryMonitor extends Thread {

	static final Logger logger = Logger.getLogger(LibraryMonitor.class);
	static final long interval = 60L * 60L * 1000L; //60 minutes

	static final String mircQueryString = "<MIRCquery maxresults=\"1\"/>";
	static final String cookie = null;

	volatile boolean success = false;

	/**
	 * Construct a LibraryMonitor.
	 */
	public LibraryMonitor() {
		super("LibraryMonitor");
		this.setPriority(Thread.MIN_PRIORITY);
	}

	public void run() {
		logger.debug("LibraryMonitor started");
		while (true) {
			try {
				//Capture the RSNA system's version.
				checkRSNAVersion();

				//Note, start with a sleep so I can stop and start
				//the system for development testing without
				//peppering the world with unnecessary checks.
				sleep(interval);
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
		int timeout = mc.getQueryTimeout();
		Element servers = mc.getLibraries(false);
		NodeList serversNodeList = servers.getElementsByTagName("Library");
		int changes = 0;
		for (int i=0; i<serversNodeList.getLength(); i++) {
			Element server = (Element)serversNodeList.item(i);
			String address = server.getAttribute("address");
			if (!mc.isLocal(address)) {
				boolean oldEnabled = server.getAttribute("enabled").equals("yes");
				boolean newEnabled = checkServer(server, timeout);
				if (oldEnabled != newEnabled) {
					mc.setLibraryEnable(address, newEnabled);
					changes++;
				}
			}
		}
		if (changes > 0) mc.sortLibraries(); //Note: this does an automatic save and load
		logger.info("Remote library availability scan complete ("+((changes>0)?changes:"no")+" changes were detected)");
	}

	//Query a server and return true if it responded within the timeout.
	//NOTE: this method assumes that the server is remote. If it isn't,
	//the URL will not include the full site URL, and the query will fail.
	private boolean checkServer(Element server, int timeout) {
		BufferedWriter writer = null;
		BufferedReader reader = null;
		HttpURLConnection conn = null;
		char[] cbuf = new char[1024];
		int n;
		long requestTime = System.currentTimeMillis();
		String address = server.getAttribute("address").trim();
		Element titleEl = XmlUtil.getFirstNamedChild(server, "title");
		String title = ( (titleEl!=null) ? titleEl.getTextContent() : address );
		String serverName = server.getTextContent().trim();

		boolean ok = false;
		try {
			URL url = new URL(address);
			if (url.getUserInfo() != null) Authenticator.setDefault(new QueryAuthenticator(url));
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type","text/xml; charset=\"UTF-8\"");
			conn.setDoOutput(true);
			conn.setReadTimeout( 1000 * timeout );
			conn.connect();

			//Send the query to the server
			writer = new BufferedWriter(new OutputStreamWriter( conn.getOutputStream(), FileUtil.utf8 ) );
			writer.write( mircQueryString );
			writer.flush();

			//See if we get a response
			reader = new BufferedReader(new InputStreamReader( conn.getInputStream(), FileUtil.utf8 ) );
			while ((n = reader.read(cbuf,0,1024)) != -1) ; //throw away the contents
			logger.debug("MIRCquery success for "+title);
			ok = true;
		}
		catch (Exception ex) { logger.debug("MIRCquery timeout for "+title); }
		finally {
			FileUtil.close(writer);
			FileUtil.close(reader);
		}
		return ok;
	}

	//Get the Version running on the RSNA site and set it in the configuration
	//for use by the Query Service user interfaces' "About TFS" popups.
	private void checkRSNAVersion() {
		BufferedReader reader = null;
		StringWriter sw = new StringWriter();
		HttpURLConnection conn = null;
		char[] cbuf = new char[1024];
		int n;
		try {
			URL url = new URL("http://mirc.rsna.org/mirc/version");
			conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			conn.setReadTimeout( 10 * 1000 );
			conn.connect();

			//See if we get a response
			reader = new BufferedReader(new InputStreamReader( conn.getInputStream(), FileUtil.utf8 ) );
			while ((n = reader.read(cbuf,0,1024)) != -1) sw.write(cbuf,0,n);
		}
		catch (Exception ignore) { }
		finally { FileUtil.close(reader); }
		MircConfig.getInstance().setRSNAVersion(sw.toString());
	}
}

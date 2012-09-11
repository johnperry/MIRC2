/*---------------------------------------------------------------
*  Copyright 201 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.activity;

import java.io.*;
import java.net.*;

import mirc.MircConfig;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A Thread to send ActivitySummaryReports to the RSNA site.
 */
public class SummarySubmitter extends Thread {

	static final Logger logger = Logger.getLogger(SummarySubmitter.class);
	static final long oneDay = 24L * 60L * 60L * 1000L; //one day
	static final long interval = oneDay;

	/**
	 * Construct a SummarySubmitter.
	 */
	public SummarySubmitter() {
		super("SummarySubmitter");
		this.setPriority(Thread.MIN_PRIORITY);
	}

	public void run() {
		logger.debug("SummarySubmitter started");
		while (true) {
			try {
				submitIfRequired();
				sleep(interval);
			}
			catch (Exception e) { }
		}
	}

	//Send an activity summary report if enabled and it has been
	//more than a week since the last report.
	private void submitIfRequired() {
		MircConfig mc = MircConfig.getInstance();
		if (mc.shareStats()) {
			ActivityDB db = ActivityDB.getInstance();
			long lastReportTime = db.getLastReportTime();
			long now = System.currentTimeMillis();
			if ((now - lastReportTime) > oneDay) {
				try {
					Document doc = db.getXML();
					Document summaryXSL = XmlUtil.getDocument( FileUtil.getStream( "/activity/ActivitySummaryReport.xsl" ) );
					String report = XmlUtil.getTransformedText( doc, summaryXSL, null );
					report = URLEncoder.encode(report, "UTF-8");
					send(report);
					db.setLastReportTime(System.currentTimeMillis());
					logger.info("Activity summary report sent to the RSNA");
				}
				catch (Exception ex) { logger.warn("Unable to send the activity summary report to the RSNA"); }
			}
		}
	}

	private void send(String report) throws Exception {
		BufferedReader reader = null;
		StringWriter sw = new StringWriter();
		HttpURLConnection conn = null;
		char[] cbuf = new char[1024];
		int n;
		URL url = new URL("http://mirc.rsna.org/activity/submit?report="+report);
		conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		conn.setReadTimeout( 60 * 1000 );
		conn.connect();

		//See if we get a response
		reader = new BufferedReader(new InputStreamReader( conn.getInputStream(), FileUtil.utf8 ) );
		while ((n = reader.read(cbuf,0,1024)) != -1) sw.write(cbuf,0,n);
		FileUtil.close(reader);
		if (!sw.toString().startsWith("Thank")) throw new Exception("Report rejected");
	}
}

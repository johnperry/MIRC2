/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.io.File;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import org.rsna.server.HttpRequest;
import org.rsna.server.User;

import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import mirc.MircConfig;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Static method for logging access to and export of MIRCdocuments containing PHI.
 */
public class AccessLog {

	static volatile Logger accessLog = null;
	static final Logger logger = Logger.getLogger(AccessLog.class);
	static final String indent = "\n     ";

    /**
     * This method creates a local log entry in a simple format when a MIRCdocument
     * containing PHI is accessed. If the MIRCdocument contains no PHI, no log entry is created.
     * @param req The request for the MIRCdocument..
     * @param xmlDocument The MIRCdocument that was was accessed.
     */
	public static synchronized void logAccess(HttpRequest req, Document xmlDocument) {
		try {
			//See if the MIRCdocument has PHI
			Element phi = XmlUtil.getFirstNamedChild(xmlDocument, "phi");
			if (phi == null) return;

			//Get the document path
			String path = req.getPath();
			String params = req.getQueryString();

			//Get the user parameters
			User user = req.getUser();
			String username = (user != null) ? user.getUsername() : "[User NOT authenticated]";
			String userip = req.getRemoteAddress();

			//Get the event
			String datetime = StringUtil.getDateTime(" at ");
			String event = req.hasParameter("zip") ? "Export" : "Access";

			//Make sure the log exists
			createAccessLog();

			//Log the entry
			makeAccessLogEntry(datetime,
							   event,
							   username,
							   userip,
							   path,
							   params,
							   phi);
		}
		catch (Exception unable) {
			logger.warn("Unable to create an access log entry for a PHI access.", unable);
		}
	}

	//Create the local access log with a monthly rolling appender
	private static void createAccessLog() {
		if (accessLog == null) {
			MircConfig mc = MircConfig.getInstance();
			File dir = new File( mc.getRootDirectory(), "phi" );
			File log = new File( dir, "AccessLog.txt" );
			try {
				accessLog = Logger.getLogger("PHIAccess");
				accessLog.setAdditivity(false);
				PatternLayout layout = new PatternLayout("%m%n");
				dir.mkdirs();
				DailyRollingFileAppender appender = new DailyRollingFileAppender(
						layout,
						log.getAbsolutePath(),
						"'.'yyyy-MM");
				accessLog.addAppender(appender);
				accessLog.setLevel((Level)Level.ALL);
			}
			catch (Exception e) {
				logger.warn("Unable to instantiate the PHI Access logger");
				accessLog = null;
			}
		}
	}

	//Make an entry in the access log.
	private static void makeAccessLogEntry(
								String datetime,
								String event,
								String username,
								String userip,
								String path,
								String params,
								Element phi) {
		if (accessLog != null) {
			StringBuffer sb = new StringBuffer();
			sb.append(datetime + " - " + event + " by " + username + " @" + userip);
			sb.append(  indent + "path:   " + path
					  + indent + "params: " + params);
			NodeList nl = phi.getElementsByTagName("study");
			for (int i=0; i<nl.getLength(); i++) {
				Element study = (Element)nl.item(i);
				String siuid = XmlUtil.getValueViaPath(study, "study/si-uid");
				String ptid = XmlUtil.getValueViaPath(study, "study/pt-id");
				String ptname = XmlUtil.getValueViaPath(study, "study/pt-name");
				sb.append(  indent + "SIUID:  " + siuid
						  + indent + "Pt ID:  " + ptid
						  + indent + "Name:   " + ptname);
				if (i < nl.getLength()-1) sb.append(indent + "---");
			}
			accessLog.info(sb.toString());
		}
	}
}

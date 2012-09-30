/*---------------------------------------------------------------
 *  Copyright 2012 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.activity;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Encapsulates a database entry for a summary report.
 */
public class SummariesDBEntry implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(SummariesDBEntry.class);

	String id;
	Document report;

	/**
	 * Construct a summary report for storage in the summaries database.
	 * @param report the XML string containing the summary report.
	 */
	public SummariesDBEntry(String report, String ipAddress) throws Exception {
		this( XmlUtil.getDocument(report), ipAddress );
	}

	/**
	 * Construct a summary report for storage in the summaries database.
	 * @param report the summary report XML document.
	 */
	public SummariesDBEntry(Document report, String ipAddress) throws Exception {
		Element root = report.getDocumentElement();
		String idString = root.getAttribute("siteID");
		Long idValue = Long.parseLong(idString);
		if (!root.getTagName().equals("ActivitySummary")
				|| (idValue.longValue() < 1340000000000L)) {
			throw new Exception("Invalid Report");
		}
		root.setAttribute("ip", ipAddress);
		this.report = report;
		this.id = idString;
	}

	/**
	 * Get the site ID for this summary report.
	 */
	public synchronized String getSiteID() {
		return id;
	}

	/**
	 * Get the document for this summary report.
	 */
	public synchronized Document getDocument() {
		return report;
	}

	/**
	 * Get the root element for this summary report.
	 */
	public synchronized Element getDocumentElement() {
		return report.getDocumentElement();
	}

	public synchronized Element getXML(Document doc) {
		try {
			Element reportEl = (Element)doc.importNode(report.getDocumentElement(), true);
			return reportEl;
		}
		catch (Exception unable) {
			return null;
		}
	}
}

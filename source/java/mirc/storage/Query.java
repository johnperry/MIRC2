/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.util.*;
import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.rsna.util.StringUtil;

/**
 * A class to encapsulate a MIRCquery.
 */
public class Query extends Hashtable<String, String> {

	static final Logger logger = Logger.getLogger(Query.class);

	public boolean isBlankQuery = true;
	public boolean containsNonFreetextQueries = false;

	public boolean unknown = false;
	public String bgcolor  = "";
	public String display  = "";
	public String icons    = "";
	public String orderby  = "";
	public int firstresult = 0;
	public int maxresults  = 1;
	public boolean containsAgeQuery = false;
	public int minAge = 0;
	public int maxAge = Integer.MAX_VALUE;

	public Query(String freetext) {
		super();
		freetext = freetext.replaceAll("\\s+", " ").trim();
		isBlankQuery = freetext.equals("");
		this.put("freetext", freetext);
	}

	/**
	 * Construct a Query from an XML document in the form
	 * described on the RSNA MIRC wiki.
	 * @param queryDoc the MIRCquery XML DOM object.
	 */
	public Query(Document queryDoc) {
		super();
		Element root = queryDoc.getDocumentElement();
		unknown = root.getAttribute("unknown").trim().equals("yes");
		bgcolor = root.getAttribute("bgcolor").trim();
		display = root.getAttribute("display").trim();
		icons   = root.getAttribute("icons").trim();
		orderby = root.getAttribute("orderby").trim();
		firstresult = StringUtil.getInt(root.getAttribute("firstresult"));
		if (firstresult <= 0) firstresult = 1;
		maxresults = StringUtil.getInt(root.getAttribute("maxresults"));
		if (maxresults <= 0) maxresults = 1;

		StringBuffer sb = new StringBuffer();
		Node child = root.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String dbname = child.getNodeName();
				String text = getTextContent(child);
				if (!text.equals("")) {
					this.put(dbname, text);
					containsNonFreetextQueries = true;
				}
			}
			else if (child.getNodeType() == Node.TEXT_NODE) {
				sb.append(" " + child.getTextContent());
			}
			child = child.getNextSibling();
		}
		String freetext = sb.toString().replaceAll("\\s+", " ").trim();
		isBlankQuery = freetext.equals("");
		this.put("freetext", freetext);
		setAgeRange(root);
		//log();
	}

	private String getTextContent(Node node) {
		StringBuffer sb = new StringBuffer();
		appendNodeText(sb, node);
		return sb.toString().replaceAll("\\s+", " ").trim();
	}

	private void appendNodeText(StringBuffer sb, Node node) {
		if (node.getNodeType() == Node.TEXT_NODE) {
			sb.append(" " + node.getTextContent());
		}
		else if (node.getNodeType() == Node.ELEMENT_NODE) {
			if (!node.getNodeName().equals("pt-age")) {
				Node child = node.getFirstChild();
				while (child != null) {
					appendNodeText(sb, child);
					child = child.getNextSibling();
				}
			}
		}
	}

	//Set up the age range parameters
	private void setAgeRange(Element root) {
		NodeList nl = root.getElementsByTagName("pt-age");
		if (nl.getLength() != 0) {
			Element ptAge = (Element)nl.item(0);
			int[] ageRange = getAgeRange(ptAge);
			containsAgeQuery = (ageRange[0] != -1);
			if (containsAgeQuery) {
				minAge = ageRange[1];
				maxAge = ageRange[2];
			}
		}
	}

	/**
	 * Get an age range array from a MIRCquery (or MIRCdocument) pt-age element.
	 * @param ptAge the pt-age element.
	 * @return the age range array, containing three values:
	 * <ul><li>[0]: -1 if no age range was found; 0 if an age range was found
	 * <li>[1]: the minimum age in the range
	 * <li>[2]: the maximum age in the range
	 * </ul>If a single age was found, it is returned in both the min and max age values.
	 */
	public static int[] getAgeRange(Element ptAge) {
		int ageRange[] = {-1,0,0};
		ageRange = updateAgeRange(ageRange, getChildText(ptAge,"years"), 365);
		ageRange = updateAgeRange(ageRange, getChildText(ptAge,"months"), 30);
		ageRange = updateAgeRange(ageRange, getChildText(ptAge,"weeks"), 7);
		ageRange = updateAgeRange(ageRange, getChildText(ptAge,"days"), 1);
		return ageRange;
	}

	//Get the text value of a child element with a specified name.
	private static String getChildText(Element el, String childName) {
		String value = "";
		Node child = el.getFirstChild();
		while (child != null) {
			if ((child.getNodeType() == Node.ELEMENT_NODE) &&
				child.getNodeName().equals(childName)) {
					return child.getTextContent();
			}
			child = child.getNextSibling();
		}
		return value;
	}

	//Update the age range array.
	//
	//The values in the array are:
	//0: a flag indicating that values have been inserted.
	//1: the lower limit of the age range.
	//2: the upper limit of the age range.
	//
	//The input string is a number or range (e.g., 17 or 11-19).
	//The multiplier converts the number to days.
	//If an error occurs while parsing the input string, the
	//age range is returned with the best value possible under
	//the circumstances.
	private static int[] updateAgeRange(int[] a, String s, int multiplier) {
		s = s.trim();
		if (s.equals("")) return a;
		String[] sParts = s.split("-");
		if (sParts.length == 0) return a;
		int v1 = 0;
		try {
			v1 = Integer.parseInt(sParts[0].trim()) * multiplier;
			a[0] = 0;
			a[1] += v1;
		}
		catch (Exception e) { return a; }
		if (sParts.length == 1) {
			a[2] += v1;
			return a;
		}
		try {
			int v2 = Integer.parseInt(sParts[1].trim()) * multiplier;
			a[2] += v2;
		}
		catch (Exception e) {
			a[2] += v1;
		}
		return a;
	}

	/**
	 * Log the contents of the MircQuery.
	 */
	public void log() {
		logger.warn("isBlankQuery     = "+ isBlankQuery);
		logger.warn("containsNonFreetextQueries = "+ containsNonFreetextQueries);

		logger.warn("unknown          = "+ unknown);
		logger.warn("bgcolor          = "+ bgcolor);
		logger.warn("display          = "+ display);
		logger.warn("icons            = "+ icons);
		logger.warn("firstresult      = "+ firstresult);
		logger.warn("maxresults       = "+ maxresults);
		logger.warn("containsAgeQuery = "+ containsAgeQuery);
		logger.warn("minAge           = "+ minAge);
		logger.warn("maxAge           = "+ maxAge);
		for (String key : keySet()) {
			logger.warn("field "+key+": \""+ get(key)+"\"");
		}
	}

}



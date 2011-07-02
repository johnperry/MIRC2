/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.prefs;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.rsna.util.XmlUtil;

/**
 * A class to encapsulate a single export destination.
 */
public class ExportSite implements Comparable<ExportSite> {

	String name;
	String url;
	String username;
	String password;

	/**
	 * Create a ExportSite.
	 * @param name the name of the site.
	 * @param url the fully qualified URL of the site.
	 * @param username the username to be used to access the site.
	 * @param password the password to be used to access the site.
	 */
	public ExportSite(String name, String url, String username, String password) {
		this.name = name;
		this.url = url;
		this.username = username;
		this.password = password;
	}

	/**
	 * Create a site Element and populate its attributes with
	 * the values of the name, url, username, and password properties.
	 * @param doc the Document to be used to create the Element.
	 * @return the site Element
	 */
	public Element getSiteElement(Document doc) {
		Element site = doc.createElement("site");
		site.setAttribute("name", name);
		site.setAttribute("url", url);
		site.setAttribute("username", username);
		site.setAttribute("password", password);
		return site;
	}

	/**
	 * Implementation of the comparable interface;
	 * comparing on the name property.
	 */
	public int compareTo(ExportSite site) {
		return this.name.compareTo(site.name);
	}

}
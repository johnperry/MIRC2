/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import org.w3c.dom.*;
import org.rsna.server.User;
import org.rsna.util.JdbmUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

/**
 * The encapsulation of the information required for the index of a single MIRCdocument.
 * This object is stored in the IDToMIE table in the index of a storage service.
 */
public class IndexEntry implements Serializable {

	static final long serialVersionUID = 1;

	public Element md		= null;
	public String title		= "";
	public String pubdate	= "";
	public long lmdate		= 0;
	public int img          = 0;
	public int ann          = 0;
	public String access    = "";
	public String lmstring	= "";
	public boolean isPublic = false;
	public boolean hasPubReq= false;
	public int[] ptAges		= new int[0];

	public HashSet<String> owners = new HashSet<String>();
	public HashSet<String> users  = new HashSet<String>();
	public HashSet<String> roles  = new HashSet<String>();

	/**
	 * Create an IndexEntry.
	 * @param file the file containing the MIRCdocument
	 * @param path the path by which the document is to be indexed
	 * @param doc the XML DOM object containing the parsed MIRCdocument
	 */
	public IndexEntry(File file, String path, Document doc, Document xsl) throws Exception {

		lmdate = file.lastModified();
		lmstring = StringUtil.getDate(lmdate, ".");

		//Transform the MIRCdocument into an indexable object containing
		//only those things which are necessary for the creation of query results
		//and author summaries.
		Element root = doc.getDocumentElement();
		root.setAttribute("path", path);
		Object[] params = {
				"lmdate",	Long.toString(lmdate),
				"lmstring",	lmstring
		};
		Document qrDoc = XmlUtil.getTransformedDocument( doc, xsl, params );

		//Now get the key information from the result to populate this object.
		Element qrRoot = qrDoc.getDocumentElement();
		Node child = qrRoot.getFirstChild();
		while (child != null) {
			if (child instanceof Element) {
				String name = child.getNodeName();
				if (name.equals("MIRCdocument")) {
					md = (Element)child;
					//Save the title so title sorts are faster.
					Node xChild = child.getFirstChild();
					while (xChild != null) {
						if (xChild instanceof Element) {
							String xChildName = xChild.getNodeName();
							if (xChildName.equals("title")) {
								title = xChild.getTextContent().trim();
								break;
							}
						}
						xChild = xChild.getNextSibling();
					}
				}
				if (name.equals("sm")) {
					//Unpack the summary element
					Node xChild = child.getFirstChild();
					while (xChild != null) {
						if (xChild instanceof Element) {
							String xChildName = xChild.getNodeName();
							if (xChildName.equals("img")) {
								img = getInt( ((Element)xChild).getAttribute("n") );
							}
							else if (xChildName.equals("ann")) {
								ann = getInt( ((Element)xChild).getAttribute("n") );
							}
							else if (xChildName.equals("pubdate")) {
								pubdate = xChild.getTextContent().trim();
							}
							else if (xChildName.equals("access")) {
								access = xChild.getTextContent().trim();
							}
							else if (xChildName.equals("owner")) {
								insert( xChild, "user", owners );
							}
							else if (xChildName.equals("read")) {
								insert( xChild, "user", users );
								insert( xChild, "role", roles );
							}
						}
						xChild = xChild.getNextSibling();
					}
				}
			}
			child = child.getNextSibling();
		}
		//Set a flag to make the Author Summary query faster.
		isPublic =
				access.equals("public")
						|| roles.contains("*")
								|| ( (users.size() == 0) && (roles.size() == 0) );

		//Set a flag to assist the Approval Queue query.
		hasPubReq = root.getAttribute("pubreq").equals("yes");

		//Get the ages of all the patients in this document
		//to make age range searches faster.
		NodeList nl = root.getElementsByTagName("pt-age");
		ArrayList<Integer> ages = new ArrayList<Integer>();
		for (int i=0; i<nl.getLength(); i++) {
			Element el = (Element)nl.item(i);
			int age = getAge(el);
			if (age >= 0) ages.add(new Integer(age));
		}
		if (ages.size() > 0) {
			ptAges = new int[ages.size()];
			for (int i=0; i<ptAges.length; i++) {
				ptAges[i] = ages.get(i).intValue();
			}
		}
	}

	/**
	 * Determine whether this document contains a patient
	 * age in a specified range. Note: all ages are in days,
	 * calculated as in the MircQuery.getAgeRange method
	 * (365*years + 30*months + 7*weeks + days).
	 * @param minAge the minimum age in days.
	 * @param maxAge the maximum age in days.
	 * @return true if the document contains a patient
	 * whose age falls in the specified range (inclusive)..
	 */
	public boolean hasPatientInAgeRange(int minAge, int maxAge) {
		for (int age: ptAges) {
			if ((age >= minAge) && (age <= maxAge)) return true;
		}
		return false;
	}

	private int getAge(Element ptAge) {
		int age = 0;
		Node child = ptAge.getFirstChild();
		while (child != null) {
			if (child.getNodeName().equals("years"))
				age += 365 * StringUtil.getInt(child.getTextContent());
			else if (child.getNodeName().equals("months"))
				age += 30 * StringUtil.getInt(child.getTextContent());
			else if (child.getNodeName().equals("weeks"))
				age += 7 * StringUtil.getInt(child.getTextContent());
			else if (child.getNodeName().equals("days"))
				age += StringUtil.getInt(child.getTextContent());
			child = child.getNextSibling();
		}
		return age;
	}

	/**
	 * Determine whether a user is allowed to read this document.
	 * @param user the user
	 * @return true if the document is public or if the user is
	 * an admin user or if the user is one of the allowed users
	 * or if the user has one of the allowed roles, or if the
	 * document has a publish request and the user is a publisher;
	 * false otherwise.
	 */
	public boolean allows(User user) {
		if (isPublic) return true;
		if (user == null) return false;
		if (user.hasRole("admin")) return true;
		if (hasPubReq && user.hasRole("publisher")) return true;
		if (users.contains(user.getUsername())) return true;
		String[] userroles = user.getRoleNames();
		for (String role : userroles) {
			if (roles.contains(role)) return true;
		}
		return false;
	}

	private void insert( Node node, String name, HashSet<String> set ) {
		Node child = node.getFirstChild();
		while (child != null) {
			if ((child instanceof Element) && child.getNodeName().equals(name)) {
				set.add( child.getTextContent().trim() );
			}
			child = child.getNextSibling();
		}
	}

	private int getInt(String s) {
		try { return Integer.parseInt(s); }
		catch (Exception ex) { return 0; }
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("title: " + title + "\n");
		sb.append("pubdate: " + pubdate + "\n");
		sb.append("lmdate: " + lmdate + "\n");
		sb.append("lmstring: " + lmstring + "\n");
		sb.append("access: " + access + "\n");
		sb.append("isPublic: " + isPublic + "\n");
		sb.append("img: " + img + "\n");
		sb.append("ann: " + ann + "\n");
		sb.append("owners: " + listValues(owners) + "\n");
		sb.append("read users: " + listValues(users) + "\n");
		sb.append("read roles: " + listValues(roles) + "\n");
		return sb.toString();
	}

	private String listValues( HashSet<String> set ) {
		boolean first = true;
		StringBuffer sb = new StringBuffer();
		for (String value : set) {
			if (!first) sb.append(";");
			sb.append(value);
			first = false;
		}
		return sb.toString();
	}

}
/*---------------------------------------------------------------
*  Copyright 2009 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.confs;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Encapsulates a single conference.
 */
public class Conference implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(Conference.class);

	public String owner = null;
	public String id = null;
	public String pid = null;
	public String title = null;
	public List<AgendaItem> agenda = null;
	public List<String> children = null;

	/**
	 * Construct a new Conference.
	 * @param owner the username of the conference owner
	 * @param id the ID of the conference
	 * @param pid the ID of the parent of the conference
	 * @param title the title of the conference
	 */
	public Conference(String owner, String id, String pid, String title) {
		this.owner = owner;
		this.id = id;
		this.pid = pid;
		this.title = title;
		agenda = new LinkedList<AgendaItem>();
		children = new LinkedList<String>();
	}

	/**
	 * Append an AgendaItem to the agenda. This method replaces
	 * any existing AgendaItem with the same url field with
	 * the new item.
	 * @param item the AgendaItem to append.
	 */
	public void appendAgendaItem(AgendaItem item) {
		//First, remove any items with the same URL
		removeAgendaItem(item.url);
		//Now add the new item
		agenda.add(item);
	}

	/**
	 * Remove an AgendaItem from the agenda.
	 * @param removeURL the URL of the AgendaItem to remove.
	 * @return the removed AgendaItem, or null if no item
	 * could be found with the specified URL.
	 */
	public AgendaItem removeAgendaItem(String removeURL) {
		ListIterator<AgendaItem> it = agenda.listIterator();
		while (it.hasNext()) {
			AgendaItem aItem = it.next();
			if (aItem.url.equals(removeURL)) {
				it.remove();
				return aItem;
			}
		}
		return null;
	}

	/**
	 * Move an AgendaItem to before another one.
	 * @param sourceURL the URL of the AgendaItem to move.
	 * @param targetURL the URL of the target AgendaItem.
	 */
	public void moveAgendaItem(String sourceURL, String targetURL) {
		//Find the source and target items
		int source = -1;
		int target = -1;
		AgendaItem aItem;
		int item = 0;
		ListIterator<AgendaItem> it = agenda.listIterator();
		while (it.hasNext()) {
			aItem = it.next();
			if (aItem.url.equals(sourceURL)) source = item;
			if (aItem.url.equals(targetURL)) target = item;
			item++;
		}
		if ((source != -1) && (target != -1) && (source != target)) {
			aItem = agenda.remove(source);
			if (source < target) agenda.add(target-1, aItem);
			else agenda.add(target,aItem);
		}
	}

	/**
	 * Append a conference to the list of children.
	 * @param childConference the conference to append.
	 */
	public void appendChildConference(Conference childConference) {
		if (!children.contains(childConference.id)) {
			children.add(childConference.id);
		}
	}

	/**
	 * Remove a conference from the list of children.
	 * @param removeID the ID of the conference to remove.
	 */
	public void removeChildConference(String removeID) {
		ListIterator<String> it = children.listIterator();
		while (it.hasNext()) {
			String childID = it.next();
			if (childID.equals(removeID)) it.remove();
		}
	}

	/**
	 * Get the conference as CaseNavigator result list.
	 */
	public Document getCaseNavigatorURLs() {
		Document doc = null;
		try { doc = XmlUtil.getDocument(); }
		catch (Exception ex) { return null; }
		Element root = doc.createElement("Results");
		doc.appendChild(root);
		Iterator<AgendaItem> it = agenda.iterator();
		while (it.hasNext()) {
			AgendaItem item = it.next();
			Element md = doc.createElement("MIRCdocument");
			md.setAttribute("docref", item.url);
			root.appendChild(md);
		}
		return doc;
	}

}
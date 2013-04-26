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

/**
 * Encapsulates a single agenda item for a conference.
 */
public class AgendaItem implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(AgendaItem.class);

	public String url = null;
	public String title = null;
	public String alturl = null;
	public String alttitle = null;
	public String subtitle = null;

	/**
	 * Construct a new Conference.
	 * @param url the URL of the referenced agenda item
	 * @param title the title to be displayed either as a link or as a tooltip.
	 * @param alturl the URL to be used in place of the url in
	 * special circumstances (as when a conference is displayed as unknowns).
	 * @param alttitle the title to be displayed in place of the title in
	 * special circumstances (as when a conference is displayed as unknowns).
	 * @param subtitle the subtitle to be displayed if the agenda item is
	 * shown as a link. This is intended to be used to show the author(s) of a
	 * referenced MIRCdocument.
	 */
	public AgendaItem(String url, String title, String alturl, String alttitle, String subtitle) {
		this.url = url;
		this.title = title;
		this.alturl = alturl;
		this.alttitle = alttitle;
		this.subtitle = subtitle;
	}

	/**
	 * Clone this agenda item.
	 */
	public AgendaItem clone() {
		return new AgendaItem(url, title, alturl, alttitle, subtitle);
	}

}
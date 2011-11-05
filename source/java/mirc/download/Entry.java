/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.download;

import java.io.File;
import java.io.Serializable;

/**
 * A class to encapsulate a download database entry.
 */
public class Entry implements Serializable {

	public static final long serialVersionUID = 1L;
	public String name;
	public String build;
	public String ip;
	public String email;
	public String pname;
	public String iname;
	public String interest;
	public String sitetype;

	/**
	 * Construct a download database entry.
	 * @param name the name of the file that was downloaded
	 * @param build the build date of the file
	 * @param ip the IP address of the client downloading the file
	 * @param email the email address of the client
	 */
	public Entry(String name,
				 String build,
				 String ip,
				 String email,
				 String pname,
				 String iname,
				 String interest,
				 String sitetype
				 ) {
		this.name = name;
		this.build = build;
		this.ip = ip;
		this.email = email;
		this.pname = pname;
		this.iname = iname;
		this.interest = interest;
		this.sitetype = sitetype;
	}
}


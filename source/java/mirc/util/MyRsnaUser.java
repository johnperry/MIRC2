/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.Serializable;

/**
 * A class to encapsulate one MyRSNA user.
 */
public class MyRsnaUser implements Comparable, Serializable {

	public static final long serialVersionUID = 1;

	public String username;
	public String password;

	/**
	 * Construct a new MyRsnaUser.
	 */
	public MyRsnaUser(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Compare this user to another user to determine the sorting order.
	 * @param otherUser the object representing the other user.
	 * @return the sorting order: -1 if this user precedes the other user;
	 * 0 if the names are identical; +1 if this user follows the other user.
	 */
	public int compareTo(Object otherUser) {
		return username.compareTo(((MyRsnaUser)otherUser).username);
	}

	/**
	 * Make a String for this user.
	 * @return the user's parameters.
	 */
	public String toString() {
		return "[" + username + ":" + password + "]";
	}

}


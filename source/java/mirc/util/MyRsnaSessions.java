/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.File;
import java.util.Hashtable;
import org.apache.log4j.Logger;

/**
 * A class to encapsulate a Hashtable of MyRsnaSessions.
 */
public class MyRsnaSessions {

	static final Logger logger = Logger.getLogger(MyRsnaSessions.class);

	private static MyRsnaSessions myRsnaSessions = null;

	private Hashtable<String, MyRsnaSession> sessions;

	/**
	 * Protected constructor.
	 */
	protected MyRsnaSessions() {
		sessions = new Hashtable<String, MyRsnaSession>();
	}

	/**
	 * Get the singleton instance of the MyRsnaUsers database.
	 * This method is intended for normal classes.
	 */
	public static MyRsnaSessions getInstance() {
		if (myRsnaSessions == null) myRsnaSessions = new MyRsnaSessions();
		return myRsnaSessions;
	}

	/**
	 * Get a MyRsnaSession for a specific MIRC user, creating a session
	 * if one doesn't already exist.
	 * @param mircUsername the username of the user on the MIRC site.
	 * @return the user's MyRsnaSession or null if a session could not be created.
	 * This can occur if the user does not have a MyRsnaUser available in the
	 * MyRsnaUsers database, or if it is not possible to contact the MyRSNA site,
	 * or if the MyRsnaUser contains credentials which are rejected by the
	 * MyRSNA site.
	 */
	public synchronized MyRsnaSession getMyRsnaSession(String mircUsername) {
		MyRsnaSession mrs = sessions.get(mircUsername);
		if (mrs != null) {
			//Verify that the session is still active.
			if (mrs.isOpen()) return mrs;
			//It's closed, remove the session.
			sessions.remove(mircUsername);
			mrs = null;
		}
		if (mrs == null) {
			//Okay, we don't have an open session in the cache; create a new one.
			MyRsnaUser mru = MyRsnaUsers.getInstance().getMyRsnaUser(mircUsername);
			if (mru != null) {
				mrs = new MyRsnaSession(mru);
				if (mrs.login()) {
					sessions.put(mircUsername, mrs);
					return mrs;
				}
				else logger.warn("Unable to login to myRSNA ("+mircUsername+";"+mrs.username+")");
			}
		}
		return null;
	}

}
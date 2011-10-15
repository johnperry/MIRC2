/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import java.io.*;
import java.net.*;

/**
 * An Authenticator to allow a MIRC query service to authenticate
 * itself to a MIRC storage service during the query exchange.
 * This class is intended for storage services that have guest
 * accounts for query services. It does not pass individual
 * user credentials. The username and password of the guest
 * account are part of the URL of the storage service; therefore, all
 * users have the same credentials during the query exchange.
 */
public class QueryAuthenticator extends Authenticator {

	String username = null;
	String password = null;

	/**
	 * Constructs the authenticator and obtains the username
	 * and password from the URL of the storage service.
	 * @param url the URL of the storage service
	 */
	public QueryAuthenticator(URL url) {
		String userinfo = url.getUserInfo();
		if (userinfo != null) {
			String[] np = userinfo.split(":");
			if (np.length == 2) {
				username = np[0];
				password = np[1];
			}
		}
	}

	/**
	 * Provides the credentials during the authentication challenge.
	 * @return the credentials.
	 */
	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(username, password.toCharArray());
	}

}

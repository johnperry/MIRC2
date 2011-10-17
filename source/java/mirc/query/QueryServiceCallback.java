/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import org.w3c.dom.Document;

/**
 * The MIRC Query Service Callback Interfacee.
 */
public interface QueryServiceCallback {

	/**
	 * Accept a query result from a MircServer.
	 */
	public void acceptQueryResult(MircServer server, Document result);

}

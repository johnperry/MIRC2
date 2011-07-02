/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

/**
 * The MIRC Query Service User Interface Interfacee.
 */
public interface UI {

	/**
	 * Get the user interface page.
	 */
	public String getPage() throws Exception;
}

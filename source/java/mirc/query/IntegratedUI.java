/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import mirc.MircConfig;
import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;

/**
 * The MIRC Query Service Integrated UI.
 */
public class IntegratedUI implements UI {

	static final Logger logger = Logger.getLogger(IntegratedUI.class);

	/**
	 * Construct an IntegratedUI.
	 */
	public IntegratedUI() { }

	/**
	 * Return an HTML page implementing the MIRC Integrated UI.
	 */
	public String getPage() throws Exception {

		MircConfig mc = MircConfig.getInstance();
		Document mircXML = mc.getXML();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/query/IntegratedUI.xsl" ) );
		return XmlUtil.getTransformedText( mircXML, xsl, null );

	}

}

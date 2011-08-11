/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License. (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import java.io.File;
import mirc.MircConfig;
import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;

/**
 * The MIRC Query Service Classic UI.
 */
public class ClassicUI implements UI {

	static final Logger logger = Logger.getLogger(ClassicUI.class);

	/**
	 * Construct a ClassicUI.
	 */
	public ClassicUI() { }

	/**
	 * Return an HTML page implementing the MIRC Classic UI.
	 */
	public String getPage() throws Exception {

		MircConfig mc = MircConfig.getInstance();

		Document mircXML = mc.getXML();

		File root = mc.getRootDirectory();
		File query = new File(root, "query");
		File xslFile = new File(query, "ClassicUI.xsl");
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( xslFile, "/query/ClassicUI.xsl" ) );

		Object[] params = new Object[] {
			"options", mc.enumeratedValues,
			"species", mc.speciesValues
		};
		return XmlUtil.getTransformedText( mircXML, xsl, params );

	}

}

/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.radlex;

import java.io.File;
import mirc.MircConfig;
import mirc.util.RadLexIndex;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.servlets.Servlet;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Element;

/**
 * The RadLex term suggester servlet.
 * This servlet provides AJAX access to the RadLex index. It responds
 * to a GET by finding those terms in the index whose first word starts
 * with a key query parameter.
 */
public class RadLexSuggest extends Servlet {

	/**
	 * Construct a RadLexSuggest servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public RadLexSuggest(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * This method returns an XML object which contains RadLex terms
	 * whose first word starts with the supplied key query parameter.
	 * @param req The HttpServletRequest provided by the servlet container.
	 * @param res The HttpServletResponse provided by the servlet container.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		try {
			res.setContentType("xml");
			String key = req.getParameter("key");
			Element result = RadLexIndex.getSuggestedTerms(key);
			if (result == null) res.write("<RadLexTerms/>");
			else res.write(XmlUtil.toString(result));
			res.send();
		}
		catch (Exception error) {
			res.write("<RadLexTerms/>");
			res.send();
		}
	}

}












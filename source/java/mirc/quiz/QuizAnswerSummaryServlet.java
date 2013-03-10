/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.quiz;

import mirc.MircConfig;

import java.io.File;
import java.util.Hashtable;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

import mirc.storage.Index;
import mirc.util.MircDocument;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Stored Quiz Answer Summary Servlet.
 * The Quiz Answer Summary Servlet provides lists all the answers
 * submitted for a single question.
 */
public class QuizAnswerSummaryServlet extends Servlet {

	static final Logger logger = Logger.getLogger(QuizAnswerSummaryServlet.class);

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available. This method is only
	 * called once as the server starts.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a QuizAnswerSummaryServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public QuizAnswerSummaryServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a page listing all the answers to a single question.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {
		res.setContentType("html");
		res.disableCaching();
		Path path = req.getParsedPath();

		//If this is a request for a file,
		//just handle it and return
		if (path.length() == 2) {
			super.doGet(req, res);
			return;
		}

		//It must be a request for the answer summary page.
		String ssid = path.element(1);
		String qid = req.getParameter("qid", "");

		//Get the document
		Index index = Index.getInstance(ssid);
		String docPath = path.subpath(3).substring(1);
		File docFile = new File( index.getDocumentsDir(), docPath );
		MircDocument md = new MircDocument(docFile);

		//Make sure the user owns the document.
		//Only owners are allowed to score quiz questions,
		//so only owners can see all the answers.
		if (md.hasOwner(req.getUser())) {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Quiz");
			doc.appendChild(root);

			ScoredQuizDB db = ScoredQuizDB.getInstance();

			Document mdDoc = md.getXML();
			Element mdRoot = mdDoc.getDocumentElement();
			root.appendChild( doc.importNode(XmlUtil.getFirstNamedChild(mdRoot, "title"), true) );
			NodeList nl = mdRoot.getElementsByTagName("ScoredQuestion");
			for (int i=0; i<nl.getLength(); i++) {
				Element docSQ = (Element)nl.item(i);
				String docQID = docSQ.getAttribute("id");
				if (docQID.equals(qid)) {
					Element sumSQ = doc.createElement("ScoredQuestion");
					sumSQ.setTextContent( docSQ.getTextContent() );
					root.appendChild(sumSQ);
					Question q = db.get(qid);
					String[] respondentIDs = q.getRespondentIDs();
					Hashtable<String,Integer> sumTable = new Hashtable<String,Integer>();
					for (String rid : respondentIDs) {
						Answer ans = q.get(rid);
						String value = ans.getValue();
						Integer count = sumTable.get(value);
						if (count == null) count = new Integer(0);
						count = new Integer( 1 + count.intValue() );
						sumTable.put(value, count);
					}
					for (String value : sumTable.keySet()) {
						Integer count = sumTable.get(value);
						Element ansEl = doc.createElement("Answer");
						ansEl.setAttribute("n", Integer.toString( count.intValue() ));
						ansEl.setTextContent(value);
						sumSQ.appendChild(ansEl);
					}
				}
			}
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/quizmgr/ScoredQuizAnswerSummary.xsl" ) );
			res.write( XmlUtil.getTransformedText( doc, xsl, null ) );
		}
		else res.setResponseCode(res.notfound);
		res.send();
	}

}
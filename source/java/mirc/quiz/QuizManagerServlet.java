/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.quiz;

import mirc.MircConfig;

import java.io.File;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import mirc.prefs.Preferences;
import mirc.storage.Index;
import mirc.util.MircDocument;

import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Stored Quiz Manager Servlet.
 * The Quiz Manager Servlet provides an interface for
 * scoring quiz questions in MIRCdocuments.
 */
public class QuizManagerServlet extends Servlet {

	static final Logger logger = Logger.getLogger(QuizManagerServlet.class);

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
	 * Construct a QuizManagerServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public QuizManagerServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a page containing a form for scoring all
	 * the questions in a single MIRCdocument.
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

		//It must be a request for the score card page.
		String ssid = path.element(1);

		//Get the document
		Index index = Index.getInstance(ssid);
		String docPath = path.subpath(3).substring(1);
		File docFile = new File( index.getDocumentsDir(), docPath );
		MircDocument md = new MircDocument(docFile);

		Preferences prefs = Preferences.getInstance();

		//Make sure the user owns the document.
		//Only owners are allowed to score quiz questions.
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
				Element sq = (Element)doc.importNode(nl.item(i), true);
				root.appendChild(sq);
				String id = sq.getAttribute("id");
				Question q = db.get(id);
				String[] respondentIDs = q.getRespondentIDs();
				for (String rid : respondentIDs) {
					Answer ans = q.get(rid);
					Element ansEl = doc.createElement("Answer");
					ansEl.setAttribute("id", rid);
					ansEl.setAttribute("name", prefs.get(rid,true).getAttribute("name"));
					ansEl.setAttribute("score", Integer.toString(ans.getScore()));
					ansEl.setTextContent(ans.getValue());
					sq.appendChild(ansEl);
				}
			}
			Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/quizmgr/ScoredQuiz.xsl" ) );
			String[] params = { "url", req.getPath() };
			res.write( XmlUtil.getTransformedText( doc, xsl, params ) );
		}
		else res.setResponseCode(res.notfound);
		res.send();
	}

	/**
	 * Handle a score submission.
	 */
	public void doPost(HttpRequest req, HttpResponse res ) throws Exception {
		res.setContentType("html");
		res.disableCaching();
		Path path = req.getParsedPath();
		String ssid = path.element(1);

		//Get the document
		Index index = Index.getInstance(ssid);
		String docPath = path.subpath(3).substring(1);
		File docFile = new File( index.getDocumentsDir(), docPath );
		MircDocument md = new MircDocument(docFile);

		//Make sure the user owns the document.
		//Only owners are allowed to score quiz questions.
		if (md.hasOwner(req.getUser())) {
			String xml = req.getParameter("xml", "<null/>");
			Document doc = XmlUtil.getDocument(xml);
			Element root = doc.getDocumentElement();
			Node qNode = root.getFirstChild();
			while (qNode != null) {
				if ((qNode instanceof Element) && qNode.getNodeName().equals("Question")) {
					processQuestion( (Element)qNode );
				}
				qNode = qNode.getNextSibling();
			}
			doGet(req, res);
			return;
		}

		//Not an owner
		res.setResponseCode(res.notfound);
		res.send();
	}

	private void processQuestion(Element qElement) {
		ScoredQuizDB db = ScoredQuizDB.getInstance();
		String qID = qElement.getAttribute("id").trim();
		if (!qID.equals("")) {
			Question question = db.get(qID);
			Node aNode = qElement.getFirstChild();
			while (aNode != null) {
				if ((aNode instanceof Element) && aNode.getNodeName().equals("Answer")) {
					Element aEl = (Element)aNode;
					String userID = aEl.getAttribute("id");
					int score = StringUtil.getInt(aEl.getAttribute("score"));
					Answer answer = question.get(userID);
					if (answer != null) {
						answer.setScore(score);
						question.put(userID, answer);
					}
				}
				aNode = aNode.getNextSibling();
			}
		}
	}

}
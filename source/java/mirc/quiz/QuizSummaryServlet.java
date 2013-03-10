/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.quiz;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Hashtable;

import mirc.MircConfig;
import mirc.prefs.Preferences;
import mirc.storage.Index;
import mirc.util.MircDocument;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A Servlet to produce a summary page listing the total scores for multiple MIRCdocuments..
 */
public class QuizSummaryServlet extends Servlet {

	static final Logger logger = Logger.getLogger(QuizSummaryServlet.class);

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) { }

	/**
	 * Construct a QuizSummaryServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public QuizSummaryServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Get a web page containing a quiz summary for a list of URLs.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		String suppressHome = req.getParameter("suppressHome", "no");
		String urlsParam = req.getParameter("urls");

		if (req.userHasRole("admin")) {
			Preferences prefs = Preferences.getInstance();
			MircConfig mc = MircConfig.getInstance();

			//Total the scores for all the local documents
			Hashtable<String,Score> scores = new Hashtable<String,Score>();
			if (urlsParam != null) {
				String[] urls = urlsParam.split("\\|");
				for (String url : urls) {
					if (mc.isLocal(url)) {
						addScoresForDocument(scores, url);
					}
				}
			}

			//Make the page
			Score[] scoresArray = new Score[ scores.size() ];
			scoresArray = scores.values().toArray(scoresArray);
			Arrays.sort(scoresArray);
			try {
				Document doc = XmlUtil.getDocument();
				Element root = doc.createElement("Users");
				doc.appendChild(root);
				for (Score score : scoresArray) {
					score.appendTo(root, prefs);
				}

				Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/quizmgr/ScoredQuizSummary.xsl" ) );
				res.write( XmlUtil.getTransformedText( doc, xsl, null ) );
				res.disableCaching();
				res.setContentType("html");
				res.send();
				return;
			}
			catch (Exception ex) { }
		}
		//Unable or not allowed; return 404
		res.setResponseCode(res.notfound);
		res.send();
	}

	class Score implements Comparable<Score> {
		String id;
		int score;
		public Score(String id) {
			this.id = id;
			this.score = 0;
		}
		public String getID() {
			return id;
		}
		public int getScore() {
			return score;
		}
		public int addScore(int inc) {
			score += inc;
			return score;
		}
		public void setScore(int score) {
			this.score = score;
		}
		public int compareTo(Score s) {
			int ss = s.getScore();
			if (score > ss) return -1;
			if (score == ss) return 0;
			return 1;
		}
		public void appendTo(Element root, Preferences prefs) {
			Element user = root.getOwnerDocument().createElement("User");
			user.setAttribute("id", id);
			user.setAttribute("score", Integer.toString(score));
			user.setAttribute("name", prefs.get(id,true).getAttribute("name"));
			root.appendChild(user);
		}
	}

	private void addScoresForDocument(Hashtable<String,Score> scores, String urlString) {
		try {
			URL url = new URL(urlString);
			Path path = new Path(url.getPath());
			String ssid = path.element(1);
			String docPath = path.subpath(3);
			Index index = Index.getInstance(ssid);
			File docFile = new File( index.getDocumentsDir(), docPath );
			MircDocument md = new MircDocument(docFile);
			ScoredQuizDB db = ScoredQuizDB.getInstance();
			Document mdDoc = md.getXML();
			Element mdRoot = mdDoc.getDocumentElement();
			NodeList nl = mdRoot.getElementsByTagName("ScoredQuestion");
			for (int i=0; i<nl.getLength(); i++) {
				Element sq = (Element)nl.item(i);
				String id = sq.getAttribute("id");
				Question q = db.get(id);
				String[] respondentIDs = q.getRespondentIDs();
				for (String rid : respondentIDs) {
					Answer ans = q.get(rid);
					Score score = scores.get(rid);
					if (score == null) score = new Score(rid);
					score.addScore(ans.getScore());
					scores.put(rid, score);
				}
			}
		}
		catch (Exception skipDocument) { }
	}
}

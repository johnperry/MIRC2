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

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The Stored Quiz Servlet.
 * The Quiz Servlet provides an interface to the Stored Quiz database.
 */
public class QuizServlet extends Servlet {

	static final Logger logger = Logger.getLogger(QuizServlet.class);

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
	 * Construct a QuizServlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public QuizServlet(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Handle GET requests for individual answers.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {
			res.setContentType("xml");

		//Require authentication
		if (req.isFromAuthenticatedUser()) {
			String username = req.getUser().getUsername();
			Path path = req.getParsedPath();

			ScoredQuizDB db = ScoredQuizDB.getInstance();
			String questionID = path.element(1);
			Question question = db.get(questionID);
			Answer answer = question.get(username);
			if (answer == null) answer = new Answer("");
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("answer");
			root.setAttribute("value", answer.getValue());
			root.setAttribute("score", Integer.toString(answer.getScore()));
			root.setAttribute("isClosed", Boolean.toString(question.isClosed()));
			res.write(XmlUtil.toString(root));
			res.send();
			return;
		}
		//Not allowed
		res.setResponseCode(res.notfound);
		res.send();
	}

	/**
	 * Handle an answer submission.
	 */
	public void doPost(HttpRequest req, HttpResponse res ) throws Exception {

		logger.info("POST:\n"+req.toString());
		res.setContentType("xml");

		//Require authentication
		if (req.isFromAuthenticatedUser()) {
			String username = req.getUser().getUsername();
			logger.info("username = "+username);
			Path path = req.getParsedPath();

			ScoredQuizDB db = ScoredQuizDB.getInstance();
			String questionID = path.element(1);
			logger.info("questionID = "+questionID);
			Question question = db.get(questionID);
			if (!question.isClosed()) {
				logger.info("question is not closed");
				String value = req.getParameter("value");
				logger.info("value = "+value);
				question.put(username, new Answer(value));
				db.put(question);
				res.write("<OK/>");
				res.send();
				return;
			}
		}
		res.write("<NOTOK/>");
		res.send();
	}

}
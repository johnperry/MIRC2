/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.comment;

import java.io.File;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.prefs.Preferences;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * A servlet to return a summary of documents produced by authors.
 */
public class CommentService extends Servlet {

	static final Logger logger = Logger.getLogger(CommentService.class);

	/**
	 * Construct a CommentService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public CommentService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * <p>
	 * Return content depending on the parameters, which are the same as
	 * the query parameters specified for a GET.
	 */
	public void doPost( HttpRequest req, HttpResponse res ) throws Exception {

		logger.info(req.toString());

		Path path = req.getParsedPath();
		if (req.userHasRole("author")) {

			try {
				String command = path.element(1);
				String ssid = path.element(2);
				Index index = Index.getInstance(ssid);
				String docPath = path.subpath(4).substring(1);
				File docFile = new File( index.getDocumentsDir(), docPath );
				Document doc = XmlUtil.getDocument(docFile);

				String username = req.getUser().getUsername();
				Element pref = Preferences.getInstance().get(username, true);
				String personName = pref.getAttribute("name");
				String datetime = StringUtil.getDateTime(" at ");

				if (command.equals("newthread")) {
					String id = req.getParameter("threadblockID", "").trim();
					String title = req.getParameter("threadtitle", "").trim();
					Element el = getTarget(doc, "threadblock", id);

					logger.info("newthread:");
					logger.info("... id: "+id);
					logger.info("... title: "+title);
					if (el == null) logger.info("... could not find target");
					else logger.info("... found target");

					if (el != null) {
						Element thread = doc.createElement("thread");
						thread.setAttribute("id", StringUtil.makeNameFromDate());
						thread.setAttribute("username", username);
						thread.setAttribute("name", personName);
						thread.setAttribute("date", datetime);
						thread.setAttribute("title", title);
						el.appendChild(thread);
						save(docFile, doc, index);
					}
				}

				else if (command.equals("newpost")) {
					String id = req.getParameter("threadID", "").trim();
					String text = req.getParameter("posttext", "").trim();

					logger.info("newpost:");
					logger.info("... id: "+id);
					logger.info("... text: "+text);

					if (!text.equals("")) {
						Element el = getTarget(doc, "thread", id);

						if (el == null) logger.info("... could not find target");
						else logger.info("... found target");

						if (el != null) {
							Element post = doc.createElement("post");
							post.setAttribute("username", username);
							post.setAttribute("name", personName);
							post.setAttribute("date", datetime);
							post.appendChild( doc.createTextNode(text) );
							el.appendChild(post);
							save(docFile, doc, index);
						}
					}
				}
			}
			catch (Exception ignore) { logger.warn(ignore); }
		}
		res.redirect("/storage"+path.subpath(2));
	}

	private void save(File file, Document doc, Index index) {
		FileUtil.setText(file, XmlUtil.toString(doc));
		String key = index.getKey( file );
		index.insertDocument( key );
	}

	private Element getTarget(Document doc, String name, String id) {
		Element root = doc.getDocumentElement();
		NodeList nl = root.getElementsByTagName(name);
		for (int i=0; i<nl.getLength(); i++) {
			Element e = (Element)nl.item(i);
			if (e.getAttribute("id").equals(id)) return e;
		}
		return null;
	}

}


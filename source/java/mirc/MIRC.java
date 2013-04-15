/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
import mirc.aauth.*;
import mirc.activity.*;
import mirc.addimg.*;
import mirc.bauth.*;
import mirc.casenav.*;
import mirc.comment.*;
import mirc.confs.*;
import mirc.delete.*;
import mirc.download.*;
import mirc.files.*;
import mirc.fsadmin.*;
import mirc.login.*;
import mirc.myrsna.*;
import mirc.prefs.*;
import mirc.presentation.*;
import mirc.publish.*;
import mirc.qsadmin.*;
import mirc.query.*;
import mirc.quiz.*;
import mirc.radlex.*;
import mirc.reset.*;
import mirc.revert.*;
import mirc.sort.*;
import mirc.ssadmin.*;
import mirc.storage.*;
import mirc.submit.*;
import mirc.summary.*;
import mirc.util.*;
import mirc.users.*;
import mirc.zip.*;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.plugin.AbstractPlugin;
import org.rsna.server.ServletSelector;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.server.UsersXmlFileImpl;
import org.rsna.util.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The class that represents a single MIRC site plugin.
 */
public class MIRC extends AbstractPlugin {

	static final Logger logger = Logger.getLogger(MIRC.class);

	File configFile = null;

	/**
	 * Construct a plugin implementing a MIRC site.
	 * @param element the XML element from the configuration file
	 * specifying the configuration of the plugin.
	 */
	public MIRC(Element element) {
		super(element);

		//Install the config file if necessary.
		configFile = new File(root, "mirc.xml");
		FileUtil.getFile(configFile, "/mirc/mirc.xml");

		logger.info("MIRC Plugin instantiated");
	}

	/**
	 * Start the plugin.
	 */
	public void start() {
		//Load the MIRC configuration.
		//Note: this must be done here, rather than in the constructor
		//because the CTP Configuration is not yet instantiated when
		//the constructor is called, and MircConfig calls the CTP
		//configuration to obtain the server IP address and port.
		MircConfig mc = MircConfig.load(configFile);

		//Load the Preferences
		Preferences prefs = Preferences.load( root );

		//Load the DownloadDB
		DownloadDB.load( root );

		//Load the ScoredQuizDB
		ScoredQuizDB.load( root );

		//Load the ActivityDB
		ActivityDB.load( root );

		//Install the servlets
		Configuration config = Configuration.getInstance();
		ServletSelector selector = config.getServer().getServletSelector();
		selector.addServlet("users", MircUserManagerServlet.class);
		selector.addServlet("mirc", MircServlet.class);
		selector.addServlet("query", QueryService.class);
		selector.addServlet("qsadmin", QueryServiceAdmin.class);
		selector.addServlet("casenav", CaseNavigatorService.class);
		selector.addServlet("confs", ConferenceService.class);
		selector.addServlet("delete", DeleteService.class);
		selector.addServlet("files", FileService.class);
		selector.addServlet("fsadmin", FileServiceAdmin.class);
		selector.addServlet("challenge", ChallengeServlet.class);
		selector.addServlet("radlex", RadLexSuggest.class);
		selector.addServlet("reset", ResetService.class);
		selector.addServlet("revert", RevertService.class);
		selector.addServlet("sort", SortImagesService.class);
		selector.addServlet("storage", StorageService.class);
		selector.addServlet("ssadmin", StorageServiceAdmin.class);
		selector.addServlet("submit", SubmitService.class);
		selector.addServlet("summary", AuthorSummary.class);
		selector.addServlet("activity", ActivityReport.class);
		selector.addServlet("prefs", PreferencesServlet.class);
		selector.addServlet("zip", ZipService.class);
		selector.addServlet("bauth", BasicAuthorService.class);
		selector.addServlet("aauth", AuthorService.class);
		selector.addServlet("addimg", AddImageService.class);
		selector.addServlet("publish", PublishService.class);
		selector.addServlet("download", DownloadServlet.class);
		selector.addServlet("comment", CommentService.class);
		selector.addServlet("myrsna", MyRSNAServlet.class);
		selector.addServlet("quiz", QuizServlet.class);
		selector.addServlet("quizmgr", QuizManagerServlet.class);
		selector.addServlet("quizsummary", QuizSummaryServlet.class);
		selector.addServlet("quizanswers", QuizAnswerSummaryServlet.class);
		selector.addServlet("presentation", PresentationService.class);

		//Install the standard roles
		Users users = Users.getInstance();
		users.addRole("publisher");
		users.addRole("author");
		users.addRole("update");
		users.addRole("department");

		//Make sure the admin has the MIRC roles
		User admin = users.getUser("admin");
		if (admin != null) {
			admin.addRole("author");
			admin.addRole("publisher");
			admin.addRole("department");

			//Set a person name for the admin user
			//if it doesn't already have one.
			Element pref = prefs.get("admin", true);
			if (pref == null) {
				prefs.setAuthorInfo("admin", "Administrator", "", "");
			}
			else {
				String name = pref.getAttribute("name");
				if (name.equals("")) {
					prefs.setAuthorInfo("admin",
										"Administrator",
										pref.getAttribute("affiliation"),
										pref.getAttribute("contact"));
				}
			}
		}

		logger.info("MIRC Plugin started");

		//Install the defined roles
		mc.setDefinedRoles();

		//Install the redirector
		installRedirector();

		//Load the RadLex index
		RadLexIndex.loadIndex(root);

		//Start the LibraryMonitor
		new LibraryMonitor().start();

		//Start the DraftDocumentMonitors
		Set<String> ssids = mc.getLocalLibraryIDs();
		for (String ssid : ssids) {
			Element lib = mc.getLocalLibrary(ssid);
			if (lib != null) {
				int timeout = StringUtil.getInt(lib.getAttribute("timeout"));
				if (timeout > 0) new DraftDocumentMonitor(ssid, timeout).start();
			}
		}

		//Start the Activity Summary Report Submitter
		new SummarySubmitter().start();

	}

	/**
	 * Stop the plugin.
	 */
	public void shutdown() {
		Index.closeAll();
		RadLexIndex.close();
		Preferences.close();
		DownloadDB.close();
		ActivityDB.close();
		stop = true;
		logger.info("MIRC Plugin stopped");
	}

	//Copy the redirector into the root of the server
	private void installRedirector() {
		FileOutputStream out = null;
		InputStream in = null;
		try {
			File serverRoot = Configuration.getInstance().getServer().getServletSelector().getRoot();
			File indexHTML = new File(serverRoot, "index.html");
			out = new FileOutputStream(indexHTML);
			String redirector = "/mirc/redirector.html";
			in = getClass().getResourceAsStream(redirector);
			FileUtil.copy(in, out, -1);
		}
		catch (Exception ignore) { }
		FileUtil.close(in);
		FileUtil.close(out);
	}

	/**
	 * Get HTML text displaying the current status of the plugin.
	 * @return HTML text displaying the current status of the plugin.
	 */
	public String getStatusHTML() {
		return getStatusHTML("");
	}
}
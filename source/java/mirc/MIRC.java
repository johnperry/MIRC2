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
import mirc.aauth.*;
import mirc.addimg.*;
import mirc.bauth.*;
import mirc.casenav.*;
import mirc.confs.*;
import mirc.download.*;
import mirc.files.*;
import mirc.fsadmin.*;
import mirc.login.*;
import mirc.prefs.*;
import mirc.publish.*;
import mirc.qsadmin.*;
import mirc.query.*;
import mirc.sort.*;
import mirc.ssadmin.*;
import mirc.storage.*;
import mirc.submit.*;
import mirc.summary.*;
import mirc.util.*;
import mirc.zip.*;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.plugin.AbstractPlugin;
import org.rsna.server.ServletSelector;
import org.rsna.server.User;
import org.rsna.server.Users;
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
		MircConfig.load(configFile);

		//Load the RadLex index
		RadLexIndex.loadIndex(root);

		//Load the Preferences
		Preferences.load( root );

		//Load the DownloadDB
		DownloadDB.load( root );

		//Install the servlets
		Configuration config = Configuration.getInstance();
		ServletSelector selector = config.getServer().getServletSelector();
		selector.addServlet("mirc", MircServlet.class);
		selector.addServlet("query", QueryService.class);
		selector.addServlet("qsadmin", QueryServiceAdmin.class);
		selector.addServlet("casenav", CaseNavigatorService.class);
		selector.addServlet("confs", ConferenceService.class);
		selector.addServlet("files", FileService.class);
		selector.addServlet("fsadmin", FileServiceAdmin.class);
		selector.addServlet("challenge", ChallengeServlet.class);
		selector.addServlet("sort", SortImagesService.class);
		selector.addServlet("storage", StorageService.class);
		selector.addServlet("ssadmin", StorageServiceAdmin.class);
		selector.addServlet("submit", SubmitService.class);
		selector.addServlet("summary", AuthorSummary.class);
		selector.addServlet("prefs", PreferencesServlet.class);
		selector.addServlet("zip", ZipService.class);
		selector.addServlet("bauth", BasicAuthorService.class);
		selector.addServlet("aauth", AuthorService.class);
		selector.addServlet("addimg", AddImageService.class);
		selector.addServlet("publish", PublishService.class);
		selector.addServlet("download", DownloadServlet.class);

		//Install the standard roles
		Users users = Users.getInstance();
		users.addRole("publisher");
		users.addRole("author");
		users.addRole("update");

		//Make sure the admin has the MIRC roles
		User admin = users.getUser("admin");
		if (admin != null) {
			admin.addRole("author");
			admin.addRole("publisher");
		}

		//Install the defined roles
		MircConfig.getInstance().setDefinedRoles();

		//Install the redirector
		installRedirector();

		logger.info("MIRC Plugin started");
	}

	/**
	 * Stop the plugin.
	 */
	public void shutdown() {
		Index.closeAll();
		RadLexIndex.close();
		Preferences.close();
		DownloadDB.close();
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
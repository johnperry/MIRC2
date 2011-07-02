/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.zip;

import java.io.File;

import mirc.MircConfig;
import mirc.prefs.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedList;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.multipart.UploadedFile;

import org.apache.log4j.Logger;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.rsna.server.Users;

/**
 * The Zip Service accepts multipart/form-data submissions of
 * zip files containing trees of files. It walks the tree,
 * creating MIRCdocuments from the files in each leaf directory.
 */
public class ZipService extends Servlet {

	static final Logger logger = Logger.getLogger(ZipService.class);

	/**
	 * Construct a ZipService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public ZipService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * The servlet method that responds to an HTTP GET.
	 * It returns a web page containing a submission form
	 * to the user in the response text.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Allow a third-party to request a challenge if the user is not authenticated.
		if (!req.isFromAuthenticatedUser() && req.hasParameter("challenge")) {
			res.setResponseCode( res.unauthorized );
			res.setHeader("WWW-Authenticate", "Basic realm=\"MIRC\"");
			res.send();
			return;
		}

		//Only accept connections from authors
		if (!req.userHasRole("author")) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		String ssid = path.element(1);
		if (ssid.startsWith("ss")) {

			//Make sure the zip service is enabled
			MircConfig mc = MircConfig.getInstance();
			Element lib = mc.getLocalLibrary(ssid);
			boolean enabled = ((lib != null) && lib.getAttribute("zipenb").equals("yes"));
			if (enabled) {

				//Get the user and his preferences
				String username = req.getUser().getUsername();
				Element prefs = Preferences.getInstance().get( username, true );

				//Generate the submission page.
				String[] params = {
					"ssid",			ssid,
					"name",			prefs.getAttribute("name"),
					"affiliation",	prefs.getAttribute("affiliation"),
					"contact",		prefs.getAttribute("contact"),
					"username",		username,
					"read",			"",
					"update",		"",
					"export",		"",
					"textext",		".txt",
					"skipext",		".dba",
					"skipprefix",	"__",
					"result",		""
				};
				Document xsl = getDocument("ZipService.xsl");
				res.write( XmlUtil.getTransformedText( mc.getXML(), xsl, params ) );
				res.setContentType("html");
				res.send();
			}
			else res.redirect("/query");
		}
		else super.doGet(req, res);
	}

	private Document getDocument(String name) throws Exception {
		File zip = new File(root, "zip");
		File file = new File(zip, name);
		InputStream in = FileUtil.getStream(file, "/zip/"+name);
		return XmlUtil.getDocument(in);
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * It receives a zip file submission and starts a thread to process it.
	 * It uses the content type to determine how to receive and process the submission.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		String ct = req.getContentType().toLowerCase();

		//If the user is not authenticated, return a 401 to trigger a
		//resubmission with credentials. This is for applications like
		//FileSender or the third-party authoring tools.
		if (!req.isFromAuthenticatedUser()) {
			int length = req.getContentLength();
			if (length > 0) FileUtil.discard( req.getInputStream(), length);
			res.setResponseCode( res.unauthorized );
			res.setHeader("WWW-Authenticate", "Basic realm=\"MIRC\"");
			res.send();
			return;
		}

		//Make sure we should process this request.
		Path path = req.getParsedPath();
		String ssid = path.element(1);
		MircConfig mc = MircConfig.getInstance();
		Element lib = mc.getLocalLibrary(ssid);

		boolean enabled = ((lib != null) && lib.getAttribute("zipenb").equals("yes"));
		boolean contentTypeOK = ct.contains("multipart/form-data") || ct.contains("application/x-zip-compressed");
		boolean userIsAuthor = req.userHasRole("author");
		boolean ok = enabled && contentTypeOK && userIsAuthor;

		//Redirect to the query page if anything is wrong.
		if (!ok) {
			int length = req.getContentLength();
			if (length > 0) FileUtil.discard( req.getInputStream(), length);
			res.redirect("/query");
			return;
		}

		//Make a temporary directory to receive the zip file
		File dir = MircConfig.getInstance().createTempDirectory();

		//Get the posted file
		File file = null;
		if (ct.contains("multipart/form-data")) {
			//This is a post from a web page
			int maxsize = StringUtil.getInt( lib.getAttribute("maxsize"), 0 );
			if (maxsize == 0) maxsize = 75;
			maxsize *= 1024*1024; //make it megabytes
			LinkedList<UploadedFile> files = req.getParts(dir, maxsize);
			if (files.size() > 0) file = files.getFirst().getFile();
		}
		else {
			//This is a post from a third party client-side author tool
			File x = new File(dir, "submission.zip");
			InputStream is = req.getInputStream();
			FileOutputStream fos = new FileOutputStream(x);
			if (FileUtil.copy( is, fos, req.getContentLength() )) file = x;
		}

		//The result message
		String result = "";

		String name					= req.getParameter( "name", "" );
		String affiliation			= req.getParameter( "affiliation", "" );
		String contact				= req.getParameter( "contact", "" );
		String username				= req.getParameter( "username", "" );
		String read					= req.getParameter( "read", "" );
		String update				= req.getParameter( "update", "" );
		String export				= req.getParameter( "export", "" );
		String textext				= req.getParameter( "textext", "" );
		String skipext				= req.getParameter( "skipext", "" );
		String skipprefix			= req.getParameter( "skipprefix", "" );
		String otString				= req.getParameter( "otString", "" );
		boolean overwriteTemplate	= req.getParameter( "overwrite", "").equals("overwrite");
		String anString				= req.getParameter( "anString", "" );
		boolean anonymize			= req.getParameter( "anonymize", "").equals("anonymize");;

		if (file == null) result += "It appears that no file was posted.|";

		else {
			//Create a thread to process the file and kick it off.
			//This thread will unpack the file (which must be a zip file)
			//and create MIRCdocuments for the files it contains.
			boolean autoindex = lib.getAttribute("zipautoindex").equals("yes");
			boolean publish = autoindex || req.userHasRole("publisher");
			try {
				File zip = new File(root, "zip");
				File template = new File(zip, "template.xml");
				FileUtil.getFile( template, "/zip/template.xml" );
				ZipThread zipThread =
					new ZipThread(
							ssid,
							file,
							template,
							name,
							affiliation,
							contact,
							publish,
							username,
							read,
							update,
							export,
							textext,
							skipext,
							skipprefix,
							overwriteTemplate,
							anonymize);
				zipThread.start();
				result += "The file was received and queued for processing.";
			}
			catch (Exception ex) {
				logger.warn("Exception while creating the ZipThread.",ex);
				result += "Unable to create the processing thread for the submission.|";
				FileUtil.deleteAll(dir);
			}
		}

		//If the content type was multipart/form-data, generate the submission + results page.
		Element prefs = Preferences.getInstance().get( username, true );
		if (ct.contains("multipart/form-data")) {
			Object[] params = {
				"ssid",			ssid,
				"name",			name,
				"affiliation",	affiliation,
				"contact",		contact,
				"username",		username,
				"read",			read,
				"update",		update,
				"export",		export,
				"textext",		textext,
				"skipext",		skipext,
				"skipprefix",	skipprefix,
				"result",		result
			};
			Document xsl = getDocument("ZipService.xsl");
			res.write( XmlUtil.getTransformedText( mc.getXML(), xsl, params ) );
			res.setContentType("html");
		}
		else { res.setContentType("txt"); res.write(result); }
		res.send();
	}

}
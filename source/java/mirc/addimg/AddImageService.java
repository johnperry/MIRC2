/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.addimg;

import java.io.File;
import java.util.LinkedList;
import java.util.Properties;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.util.MircDocument;
import mirc.util.MircImage;

import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.server.User;
import org.rsna.multipart.UploadedFile;
import org.rsna.servlets.Servlet;

import org.apache.log4j.Logger;

import org.rsna.ctp.pipeline.PipelineStage;
import org.rsna.ctp.stdstages.DicomAnonymizer;
import org.rsna.ctp.stdstages.ScriptableDicom;
import org.rsna.ctp.Configuration;
import org.rsna.ctp.objects.DicomObject;

import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.IntegerTable;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class to add images to a MIRCdocument..
 * This service accepts multipart/form-data submissions
 * containing files for insertion in MIRCdocuments.
 */
public class AddImageService extends Servlet {

	static final Logger logger = Logger.getLogger(AddImageService.class);

	static final String[] textExtensions = { ".txt" };

	/**
	 * Construct an AddImageService servlet.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public AddImageService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * This class does not accept a GET.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//On a GET, redirect to the query page
		res.redirect("/query");
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * @param req The HttpServletRequest provided by the servlet container.
	 * @param res The HttpServletResponse provided by the servlet container.
	 * @throws ServletException if the servlet cannot handle the request.
	 * @throws IOException if any IO error occurs.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		//Only accept connections from authenticated users
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		Path path = req.getParsedPath();
		String ssid = path.element(1);
		if (ssid.startsWith("ss")) {

			//Make sure the document authorizes updates by this user
			MircConfig mc = MircConfig.getInstance();
			Element lib = mc.getLocalLibrary(ssid);
			boolean enabled = ((lib != null) && lib.getAttribute("authenb").equals("yes"));
			if (enabled) {

				//Get the document
				Index index = Index.getInstance(ssid);
				String docPath = path.subpath(3).substring(1);
				File docFile = new File( index.getDocumentsDir(), docPath );
				MircDocument md = new MircDocument(docFile);

				//See if we can update this document
				if (md.authorizes( "update", req.getUser() )) {

					//Make a temporary directory to receive the files
					File dir = mc.createTempDirectory();

					//Get the posted files
					int maxsize = StringUtil.getInt( lib.getAttribute("maxsize"), 0 );
					if (maxsize == 0) maxsize = 75;
					maxsize *= 1024*1024; //make it megabytes
					LinkedList<UploadedFile> uploadedFiles = req.getParts(dir, maxsize);

					boolean anonymize = req.hasParameter("anonymize");

					//Add them into the document
					for (UploadedFile uploadedFile : uploadedFiles) {
						File file = uploadedFile.getFile();
						md.insertFile(file, true, textExtensions, anonymize); //allow unpacking of zip files
					}
					md.save();

					//Index the document
					index.insertDocument( index.getKey(docFile) );

					//Redirect to the document
					res.redirect("/storage" + path.subpath(1));
					return;

				}
			}
		}
		//If we get here, either the user isn't allowed to modify the document
		//or the POST didn't come from a MIRC site and somebody is hacking us.
		res.redirect("/query");
	}

}
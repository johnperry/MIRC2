/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.submit;

import java.io.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.storage.StorageService;
import mirc.ssadmin.StorageServiceAdmin;

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

/**
 * The MIRC Submit Service.
 * The Submit Service accepts multipart/form-data submissions of
 * zip files containing a MIRCdocument and the files it references
 * in its own directory. This is the interface used by browsers.
 * <p>
 * This Submit Service also accepts application/x-zip-compressed
 * submissions of zip files. This is the interface used by client-side
 * authoring tools.
 */
public class SubmitService extends Servlet {

	static final Logger logger = Logger.getLogger(SubmitService.class);

	/**
	 * Construct a SubmitService.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public SubmitService(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) { }

	/**
	 * Get a web page containing a submission form for the submission.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		if (req.userHasRole("author")) {

			Path path = req.getParsedPath();
			MircConfig mc = MircConfig.getInstance();
			String ssid = path.element(1).trim();
			Element lib = mc.getLocalLibrary(ssid);
			boolean enabled = (lib != null) && lib.getAttribute("subenb").equals("yes");

			if ((path.length() == 2) && enabled) {
					res.disableCaching();
					String ui = req.getParameter("ui", "");
					if (!ui.equals("integrated")) ui = "classic";
					res.write( getPage( ui, ssid, "" ) );
					res.setContentType("html");
					res.send();
			}
			else { super.doGet(req, res); return; }
		}
		else res.redirect("/query");
	}

	private String getPage(String ui, String ssid, String result) throws Exception {
		Document xml = MircConfig.getInstance().getXML();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/submit/SubmitService.xsl" ) );
		Object[] params = new Object[] {
							"ui", ui,
							"ssid", ssid,
							"result", result
						};
		String page = XmlUtil.getTransformedText( xml, xsl, params );
		return page;
	}

	private void finish(HttpResponse res, String ui, String ssid, StringBuffer result, boolean suppress) throws Exception {
		if( suppress ) res.write(result.toString().replaceAll("\\|", "<br/>"));
		else res.write( getPage( ui, ssid, result.toString() ) );
		res.setContentType("html");
		res.send();
	}

	/**
	 * Handle the upload of a MIRCdocument zip file, using the content type
	 * to determine how to receive and process the submission.
	 */
	public void doPost(HttpRequest req, HttpResponse res ) throws Exception {

		String ct = req.getContentType().toLowerCase();

		//If this request is not a multipart form and the user is not authenticated,
		//return a 401 to trigger a resubmission with credentials. This is for
		//applications like FileSender or the third-party authoring tools.
		if (ct.contains("application/x-zip-compressed") && !req.isFromAuthenticatedUser()) {
			int length = req.getContentLength();
			if (length > 0) FileUtil.discard( req.getInputStream(), length);
			res.setResponseCode( res.unauthorized );
			res.setHeader("WWW-Authenticate", "Basic realm=\"MIRC\"");
			res.send();
			return;
		}

		Path path = req.getParsedPath();
		MircConfig mc = MircConfig.getInstance();
		String ssid = path.element(1).trim();
		Element lib = mc.getLocalLibrary(ssid);
		boolean enabled = ((lib != null) && lib.getAttribute("subenb").equals("yes"));
		boolean contentTypeOK = ct.contains("multipart/form-data") || ct.contains("application/x-zip-compressed");

		//Redirect to the query page if anything is wrong.
		if (!req.userHasRole("author") || !contentTypeOK || !enabled) {
			res.redirect("/query");
			return;
		}

		StringBuffer result = new StringBuffer();

		Index index = Index.getInstance(ssid);
		File documentsDir = index.getDocumentsDir();

		//Make a directory to receive the document
		File dir = new File(documentsDir, StringUtil.makeNameFromDate());
		dir.mkdirs();

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

		//Note: It is important not to get the query parameters until
		//after the multipart form has been parsed. The parsing process
		//adds the non-file form parameters to the req.params table,
		//making them available to the req.getParameter(...) methods.

		//Get the docref part of the path. This is the part after the
		//the ssid. That is the key under which documents are indexed.
		//If docref has multiple path elements and starts with the name
		//of the identified library's documents directory ("docs"),
		//it is treated as an update from a third-party author tool;
		//otherwise, it is treated as a new document.
		String docref = path.subpath(2);
		Path docrefPath = new Path(docref);
		if ((docrefPath.length() > 1) && docrefPath.element(0).equals(documentsDir.getName())) {
			docref = docref.substring(1);
		}
		else docref = "";
		boolean isDocumentUpdate = !docref.equals("");
		boolean preserveOwners = req.hasParameter("preserveOwners");
		String ui = req.getParameter("ui", "");
		if (!ui.equals("integrated")) ui = "classic";

		//Get the suppress query string parameter. This parameter can be used
		//by client-side applications to suppress the submission form on
		//results pages. If the parameter is present, the form is suppressed
		//but the result of the submission is still provided.
		boolean suppress = (req.getParameter("suppress") != null) || (req.getParameter("ppt") != null);

		//If we didn't get a file, just go back to the query page.
		if (file == null) { res.redirect("/query"); return; }

		//We have a file; unpack it into the same directory.
		File mainFile = null;
		try { mainFile = unpackZipFile(file); }
		catch (Exception ex) {
			result.append("There was a problem unpacking the posted file.|");
			result.append( deleteResponse( FileUtil.deleteAll(dir) ) );
			finish(res, ui, ssid, result, suppress);
			return;
		}

		//Now see if we have a MIRCdocument
		if (mainFile == null) {
			result.append("The zip file was unpacked successfully, but "
					 +  "it did not contain a MIRCdocument file to index.|"
					 +   deleteResponse(FileUtil.deleteAll(dir)) );
			finish(res, ui, ssid, result, suppress);
			return;
		}
		else {
			//Okay, we have an acceptable submission and it is in
			//the dir directory. The path query parameter, if any, is
			//in pathParam; this is the path to the XML file of the
			//MIRCdocument to be updated.
			file.delete(); //delete the zip file

			if (isDocumentUpdate) {
				//This is an update of an existing MIRCdocument.
				//Remove the old version and put the new one in its place.
				try {
					//First parse the old document and see if the user is authorized to change it.
					File oldFile = new File(documentsDir, docref);
					Document docXML = XmlUtil.getDocument(oldFile);
					boolean canUpdate = StorageService.userIsAuthorizedTo("update", docXML, req);
					if (canUpdate) {
						//Okay, remove the old document.
						StorageServiceAdmin.deleteDocument(ssid, docref);

						//Now rename the new directory to the old name so the URL
						//has a chance of staying the same
						File oldDirFile = oldFile.getParentFile();
						if (dir.renameTo(oldDirFile)) {
							//Okay, everything is ready, change mainFile to point
							//to the new file in the old directory.
							mainFile = new File( oldDirFile, mainFile.getName() );
							result.append("The document has been updated.|");
						}
					}
				}
				catch (Exception processAsANewSubmission) { }
			}

			String docpath = mainFile.getAbsolutePath();
			docpath = docpath.substring( docpath.indexOf( documentsDir.getName() ) ).replace('\\', '/');
			result.append("The zip file was received and unpacked successfully:|");
			result.append("@/storage/" + ssid + "/" + docpath + "|");

			String username = req.getUser().getUsername();
			boolean isAutoindex = lib.getAttribute("autoindex").equals("yes");
			boolean isPublisher = req.userHasRole("publisher");

			setAuthorization(mainFile, username, isAutoindex, isPublisher, (isDocumentUpdate || preserveOwners));

			//Now index the document.
			if (isAutoindex || isPublisher) {
				if (index.insertDocument(docpath))
					result.append("The site index has been updated.|");
				else
					result.append("The attempt to update the site index failed.|");
			}
			else {
				result.append("The site index was not updated.|");
				/* **************************************************************************************************************
				if (ApprovalQueue.addEntry(docpath, docXML))
					result.append("The document has been added to the approval queue.|");
				else
					result.append("The attempt to update the approval queue failed.|");
				*/ //************************************************************************************************************
			}
		}
		finish(res, ui, ssid, result, suppress);
	}

	private void setAuthorization(
						File docFile,
						String owner,
						boolean isAutoindex,
						boolean isPublisher,
						boolean preserveOwners) {
		Document doc;
		try { doc = XmlUtil.getDocument(docFile); }
		catch (Exception ex) { return; }

		//Remove the filename and path attributes of the root element
		Element root = doc.getDocumentElement();
		root.removeAttribute("filename");
		root.removeAttribute("path");

		Element authElement = XmlUtil.getFirstNamedChild(doc, "authorization");
		if (authElement == null) {
			//The document does not have an authorization element; insert it.
			authElement = (Element)root.appendChild(doc.createElement("authorization"));
		}
		//Remove any existing owner element and add one containing only
		//the username of the submitting user. NOTE: this is ONLY done if
		//the submission is NOT an update to an existing document.
		if (!preserveOwners) {
			Element ownerElement = XmlUtil.getFirstNamedChild(authElement, "owner");
			if (ownerElement != null) authElement.removeChild(ownerElement);
			ownerElement = doc.createElement("owner");
			ownerElement.appendChild(doc.createTextNode(owner));
			authElement.appendChild(ownerElement);
		}
		//Set up the read element, if necessary.
		//The rules are:
		//  If autoindexing is enabled, the document is accepted.
		//  If the user has the publisher role, it is accepted.
		//  If the document is private or restricted, it is accepted.
		//  Otherwise, the document is made non-public.
		Element readElement = XmlUtil.getFirstNamedChild(authElement, "read");
		if (readElement == null) {
			//The element is missing, add an empty one so that the document will be private by default.
			readElement = (Element)authElement.appendChild(doc.createElement("read"));
		}
		else {
			String read = readElement.getTextContent();
			boolean isPublic = read.contains("*");
			if (!isAutoindex && !isPublisher && isPublic) {
				read = read.replaceAll("\\s+","").replace("*",",").replaceAll("[,]+",",");
				if (read.startsWith(",")) read = read.substring(1);
				if (read.endsWith(",")) read = read.substring(0,read.length()-1);
				while (readElement.hasChildNodes()) readElement.removeChild(readElement.getFirstChild());
				readElement.appendChild(doc.createTextNode(read));
			}
		}
		//Finally, save the document to the original file.
		FileUtil.setText(docFile, XmlUtil.toString(doc));
	}

	//Produce a string for the submission result page indicating whether the
	//submission could be deleted after processing.
	private String deleteResponse(boolean b) {
		if (b) return "The submission was deleted.|";
		return "There was a problem deleting the submission.|";
	}

	//Unpack the submitted zip file into the directory where it is stored,
	//ignoring all path inUIion.
	//
	//Note that some MIRCdocuments from early MIRC versions may be research datasets
	//and may contain "phi" and "no-phi" subdirectories. These subdirectories will
	//be flattened into the main directory, possibly causing unanticipated effects.
	//It may be necessary to alter this code to preserve these subdirectories at some
	//point (but probably not).
	private File unpackZipFile(File file) throws Exception {
		if (!file.exists()) return null;
		if (!file.getName().toLowerCase().endsWith("zip")) return null;
		File parent = file.getParentFile();
		File xmlFile = null;
		ZipFile zipFile = new ZipFile(file);
		Enumeration zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry entry = (ZipEntry)zipEntries.nextElement();
			if (!entry.isDirectory()) {
				//Eliminate the path inUIion.
				String name = entry.getName().replace("\\", "/");
				name = name.substring(name.lastIndexOf("/")+1);
				//Store the entry;
				File outFile = new File(parent, name);
				FileUtil.copy( zipFile.getInputStream(entry), new FileOutputStream(outFile), -1 );
				//Capture the MIRCdocument xml file
				if (name.toLowerCase().endsWith(".xml")) {
					try {
						Document doc = XmlUtil.getDocument(outFile);
						Element root = doc.getDocumentElement();
						if (root.getTagName().equals("MIRCdocument")) {
							xmlFile = checkFilename(outFile);
						}
					}
					catch (Exception doesNotParse) {
						outFile.delete();
					}
				}
			}
		}
		zipFile.close();
		return xmlFile;
	}

	//Check a filename to see if it contains characters that would
	//cause problems when they appear in the docref attribute and
	//rename the file to an acceptable name if necessary.
	//Test for asterisks, apostrophes, quotes and angle brackets.
	private File checkFilename(File file) {
		String name = file.getName();
		String newName = name.trim().replaceAll("[\\s]+","_").replaceAll("[\"&'><#;:@/?=]","_");
		if (newName.equals(name)) return file;
		File newFile = new File(file.getParentFile(), newName);
		file.renameTo(newFile);
		return newFile;
	}

}
/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.files;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import org.rsna.servlets.Servlet;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.multipart.UploadedFile;

import org.rsna.ctp.Configuration;
import org.rsna.ctp.pipeline.PipelineStage;
import org.rsna.ctp.stdstages.DicomAnonymizer;
import org.rsna.ctp.stdstages.ScriptableDicom;

import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.IntegerTable;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;

import org.rsna.ctp.objects.DicomObject;
import mirc.MircConfig;
import mirc.util.*;
import org.rsna.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * The top-level class of the MIRC File Cabinet servlet.
 * The File Cabinet stores and manages files for users.
 * This servlet responds to both HTTP GET and POST.
 */
public class FileService extends Servlet {

	static final Logger logger = Logger.getLogger(FileService.class);

	static File shared;
	static File personal;

	FileFilter dirsOnly = new GeneralFileFilter();
	String username = "";

	/**
	 * Construct a FileService.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public FileService(File root, String context) {
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
	 * been created and is available. This method is only
	 * called once as the server starts.
	 */
	public static void init(File root, String context) {
		File files = new File( MircConfig.getInstance().getRootDirectory(), context );
		shared = new File(files, "Shared");
		personal = new File(files, "Personal");
		shared.mkdirs();
		personal.mkdirs();
	}

	/**
	 * Clean up as the server shuts down.
	 */
	public void destroy() { }

	/**
	 * The servlet method that responds to an HTTP GET and provides
	 * the user interface to the shared and personal file cabinets.
	 * @param req the HttpServletRequest provided by the servlet container.
	 * @param res the HttpServletResponse provided by the servlet container.
	 * @throws Exception if the servlet cannot handle the request.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		//Make sure the user is authorized to do this.
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		//Okay, the user was authenticated, get the name.
		username = req.getUser().getUsername();

		Path path = req.getParsedPath();

		if (path.length() == 1) {
			//This is a GET for the main file cabinet page.
			//Get the HTML file from either the disk or the jar.
			File files = new File(root, context);
			File htmlFile = new File(files, "FileService.html");
			String page = FileUtil.getText( FileUtil.getStream(htmlFile, "/files/FileService.html") );

			//Put in the parameters
			Properties props = new Properties();
			props.setProperty("username", username);
			props.setProperty("userisadmin", (req.userHasRole("admin") ? "true" : "false"));
			String openpath = req.getParameter("openpath", "");
			props.setProperty("openpath", openpath);
			String suppressHome = req.getParameter("suppressHome", "no");
			props.setProperty("suppressHome", suppressHome);
			page = StringUtil.replace(page, props);

			//Send the page
			res.disableCaching();
			res.setContentType("html");
			res.write(page);
			res.send();
			return;
		}

		String function = path.element(1).toLowerCase();
		String subfunction = path.element(2).toLowerCase();

		if (function.equals("tree")) {
			String myrsnaParam = req.getParameter("myrsna", "no");
			boolean includeMyRSNA = !myrsnaParam.equals("no");
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("tree");
			doc.appendChild(root);
			appendDir(root, new File(shared, "Files"), "Shared");
			File userDir = new File(personal, username);
			userDir = new File(userDir, "Files");
			appendDir(root, userDir, "Personal");
			if (includeMyRSNA) appendMyRsnaFiles(root, "MyRSNA");
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString(root) );
			res.send();
			return;
		}

		else if (function.equals("mirc")) {
			//This is a GET for the contents of a single MIRC file cabinet directory.
			Document doc = getDirContentsDoc( new Path( path.subpath(2) ) );
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString( doc.getDocumentElement() ) );
			res.send();
			return;
		}

		else if (function.equals("myrsna") && subfunction.equals("folder/")) {
			//This is a GET for the contents of a single myRSNA folder
			String nodeID = path.subpath(3).substring(1);
			res.disableCaching();
			res.setContentType("xml");
			try  {
				Document doc = getMyRsnaDirContentsDoc(nodeID);
				res.write( XmlUtil.toString( doc.getDocumentElement() ) );
			}
			catch (Exception ex) { res.setResponseCode( res.notfound ); }
			res.send();
			return;
		}

		else if (function.equals("createfolder")) {
			//This a request to create a directory and return the tree of its parent
			Path subpath = new Path( path.subpath(2) );
			FilePath fp = new FilePath(subpath, username, shared, personal);
			fp.filesDir.mkdirs();
			fp.iconsDir.mkdirs();
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("tree");
			doc.appendChild(root);
			File parentDir = fp.filesDir.getParentFile();
			appendDir(root, parentDir, parentDir.getName());
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString(root) );
			res.send();
			return;
		}

		else if (function.equals("deletefolder")) {
			//This a request to delete a directory and return the tree of its parent
			Path subpath = new Path( path.subpath(2) );
			FilePath fp = new FilePath(subpath, username, shared, personal);
			File parentDir = fp.filesDir.getParentFile();
			FileUtil.deleteAll(fp.filesDir);
			FileUtil.deleteAll(fp.iconsDir);
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("tree");
			doc.appendChild(root);
			appendDir(root, parentDir, parentDir.getName());
			res.disableCaching();
			res.setContentType("xml");
			res.write( XmlUtil.toString(root) );
			res.send();
			return;
		}

		else if (function.equals("renamefolder")) {
			//This a request to rename a directory
			Path subpath = new Path( path.subpath(2) );
			FilePath fp = new FilePath(subpath, username, shared, personal);
			String newname = req.getParameter("newname", "x");
			File newFilesDir = new File(fp.filesDir.getParentFile(), newname);
			File newIconsDir = new File(fp.iconsDir.getParentFile(), newname);
			if (newFilesDir.exists() || newIconsDir.exists()) {
				res.write("<notok/>");
			}
			else {
				boolean filesResult = fp.filesDir.renameTo(newFilesDir);
				boolean iconsResult = fp.iconsDir.renameTo(newIconsDir);
				res.write(
					 "<ok>\n"
					+"  <filesResult dir=\""+fp.filesDir+"\">"+filesResult+"</filesResult>\n"
					+"  <iconsResult dir=\""+fp.iconsDir+"\">"+iconsResult+"</iconsResult>\n"
					+"</ok>"
				);
			}
			res.disableCaching();
			res.setContentType("xml");
			res.send();
			return;
		}

		else if (function.equals("deletefiles")) {
			//This a request to delete a list of files from a directory.
			//Note: only a response code is returned; the client will make
			//another call to get the contents.
			Path subpath = new Path( path.subpath(2) );
			FilePath fp = new FilePath(subpath, username, shared, personal);
			res.setResponseCode( deleteFiles( fp, req.getParameter("list"), req.userHasRole("admin") ) );
			res.send();
			return;
		}

		else if (function.equals("renamefile")) {
			//This a request to rename a single file.
			Path subpath = new Path( path.subpath(2) );
			FilePath fp = new FilePath(subpath, username, shared, personal);
			res.setResponseCode(
				renameFile(
					fp, req.getParameter("oldname"), req.getParameter("newname"), req.userHasRole("admin")
				)
			);
			res.disableCaching();
			res.setContentType("xml");
			res.send();
			return;
		}

		else if (function.equals("copyfiles")) {
			//This a request to copy a list of files from one directory to another.
			//As in the case of deleteFiles, the client will make another call to
			//display whatever directory it wants. Unlike deleteFiles, however, in
			//this case the source and destination path information is passed in
			//query parameters, along with the list of files.
			String sourcePath = req.getParameter("sourcePath");
			String destPath = req.getParameter("destPath");
			String files = req.getParameter("files");
			res.setResponseCode( copyFiles( sourcePath, destPath, files) );
			res.disableCaching();
			res.setContentType("xml");
			res.send();
			return;
		}

		else if (function.equals("exportfiles")) {
			//This is a request to export a list of files from a directory as a zip file.
			Path subpath = new Path( path.subpath(2) );
			FilePath fp = new FilePath(subpath, username, shared, personal);

			String list = req.getParameter("list", "");
			if (list.trim().equals("")) res.setResponseCode( res.notfound );
			else {
				String[] docs = list.split("\\|");
				if (docs.length > 0) {
					File outDir = MircConfig.getInstance().createTempDirectory();
					File outFile = new File(outDir, "Export.zip");
					if (FileUtil.zipFiles(docs, fp.filesDir, outFile)) {
						res.setContentType("zip");
						res.setContentDisposition(outFile);
						res.write(outFile);
						res.send();
					}
					else {
						res.setResponseCode( res.notfound );
						res.send();
					}
					FileUtil.deleteAll(outDir);
					return;
				}
				else res.setResponseCode( res.notfound );
			}
			res.send();
			return;
		}

		else if (function.equals("save")) {
			//This is a request from a MIRCdocument to save its images.
			//Put them in a subdirectory of the user's Personal/Files directory.
			String docpath = path.subpath(2);
			File doc = new File(root, docpath);
			File docdir = doc.getParentFile();

			//Okay, here is a kludge to try to avoid saving MIRCdocuments
			//on top of one another. If a MIRCdocument was created by the
			//ZipService, it will be in a directory called {datetime}/n.
			//If we just get the parent of the file in the path, then we just get n.
			//Let's try to do a little better. If the parent has a short name and
			//the grandparent has a long name (for example, "200901011234132/1"),
			//then get the grandparent and parent together.
			String parentName = docdir.getName();
			String grandparentName = docdir.getParentFile().getName();
			if ((parentName.length() < 17) && (grandparentName.length() == 17)) {
				//Note: datetime names are ALWAYS 17 chars.
				parentName = grandparentName + "/" + parentName;
			}
			String targetDir = "Personal/"+parentName;
			Path targetDirPath = new Path(targetDir);
			FilePath fp = new FilePath(targetDirPath, username, shared, personal);
			saveImages(docpath, fp);
			res.redirect("/"+context+"?openpath="+targetDir);
			return;
		}

		else if (function.equals("personal") && subfunction.equals("files")) {
			String filePath = path.subpath(2).substring(1);
			File file = new File( new File(personal, username), filePath );
			returnFile(req, res, file);
			return;
		}

		else if (function.equals("personal") && subfunction.equals("icons")) {
			String filePath = path.subpath(2).substring(1);
			File file = new File( new File(personal, username), filePath );
			res.write(file);
			res.setContentType(file);
			res.send();
			return;
		}

		else if (function.equals("shared") && subfunction.equals("files")) {
			String filePath = path.subpath(2).substring(1);
			File file = new File( shared, filePath );
			returnFile(req, res, file);
			return;
		}

		else if (function.equals("shared") && subfunction.equals("icons")) {
			String filePath = path.subpath(2).substring(1);
			File file = new File( shared, filePath );
			res.write(file);
			res.setContentType(file);
			res.send();
			return;
		}

		//This must be a regular file request; handle it in the superclass.
		super.doGet(req, res);
	}

	//Get a file and return it as requested
	private void returnFile(HttpRequest req, HttpResponse res, File file) {
		boolean isDicomFile = file.getName().toLowerCase().endsWith(".dcm");
		if (isDicomFile && req.hasParameter("list")) {
			try {
				DicomObject dob = new DicomObject(file);
				res.write( dob.getElementTablePage( req.userHasRole("admin") ) );
				res.setContentType("html");
				res.send();
				return;
			}
			catch (Exception justReturnTheFile) { logger.warn("Unable to create the element listing"); }
		}
		//If we get here, we didn't return a DICOM element listing,
		//either because it isn't a DicomObject, a listing wasn't
		//requested, or the attempt to list the elements failed.
		//In any case, the right thing to do is to return the file.
		res.setContentType(file);
		if (isDicomFile) res.setContentDisposition(file);
		res.write(file);
		res.send();
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 * @throws ServletException if the servlet cannot handle the request.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		//Make sure the user is authorized to do this.
		if (!req.isFromAuthenticatedUser()) { res.redirect("/query"); return; }

		//Okay, the user was authenticated, get the name.
		username = req.getUser().getUsername();

		Path path = req.getParsedPath();
		String function = path.element(1).toLowerCase();

		//Figure out what kind of POST this is.
		String contentType = req.getContentType().toLowerCase();
		if (contentType.contains("multipart/form-data")) {
			if (function.equals("uploadfile")) {

				//This a request to upload a file and return a file cabinet page.
				String dirPath = path.subpath(2).substring(1);
				Path subpath = new Path( dirPath );
				FilePath fp = new FilePath(subpath, username, shared, personal);

				uploadFile(req, res, fp);

				res.redirect("/files?openpath="+dirPath);
				return;
			}
		}
		//If we get here, we couldn't service the POST
		res.setResponseCode( res.notimplemented );
		res.send();
	}

	private Document getDirContentsDoc(Path path) throws Exception {
		FilePath fp = new FilePath(path, username, shared, personal);
		File[] files = fp.filesDir.listFiles();
		Document doc = XmlUtil.getDocument();
		Element root = doc.createElement("dir");
		root.setAttribute("title", fp.dirTitle);
		doc.appendChild(root);
		for (int i=0; i<files.length; i++) {
			if (files[i].isFile()) {
				Element el = doc.createElement("file");
				String name = files[i].getName();

				//Figure out whether the icon is a gif or a jpg
				String iconName = "";
				File iconFile = new File(fp.iconsDir, name+".gif");
				if (!iconFile.exists()) iconFile = new File(fp.iconsDir, name+".jpg");

				if (iconFile.exists()) iconName = iconFile.getName();

				el.setAttribute("fileURL", fp.filesURL + "/" + name);
				el.setAttribute("iconURL", fp.iconsURL + "/" + iconName);
				el.setAttribute("title", name);
				root.appendChild(el);
			}
		}
		return doc;
	}

	//Add a category node to a tree
	private Element appendCategory(Node parent, String title) throws Exception {
		Element el = parent.getOwnerDocument().createElement("node");
		el.setAttribute("name", title);
		parent.appendChild(el);
		return el;
	}

	//Add a directory and its child directories to a tree
	private void appendDir(Node parent, File dir, String title) throws Exception {
		Element el = parent.getOwnerDocument().createElement("node");
		el.setAttribute("name", title);
		el.setAttribute("sclickHandler", "showFileDirContents");
		parent.appendChild(el);
		dir.mkdirs();
		File[] files = dir.listFiles(dirsOnly);
		for (int i=0; i<files.length; i++) {
			appendDir(el, files[i], files[i].getName());
		}
	}

	//Add the user's MyRSNA folders to a tree
	private void appendMyRsnaFiles(Element parent, String title) {
		MyRsnaSession mrs = MyRsnaSessions.getInstance().getMyRsnaSession(username);
		if (mrs != null) {
			try {
				Element folders = mrs.getMyRSNAFolders();
				Element el = parent.getOwnerDocument().createElement("node");
				el.setAttribute("name", title);
				parent.appendChild(el);
				appendMyRsnaFolderDir(el, folders);
			}
			catch (Exception skipMyRsna) { }
		}
	}

	//Walk a tree of MyRsna folders
	private void appendMyRsnaFolderDir(Node parent, Node folders) {
		if (folders == null) return;
		Node child = folders.getFirstChild();
		while (child != null) {
			if ((child.getNodeType() == Node.ELEMENT_NODE) && child.getNodeName().equals("folder")) {
				Element folder = (Element)child;
				Node name = getFirstNamedChild(folder, "name");
				if (name != null) {
					Element el = parent.getOwnerDocument().createElement("node");
					el.setAttribute("name", name.getTextContent().trim());
					el.setAttribute("nodeID", folder.getAttribute("id"));
					el.setAttribute("sclickHandler", "showMyRsnaDirContents");
					parent.appendChild(el);
					Node subfolders = getFirstNamedChild(folder, "folders");
					if (subfolders != null) appendMyRsnaFolderDir(el, subfolders);
				}
			}
			child = child.getNextSibling();
		}
	}

	//Get the contents of a myRSNA folder
	private Document getMyRsnaDirContentsDoc(String nodeID) throws Exception {
		//Make the return document
		Document doc = XmlUtil.getDocument();
		Element root = doc.createElement("dir");
		doc.appendChild(root);

		//Get the session
		MyRsnaSession mrs = MyRsnaSessions.getInstance().getMyRsnaSession(username);
		if (mrs != null) {
			try {
				Element result = mrs.getMyRSNAFiles(null);
				//Get the requested folder
				Node folder = findMyRsnaFolder(result, nodeID);
				if (folder != null) {
					//Got it, get the files child
					Node files = getFirstNamedChild(folder, "files");
					if (files != null) {
						//OK, we're there, now put in all the file children
						Node child = files.getFirstChild();
						while (child != null) {
							if ((child.getNodeType() == Node.ELEMENT_NODE) && child.getNodeName().equals("file")) {
								appendMyRsnaFile(root, child);
							}
							child = child.getNextSibling();
						}
					}
				}
			}
			catch (Exception quit) { }
		}
		return doc;
	}

	private void appendMyRsnaFile(Node parent, Node file) {
		Element el = parent.getOwnerDocument().createElement("file");
		Element eFile = (Element)file;
		el.setAttribute("id",      eFile.getAttribute("id"));
		el.setAttribute("title",   getFirstNamedChild(file, "name")     .getTextContent().trim());
		el.setAttribute("fileURL", getFirstNamedChild(file, "original") .getTextContent().trim());
		el.setAttribute("iconURL", getFirstNamedChild(file, "thumbnail").getTextContent().trim());
		parent.appendChild(el);
	}

	private Node findMyRsnaFolder(Node node, String id) {
		if (node == null) return null;
		if ((node.getNodeType() == Node.ELEMENT_NODE)
				&& node.getNodeName().equals("folder")
					&& ((Element)node).getAttribute("id").equals(id)) return node;
		//If we get here, the current node is not the one we want; look at its children
		Node result;
		Node child = node.getFirstChild();
		while (child != null) {
			if ((result=findMyRsnaFolder(child, id)) != null) return result;
			child = child.getNextSibling();
		}
		return null;
	}

	private Node getFirstNamedChild(Node node, String name) {
		Node child = node.getFirstChild();
		while (child != null) {
			if ((child.getNodeType() == Node.ELEMENT_NODE) && child.getNodeName().equals(name)) return child;
			child = child.getNextSibling();
		}
		return null;
	}

	// Delete files from the specified directory.
	private int deleteFiles(FilePath fp, String list, boolean userIsAdmin) {
		//If the delete request is for the shared directory, determine
		//whether the user has the admin role and abort if he doesn't.
		//if (fp.isShared() && !userIsAdmin) return HttpResponse.forbidden;

		//Get the list of document names and verify it.
		//Do everything we can, and ignore errors.
		if ((list == null) || (list.trim().equals(""))) return HttpResponse.notfound;
		String[] docs = list.split("\\|");
		for (int i=0; i<docs.length; i++) {
			docs[i] = docs[i].replace("../","").trim();
			File file = new File(fp.filesDir, docs[i]);
			File jpgIcon = new File(fp.iconsDir, docs[i]+".jpg");
			File gifIcon = new File(fp.iconsDir, docs[i]+".gif");
			file.delete();
			jpgIcon.delete();
			gifIcon.delete();
		}
		return HttpResponse.ok;
	}

	// Rename a file in the specified directory.
	private int renameFile(FilePath fp, String oldName, String newName, boolean userIsAdmin) {
		//If the delete request is for the shared directory, determine
		//whether the user has the admin role and abort if he doesn't.
		if (fp.isShared() && !userIsAdmin) return HttpResponse.forbidden;

		if ((oldName == null) || (oldName.trim().equals(""))) return HttpResponse.notfound;
		if ((newName == null) || (newName.trim().equals(""))) return HttpResponse.notfound;
		newName = fixExtension(newName, oldName);

		File oldFile = new File(fp.filesDir, oldName);
		File oldJpgIcon = new File(fp.iconsDir, oldName+".jpg");
		File oldGifIcon = new File(fp.iconsDir, oldName+".gif");

		File newFile = new File(fp.filesDir, newName);
		File newJpgIcon = new File(fp.iconsDir, newName+".jpg");
		File newGifIcon = new File(fp.iconsDir, newName+".gif");

		oldFile.renameTo(newFile);
		oldJpgIcon.renameTo(newJpgIcon);
		oldGifIcon.renameTo(newGifIcon);
		return HttpResponse.ok;
	}

	private String fixExtension(String newName, String oldName) {
		//Get the extension on the old filename
		String ext = "";
		int k = oldName.lastIndexOf(".");
		if (k != -1) ext = oldName.substring(k);

		//See if the new filename is the same
		if (newName.endsWith(ext)) return newName;

		//It doesn't. Append the extension to the new filename.
		if (newName.endsWith(".")) newName = newName.substring(0, newName.length() - 1);
		return newName + ext;
	}

	// Copy files from one directory to another.
	private int copyFiles(String sourcePath, String destPath, String fileList) throws Exception {
		//Get the path
		if (sourcePath == null) return HttpResponse.notfound;
		if (destPath == null) return HttpResponse.notfound;

		FilePath src = new FilePath( new Path(sourcePath), username, shared, personal );
		if (!src.isShared() && !src.isPersonal()) return HttpResponse.notfound;

		FilePath dest = new FilePath(new Path(destPath), username, shared, personal);
		if (!dest.isShared() && !dest.isPersonal()) return HttpResponse.notfound;

		//Get the list of document names and verify it.
		//Do everything we can, and ignore errors.
		if ((fileList == null) || (fileList.trim().equals(""))) return HttpResponse.notfound;
		String[] files = fileList.split("\\|");
		for (String name : files) {
			name = name.replace("../", "").trim();
			copyFile(src, dest, name);
		}
		return HttpResponse.ok;
	}

	//Copy one file
	private void copyFile(FilePath src, FilePath dest, String name) {
		File destFile = new File(dest.filesDir, name);
		File destJpgIcon = new File(dest.iconsDir, name+".jpg");
		File destGifIcon = new File(dest.iconsDir, name+".gif");
		destFile.delete();
		destJpgIcon.delete();
		destGifIcon.delete();
		File srcFile = new File(src.filesDir, name);
		File srcJpgIcon = new File(src.iconsDir, name+".jpg");
		File srcGifIcon = new File(src.iconsDir, name+".gif");
		FileUtil.copy(srcFile, destFile);
		if (srcJpgIcon.exists()) FileUtil.copy(srcJpgIcon, destJpgIcon);
		if (srcGifIcon.exists()) FileUtil.copy(srcGifIcon, destGifIcon);
	}

	// Get a file from a multipart form, store it, and add it to the specified directory
	private void uploadFile(HttpRequest req, HttpResponse res, FilePath fp) throws Exception {
		MircConfig mc = MircConfig.getInstance();
		Element fileService = mc.getFileService();
		int maxsize;
		try { maxsize = Integer.parseInt( fileService.getAttribute("maxsize") ); }
		catch (Exception ex) { maxsize = 75; }
		maxsize *= 1024 * 1024;
		File dir = MircConfig.getInstance().createTempDirectory();

		//Get the submission
		LinkedList<UploadedFile> files = req.getParts(dir, maxsize);

		boolean anonymize = req.hasParameter("anonymize");

		//Note: the UI only allows one file to be uploaded at a time.
		//We process all the submitted files here, in case the UI changes
		//in the future.

		//Also note: if an uploaded file is a zip file, so it is possible
		//that the submission will result in multiple files being stored,
		//because the zip file will be automatically unpacked by the
		//saveFile method.

		for (UploadedFile file : files) {
			File dataFile = file.getFile();
			saveFile(fp, dataFile, anonymize); //ignore errors
		}
		FileUtil.deleteAll(dir);
	}

	// Put all the images from a MIRCdocument into the file cabinet.
	private void saveImages(String path, FilePath fp) throws Exception {
		//Get a File pointing to the MIRCdocument
		if (path.startsWith("/")) path = path.substring(1);
		path = path.replace('/', File.separatorChar);
		File docFile = new File(root, path);
		File docParent = docFile.getParentFile();
		fp.filesDir.mkdirs();
		fp.iconsDir.mkdirs();
		Document doc = XmlUtil.getDocument(docFile);
		Element rootElement = doc.getDocumentElement();
		NodeList nodeList = rootElement.getElementsByTagName("image");
		for (int i=0; i<nodeList.getLength(); i++) {
			File file = getBestImage(docParent, (Element)nodeList.item(i));
			if ((file != null) && file.exists()) {
				String filename = file.getName();
				File target = new File(fp.filesDir, filename);
				File targetJPG = new File(fp.iconsDir, filename+".jpg");
				File targetGIF = new File(fp.iconsDir, filename+".gif");
				//Delete the target if it exists.
				target.delete();
				targetJPG.delete();
				targetGIF.delete();
				//Now copy the inFile into the target
				if (FileUtil.copy(file, target)) makeIcon(target, fp.iconsDir);
			}
		}
	}

	// Find the best image identified by an image element.
	private File getBestImage(File docParent, Element image) {
		NodeList nodeList = image.getElementsByTagName("alternative-image");
		Element e = getImageForRole(nodeList,"original-format");
		if (e == null) e = getImageForRole(nodeList,"original-dimensions");
		if (e == null) e = image;
		String src = e.getAttribute("src");
		if ((src == null) || src.trim().equals("") || (src.indexOf("/") != -1))
			return null;
		return new File(docParent,src);
	}

	// Find an element with a specific role attribute.
	private Element getImageForRole(NodeList nodeList, String role) {
		for (int i=0; i<nodeList.getLength(); i++) {
			Element e = (Element)nodeList.item(i);
			if ((e != null) && e.getAttribute("role").equals(role))
				return e;
		}
		return null;
	}

	// Make an output File corresponding to an input File, eliminating the
	// appendages put on such files by MIRCdocument generators.
	private File getTargetFile(File targetDir, File file) {
		String name = file.getName();
		int k = name.lastIndexOf(".");
		if (k == -1) return new File(targetDir, name);
		String nameNoExt = name.substring(0,k);
		if (nameNoExt.endsWith("_base") ||
			nameNoExt.endsWith("_icon") ||
			nameNoExt.endsWith("_full")) {
				name = nameNoExt.substring(0, nameNoExt.length()-5) + name.substring(k);
		}
		return new File(targetDir, name);
	}

	//Store a file in the file cabinet.
	// fp: the object identifying the directories to be modified.
	// dataFile: the file to store.
	// filename: the name under which the file is to be stored.
	// anonymize: true if DICOM files are to be anonymized.
	// return true if the operation succeeded completely; false otherwise.
	private boolean saveFile(
			FilePath fp,
			File dataFile,
			boolean anonymize) {
		String filename = dataFile.getName();
		if (filename.toLowerCase().endsWith(".zip")) {
			boolean ok = unzipFile(fp, dataFile, anonymize);
			dataFile.delete();
			return ok;
		}
		else {
			File target = new File(fp.filesDir, filename);
			deleteTarget(fp, target);
			//Now rename the dataFile into the target
			if (dataFile.renameTo(target)) {
				target = handleDicomObject(target, anonymize);
				makeIcon(target, fp.iconsDir);
				return true;
			}
		}
		return false;
	}

	private void deleteTarget(FilePath fp, File target) {
		//Delete the target and its icons.
		String filename = target.getName();
		File targetJPG = new File(fp.iconsDir, filename+".jpg");
		File targetGIF = new File(fp.iconsDir, filename+".gif");
		target.delete();
		targetJPG.delete();
		targetGIF.delete();
	}

	// Unpack a zip submission. Note: this function ignores
	// any path information and puts all files in the
	// directory pointed to by the FilePath object.
	private boolean unzipFile(
			FilePath fp,
			File inZipFile,
			boolean anonymize) {

		if (!inZipFile.exists()) return false;
		try {
			ZipFile zipFile = new ZipFile(inZipFile);
			Enumeration zipEntries = zipFile.entries();
			while (zipEntries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry)zipEntries.nextElement();
				String name = entry.getName().replace('/',File.separatorChar);
				//name = name.substring(name.lastIndexOf(File.separator)+1); //remove the path information
				if (!entry.isDirectory()) {
					File outFile = new File(fp.filesDir, name);
					File iconDir = (new File(fp.iconsDir, name)).getParentFile();
					outFile.getParentFile().mkdirs();
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
					BufferedInputStream in = new BufferedInputStream(zipFile.getInputStream(entry));
					int size = 1024;
					int n;
					byte[] b = new byte[size];
					while ((n = in.read(b,0,size)) != -1) out.write(b,0,n);
					in.close();
					out.close();
					outFile = handleDicomObject(outFile, anonymize);
					makeIcon(outFile, iconDir);
				}
			}
			zipFile.close();
			return true;
		}
		catch (Exception e) { return false; }
	}

	//Check whether a file is a DicomObject, and if so, set its extension
	//and handle anonymization if requested.
	private static File handleDicomObject(File file, boolean anonymize) {
		try {
			DicomObject dob = new DicomObject(file);
			if (anonymize) {
				DicomAnonymizer fsa = MircConfig.getInstance().getFileServiceDicomAnonymizer();
				if (fsa != null) {
					DAScript dascript = DAScript.getInstance(fsa.getScriptFile());
					Properties script = dascript.toProperties();
					Properties lookup = LookupTable.getProperties(fsa.getLookupTableFile());
					IntegerTable intTable = fsa.getIntegerTable();
					DICOMAnonymizer.anonymize(file, file, script, lookup, intTable, false, false);
					dob = new DicomObject(dob.getFile());
					dob.renameToUID();
					file = dob.getFile();
				}
			}
			else file = dob.setStandardExtension();
		}
		catch (Exception ex) { }
		return file;
	}

	/**
	 * Create and store an icon image for a file.
	 * Try to load the file as an image.
	 * If it loads, create a 96-pixel-wide jpeg icon.
	 * If not, determine whether the file has an icon stored
	 * in the common directory, and if so, use it.
	 * If no special icon exists for the file extension,
	 * create one from the default icon.
	 * For non-image files, write the name of the target
	 * near the bottom of the icon.
	 * @param target the file for which to create the icon image.
	 * @param iconsDir the directory in which to store the icon image.
	 * @return the icon image file, or null if the icon image file
	 * could not be created.
	 */
	public static File makeIcon(File target, File iconsDir) {
		iconsDir.mkdirs();
		String name = target.getName();
		int k = name.lastIndexOf(".") + 1;
		String ext = (k>0) ? name.substring(k).toLowerCase() : "";
		try {
			ImageObject image = new ImageObject(target);
			File iconFile = new File(iconsDir, name+".jpg");
			image.saveAsJPEG(iconFile, 0, 96, 0); //(frame 0)
			return iconFile;
		}
		catch (Exception e) {
			//It's not an image.
			//See if there is an icon for this file
			String iconName = ext+".gif";
			ImageObject icon = null;
			try {
				try { icon = new ImageObject("/files/common/"+iconName); }
				catch (Exception ex) { icon = new ImageObject("/files/common/default.gif"); }

				//Now create an icon specifically for this file
				iconName = name+".gif";
				File iconFile = new File(iconsDir, iconName);
				icon.saveAsIconGIF(iconFile, 96, target.getName());
				return iconFile;
			}
			catch (Exception ex) { }
		}
		return null;
	}

	/**
	 * Get a File pointing to a file in a file cabinet.
	 * @param path the path to the file cabinet directory
	 * @param filename the name of the file in the file cabinet.
	 * @param username the name of the user requesting the file.
	 * @return a File pointing to the requested file in the file cabinet.
	 */
	public static File getFile(String path, String filename, String username) {
		Path p = new Path(path);
		FilePath fp = new FilePath(p, username, shared, personal);
		return new File( fp.filesDir, filename );
	}

	/**
	 * Get a File pointing to a file cabinet directory.
	 * @param path the path to the file cabinet directory
	 * @param username the name of the user requesting the file.
	 * @return a File pointing to the requested file in the file cabinet.
	 */
	public static FilePath getFilePath(String path, String username) {
		Path p = new Path(path);
		return new FilePath(p, username, shared, personal);
	}

}

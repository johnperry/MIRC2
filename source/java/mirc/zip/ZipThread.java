/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.zip;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.util.FileUtil;
import mirc.storage.Index;
import mirc.util.MircDocument;
import mirc.util.MircImage;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

/**
 * The Thread that processes files submitted to the Zip Service.
 */
public class ZipThread extends Thread {

	static final Logger logger = Logger.getLogger(ZipThread.class);

	String ssid;
	File submission;
	File template;
	String name;
	String affiliation;
	String contact;
	boolean canPublish;
	String username;
	String read;
	String update;
	String export;
	String textext;
	String skipext;
	String skipprefix;
	String[] textExtensions;
	String[] skipExtensions;
	String[] skipPrefixes;
	boolean overwriteTemplate;
	boolean anonymize;

	File docsDir;
	File baseDir;
	File submissionDir;
	File root;
	Filter dirsOnly;
	Filter filesOnly;
	int docCount = 0;

	Index index;

	/**
	 * Create a new ZipThread.
	 * @param name the author's name.
	 * @param affiliation the author's affiliation.
	 * @param contact the author's contact information.
	 * @param canPublish true if the user can publish the documents; false otherwise.
	 * @param template the default template file to be used unless overridden by the submission.
	 * @param username the username of the owner to be assigned to the document.
	 * @param read the read privileges to be assigned to the document
	 * @param update the update privileges to be assigned to the document
	 * @param export the export privileges to be assigned to the document
	 * @param ssid the id of the storage service in which to create the MIRCdocuments
	 * @param submission the zip file containing the submission.
	 * @param overwriteTemplate true if supplied parameters are
	 * to overwrite the values in the template; false if the template
	 * parameters are not to be overwritten.
	 * @param anonymize true if DicomObjects are to be anonymized.
	 * @throws Exception if the submission could not be unpacked or the default template file is missing.
	 */
	public ZipThread(
				String ssid,
				File submission,
				File template,
				String name,
				String affiliation,
				String contact,
				boolean canPublish,
				String username,
				String read,
				String update,
				String export,
				String textext,
				String skipext,
				String skipprefix,
				boolean overwriteTemplate,
				boolean anonymize) throws Exception {

		this.ssid = ssid;
		this.submission = submission;
		this.template = template;
		this.name = name;
		this.affiliation = affiliation;
		this.contact = contact;
		this.canPublish = canPublish;
		this.username = username;
		this.read = read;
		this.update = update;
		this.export = export;
		this.overwriteTemplate = overwriteTemplate;
		this.anonymize = anonymize;

		textExtensions = textext.replaceAll("\\s","").split(",");
		skipExtensions = skipext.replaceAll("\\s","").split(",");
		skipPrefixes = skipprefix.replaceAll("\\s","").split(",");

		dirsOnly = new Filter(true);
		filesOnly = new Filter(false, skipExtensions);
		if (!template.exists())
			throw new Exception("The Zip Service's default template does not exist.");
		if (!isValidTemplate(template))
			throw new Exception("The Zip Service's default template is not valid.");


		//The submission is in a temp directory with nothing
		//else in it. Create a subdirectory called "root" in
		//that directory and unpack the submission into it.
		submissionDir = submission.getParentFile();
		root = new File(submissionDir, "root");
		root.mkdirs();
		unpackZipFile(root, submission);
		submission.delete();

		//Get a directory in the documents tree in the selected storage service.
		//This directory will be used as the base for all the MIRCdocuments created
		//for this submission. Each MIRCdocument will be in a directory in the form
		//{docsDir}/{baseDir}/{docCount} where {docCount} is an integer that counts
		//from one and increases with each MIRCdocument created.
		index = Index.getInstance(ssid);
		docsDir = index.getDocumentsDir();
		baseDir = new File(docsDir, StringUtil.makeNameFromDate());
	}

	private String[] trim(String[] s) {
		if (s != null) {
			for (int i=0; i<s.length; i++) s[i] = s[i].trim();
		}
		return s;
	}

	public void run() {
		processDirectory(root, template, "", "");
		FileUtil.deleteAll(submissionDir);
	}

	private void processDirectory(File dir, File template, String title, String keywords) {

		//Make sure this directory should be processed
		if (skipDirectory(dir)) return;

		//Make sure the title is legal XML
		title = StringUtil.displayable(title);
		if (title.trim().equals("")) title = "Untitled";

		//If there is a valid template, use it for this directory and
		//the rest of the directories on this branch.
		File newTemplate = new File(dir, "template.xml");
		if (newTemplate.exists() && isValidTemplate(newTemplate))
			template = newTemplate;

		//Make a MIRCdocument out of any other files in this directory.
		File[] files = dir.listFiles(filesOnly);
		Arrays.sort(files);

		if (files.length > 0) createMircDocument(files, template, title, keywords);

		//Process any child directories
		keywords += " " + title;
		files = dir.listFiles(dirsOnly);
		for (int i=0; i<files.length; i++) {
			processDirectory(files[i], template, files[i].getName(), keywords);
		}
	}

	private boolean skipDirectory(File dir) {
		if ((skipPrefixes == null) || (skipPrefixes.length == 0)) return false;
		String name = dir.getName();
		for (int i=0; i<skipPrefixes.length; i++) {
			if (!skipPrefixes[i].equals("") && name.startsWith(skipPrefixes[i])) {
				return true;
			}
		}
		return false;
	}

	private void createMircDocument(File[] files, File template, String title, String keywords) {

		//Don't create MIRCdocuments for empty file lists.
		if (files.length == 0) return;

		//Create a subdirectory in the storage service's documents tree for this MIRCdocument.
		docCount++;
		File mdDir = new File( baseDir, Integer.toString(docCount) );
		mdDir.mkdirs();

		//Make the File that points to the MIRCdocument.xml file to be created.
		File mdFile = new File(mdDir, "MIRCdocument.xml");

		//While we're at it, create the index entry.
		String indexEntry = index.getKey(mdFile);

		//Copy the template into the directory.
		FileUtil.copy(template, mdFile);

		//Instantiate the MircDocument so we can add objects into it.
		MircDocument md;
		try { md = new MircDocument(mdFile); }
		catch (Exception crash) { return; }

		//Set the title, author, abstract, and keywords.
		//Use the keywords as the abstract since there is
		//no way to get a real abstract.
		md.insert(
			title,
			name,
			affiliation,
			contact,
			keywords,
			keywords,
			username,
			read,
			update,
			export,
			overwriteTemplate);

		//Now add in all the files.
		for (int i=0; i<files.length; i++) {
			//This file is allowed in the MIRCdocument.
			//Move the object to the MIRCdocument's directory.
			FileObject object = FileObject.getInstance(files[i]);
			object.setStandardExtension();
			try { md.insertFile(object.getFile(), true, textExtensions, anonymize); }
			catch (Exception skip) { }
		}

		//Sort the image-section
		md.sortImageSection();

		//Change the read permission and set the publication request, if necessary.
		md.setPublicationRequest(canPublish);

		//Save and index the document
		md.save();
		index.insertDocument(indexEntry);
	}

	class Filter implements FileFilter {
		boolean directories;
		String[] skip;
		public Filter(boolean directories) {
			this.directories = directories;
			this.skip = new String[0];
		}
		public Filter(boolean directories, String[] skip) {
			this.directories = directories;
			this.skip = skip;
			for (int i=0; i<skip.length; i++)
				skip[i] = skip[i].toLowerCase().trim();
		}
		public boolean accept(File file) {
			if (directories) return file.isDirectory();
			if (file.isDirectory()) return false;
			String name = file.getName().toLowerCase();
			if (name.equals("template.xml")) return false;
			for (int i=0; i<skip.length; i++) {
				if (!skip[i].equals("") && name.endsWith(skip[i])) return false;
			}
			return true;
		}
	}

	private boolean isValidTemplate(File template) {
		try {
			String root = XmlUtil.getDocument(template).getDocumentElement().getTagName();
			return (root != null) && root.equals("MIRCdocument");
		}
		catch (Exception ex) { return false; }
	}

	/**
	 * Unpack a zip file into a root directory, preserving the directory structure of the zip file.
	 * @param root the directory into which to unpack the zip file.
	 * @param inFile the zip file to unpack.
	 * @throws Exception if anything goes wrong.
	 */
	private static void unpackZipFile(File root, File inFile) throws Exception {
		if (!inFile.exists()) throw new Exception("Zip file does not exist ("+inFile+")");
		ZipFile zipFile = new ZipFile(inFile);
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry entry = zipEntries.nextElement();
			String name = entry.getName().replace('/', File.separatorChar);

			//Make sure that the directory is present
			File outFile = new File(root, name);
			outFile.getParentFile().mkdirs();

			if (!entry.isDirectory()) {
				//Clean up any file names that might cause a problem in a URL.
				name = outFile
						.getName()
							.trim()
								.replaceAll("[\\s]+","_")
									.replaceAll("[\"&'><#;:@/?=]","_");
				outFile = new File(outFile.getParentFile(), name);

				//Now write the file with the corrected name.
				OutputStream out = new FileOutputStream(outFile);
				InputStream in = zipFile.getInputStream(entry);
				FileUtil.copy( in, out, -1 );
			}
		}
		FileUtil.close(zipFile);
	}
}
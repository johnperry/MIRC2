/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.stages;

import java.io.*;
import mirc.activity.ActivityDB;
import mirc.MircConfig;
import mirc.storage.Index;
import mirc.util.MircDocument;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.StorageService;
import org.w3c.dom.Element;
import org.rsna.util.DigestUtil;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;

/**
 * The PipelineStage that receives DicomObjects and stores them in MIRCdocuments.
 */
public class MircDocumentStorageService extends AbstractPipelineStage implements StorageService {

	static final Logger logger = Logger.getLogger(MircDocumentStorageService.class);

	File lastFileStored = null;
	long lastTime = 0;
	File lastFileIn = null;
    int totalCount = 0;
    int storedCount = 0;
    String ssid = "";
    int ssidTag = 0;
    int caseTag = 0;
    String templateName = "";
    static final String defaultTemplateName = "DicomServiceTemplate.xml";

	/**
	 * Construct a MircDocumentStorageService.
	 * @param element the XML element from the configuration file
	 * specifying the configuration of the stage.
	 */
	public MircDocumentStorageService(Element element) {
		super(element);

		//Get the library identifier
		ssid = element.getAttribute("ssid");
		String ssidTagString = element.getAttribute("ssidTag").trim();
		ssidTag = DicomObject.getElementTag(ssidTagString);

		//Get the identifier to be used for the document directory
		String caseTagString = element.getAttribute("caseTag").trim();
		caseTag = DicomObject.getElementTag(caseTagString);
		if (caseTag == 0) caseTag = DicomObject.getElementTag("StudyInstanceUID");

		//Get the name of the template to use.
		//No path information is allowed.
		templateName = element.getAttribute("template").trim();
		if (templateName.equals("")) templateName = defaultTemplateName;
		else templateName = new File(templateName).getName();
	}

	/**
	 * Store an object. If the storage attempt fails, quarantine the
	 * input object if a quarantine was defined in the configuration,
	 * and return null to stop further processing. Non-DicomObjects are
	 * passed on without storage.
	 * @param fileObject the object to process.
	 * @return the original FileObject, or null if the object could not be stored.
	 */
	public FileObject store(FileObject fileObject) {

		//Count all the files
		totalCount++;

		//Store the object
		if (fileObject instanceof DicomObject) {

			try {
				MircConfig mc = MircConfig.getInstance();
				Element lib = null;

				DicomObject dob = (DicomObject)fileObject;
				String siUID = dob.getStudyInstanceUID();
				String caseName = StringUtil.filterName( dob.getElementValue(caseTag) );
				if (caseName.equals("")) caseName = siUID;

				//Hash the caseName so it can't contain PHI
				caseName = DigestUtil.hash( caseName, 20 ); //limit it to 20 characters

				//Get the library in which to store the MIRCdocument.
				//If there is a non-zero ssidTag, then get the ssid from
				//the specified element.
				//If a valid ssid cannot be found, use the default ssid
				//specified in the configuration element.
				//If after all this, the ssid is not valid, use the first
				//enabled library.
				//If no library can be found, forget it.
				if (ssidTag != 0) {
					String ssidFromTag = dob.getElementValue(ssidTag).trim();
					lib = mc.getLocalLibrary(ssidFromTag);
				}
				if (lib != null) {
					if (!lib.getAttribute("dcmenb").equals("yes")) lib = null;
				}
				if (lib == null) {
					lib = mc.getLocalLibrary(ssid);
					if (lib == null) lib = mc.getFirstEnabledLocalLibrary("dcmenb");
					if (lib == null) {
						logger.warn("Unable to find an enabled library in which to create a MIRCdocument.");
						return fileObject;
					}
				}

				//Get the index of the selected library
				String libID = lib.getAttribute("id");

				//Get the index and find the directory containing the documents
				Index index = Index.getInstance(libID);
				File docs = index.getDocumentsDir();

				//See if there is a MircDocument already in place,
				//and if not, create one from the template.
				File mdDir = new File(docs, caseName);
				File mdFile = new File(mdDir, "MIRCdocument.xml");
				MircDocument md;
				boolean docExists = mdFile.exists();
				if (!docExists) {
					mdDir.mkdirs();
					//Get the template
					//The strategy is to use the one that is specfied in configuration,
					//if it exists, and the default one if the specified one doesn't exist.
					File template = new File( docs.getParentFile(), templateName );
					template = FileUtil.getFile( template, "/storage/"+defaultTemplateName );
					md = new MircDocument(template);
					md.saveAs(mdFile);
				}
				else {
					md = new MircDocument(mdFile);
				}

				//Hash the SOPInstanceUID to prevent PHI leakage through the filename.
				String sopiUID = dob.getSOPInstanceUID();
				String hashed_sopiUID = DigestUtil.hash(sopiUID, 15);
				String hashed_name = hashed_sopiUID + ".dcm";
				File newName = new File( dob.getFile().getParentFile(), hashed_name );
				dob.renameTo(newName);

				//Insert the object, allowing overwrites to prevent duplicate images in the document
				md.insertDicomElements(dob);
				md.insert(dob, true);

				//Sort the image section, if it exists
				md.sortImageSection();

				//Save the MircDocument and index it
				md.save();
				index.insertDocument( index.getKey(mdFile) );

				//Record the activity
				if (!docExists) ActivityDB.getInstance().increment(libID, "dcm");

				lastFileStored = fileObject.getFile();
				lastTime = System.currentTimeMillis();
				storedCount++;
				return fileObject;
			}
			catch (Exception ex) {
				//If we didn't store the object, then quarantine it and abort.
				logger.debug("...unable to process object; object quarantined",ex);
				if (quarantine != null) quarantine.insert(fileObject);
				return null;
			}
		}
		//Pass on other object types
		return fileObject;
	}

	/**
	 * Get HTML text displaying the current status of the stage.
	 * @return HTML text displaying the current status of the stage.
	 */
	public String getStatusHTML() {
		StringBuffer sb = new StringBuffer();
		sb.append("<h3>"+name+"</h3>");
		sb.append("<table border=\"1\" width=\"100%\">");

		sb.append("<tr><td width=\"20%\">Files received for storage:</td>"
			+ "<td>" + totalCount + "</td></tr>");

		sb.append("<tr><td width=\"20%\">Files stored:</td>"
			+ "<td>" + storedCount + "</td></tr>");

		sb.append("<tr><td width=\"20%\">Last file stored:</td>");
		if (lastTime != 0) {
			sb.append("<td>"+lastFileStored+"</td></tr>");
			sb.append("<tr><td width=\"20%\">Last file stored at:</td>");
			sb.append("<td>"+StringUtil.getDateTime(lastTime,"&nbsp;&nbsp;&nbsp;")+"</td></tr>");
		}
		else sb.append("<td>No activity</td></tr>");
		sb.append("</table>");
		return sb.toString();
	}
}
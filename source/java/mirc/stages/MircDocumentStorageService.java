/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.stages;

import java.io.*;
import mirc.MircConfig;
import mirc.storage.Index;
import mirc.util.MircDocument;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.StorageService;
import org.w3c.dom.Element;
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

	/**
	 * Construct a MircDocumentStorageService.
	 * @param element the XML element from the configuration file
	 * specifying the configuration of the stage.
	 */
	public MircDocumentStorageService(Element element) {
		super(element);
		ssid = element.getAttribute("libraryID");
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
				DicomObject dob = (DicomObject)fileObject;
				String siUID = dob.getStudyInstanceUID();
				String sopiUID = dob.getSOPInstanceUID();

				//Get the library in which to store the MIRCdocument
				MircConfig mc = MircConfig.getInstance();
				Element lib = mc.getLocalLibrary(ssid);
				if (lib == null) lib = mc.getFirstEnabledLocalLibrary();
				if (lib == null) return fileObject; //bail out if we can't get a library

				//Get the index of the selected library
				String libID = lib.getAttribute("id");

				//Get the index and find the directory containing the documents
				Index index = Index.getInstance(libID);
				File docs = index.getDocumentsDir();

				//See if there is a MircDocument already in place,
				//and if not, create one from the template.
				File mdDir = new File(docs, siUID);
				File mdFile = new File(mdDir, "MIRCdocument.xml");
				MircDocument md;
				if (!mdFile.exists()) {
					mdDir.mkdirs();
					//Get the template
					File template = new File( docs.getParentFile(), "DicomServiceTemplate.xml" );
					template = FileUtil.getFile( template, "/storage/DicomServiceTemplate.xml" );
					md = new MircDocument(template);
					md.saveAs(mdFile);
				}
				else md = new MircDocument(mdFile);

				//Add the object to the MIRCdocument
				File newName = new File( fileObject.getFile().getParentFile(), sopiUID+".dcm" );
				fileObject.renameTo(newName);
				md.insertObject(fileObject, false, null, false);

				//Sort the image section, if it exists
				md.sortImageSection();

				//Save the MircDocument and index it
				md.save();
				index.insertDocument( index.getKey(mdFile) );

				lastFileStored = fileObject.getFile();
				lastTime = System.currentTimeMillis();
				storedCount++;
				return fileObject;
			}
			catch (Exception ex) {
				//If we didn't store the object, then quarantine it and abort.
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
/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.stages;

import java.io.*;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.StorageService;
import org.w3c.dom.Element;
import org.rsna.server.User;
import org.rsna.server.Users;
import mirc.files.FileService;
import mirc.files.FilePath;
import org.rsna.util.StringUtil;

/**
 * The PipelineStage that receives DicomObjects and stores them in file cabinets.
 */
public class MircFileStorageService extends AbstractPipelineStage implements StorageService {

	static final Logger logger = Logger.getLogger(MircFileStorageService.class);

	File lastFileStored = null;
	long lastTime = 0;
	File lastFileIn;
    int totalCount = 0;
    int storedCount = 0;
	int fsNameTag = 0;

	/**
	 * Construct a MircFileStorageService.
	 * @param element the XML element from the configuration file
	 * specifying the configuration of the stage.
	 */
	public MircFileStorageService(Element element) {
		super(element);
		lastFileIn = null;
		fsNameTag = StringUtil.getHexInt(element.getAttribute("fsNameTag"));
	}

	/**
	 * Store a DicomObject. If the storage attempt fails, quarantine the
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
			DicomObject dob = (DicomObject)fileObject;
			String siUID = dob.getStudyInstanceUID();
			String sopiUID = dob.getSOPInstanceUID();

			String username = "";
			if (fsNameTag != 0) {
				byte[] bytes = dob.getElementBytes(fsNameTag);
				username = new String(bytes).trim();
				User user = Users.getInstance().getUser( username );
				if (user == null) username = "";
			}
			String cat = username.equals("") ? "Shared" : "Personal";
			String path = cat + "/" + siUID;
			FilePath fp = FileService.getFilePath(path, username);

			File savedFile = new File( fp.filesDir, sopiUID+".dcm" );
			if (fileObject.copyTo(savedFile)) {
				FileService.makeIcon( savedFile, fp.iconsDir );
				lastFileStored = fileObject.getFile();
				lastTime = System.currentTimeMillis();
				storedCount++;
				return fileObject;
			}

			//If we didn't store the object, then quarantine it and abort.
			if (quarantine != null) quarantine.insert(fileObject);
			return null;
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

		sb.append("<tr><td width=\"20%\">Files storages:</td>"
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
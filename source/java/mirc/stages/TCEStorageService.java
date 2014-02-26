/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.stages;

import java.io.*;
import java.util.Properties;
import mirc.activity.ActivityDB;
import mirc.MircConfig;
import mirc.storage.Index;
import mirc.util.MircDocument;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.objects.XmlObject;
import org.rsna.ctp.pipeline.AbstractPipelineStage;
import org.rsna.ctp.pipeline.StorageService;
import org.rsna.ctp.stdstages.anonymizer.dicom.DAScript;
import org.rsna.ctp.stdstages.anonymizer.dicom.DICOMAnonymizer;
import org.rsna.ctp.stdstages.anonymizer.IntegerTable;
import org.rsna.ctp.stdstages.anonymizer.LookupTable;
import org.rsna.ctp.stdstages.ScriptableDicom;
import org.rsna.server.User;
import org.rsna.server.Users;
import org.rsna.server.UsersXmlFileImpl;
import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.w3c.dom.Element;

/**
 * The TCE Selector StorageService PipelineStage. This stage manages TCE submissions
 * and creates MIRCdocuments.
 */
public class TCEStorageService extends AbstractPipelineStage implements StorageService, ScriptableDicom {

	static final Logger logger = Logger.getLogger(TCEStorageService.class);

	File lastFileStored = null;
	long lastTime = 0;
	File lastFileIn = null;
    int totalCount = 0;
    String ssid = "";
    int ssidTag = 0;

    boolean autocreate = false;
    TCEStore store = null;
    ManifestProcessor mp = null;

	boolean anonymize = false;
	public File scriptFile = null;
	public File lookupTableFile = null;
	public IntegerTable intTable = null;

	/**
	 * Construct a TCEStorageService.
	 * @param element the XML element from the configuration file
	 * specifying the configuration of the stage.
	 */
	public TCEStorageService(Element element) {
		super(element);
		ssid = element.getAttribute("ssid");
		String ssidTagString = element.getAttribute("ssidTag").trim();
		ssidTag = DicomObject.getElementTag(ssidTagString);

		//Get the attribute that enables creation of new accounts.
		//Automatically created accounts are created with the password
		//equal to the username. Such accounts are automatically
		//granted the author role.
		autocreate = element.getAttribute("autocreate").equals("yes");

		//Get the attribute that determines whether to anonymize
		//the objects inserted into the MIRCdocument.
		anonymize = element.getAttribute("anonymize").equals("yes");
		scriptFile = FileUtil.getFile(element.getAttribute("script").trim(), "examples/example-mirc-dicom-anonymizer.script");
		lookupTableFile = FileUtil.getFile(element.getAttribute("lookupTable"), (String)null);
		try { intTable = new IntegerTable(root); }
		catch (Exception ex) { logger.warn(name+": "+ex.getMessage()); }

		//Instantiate the TCEStore for instances and manifests.
		//The TCEStore also provides the queue of manifests for
		//which all the instances have been received. It also
		//provides a garbage collection mechanism for files
		//that have timed out.
		store = new TCEStore(root);

		//Start the thread that processes the manifest queue
		mp = new ManifestProcessor();
		mp.start();
	}

	/**
	 * Get the script file.
	 */
	public File getScriptFile() {
		return scriptFile;
	}

	/**
	 * Get the lookup table file.
	 */
	public File getLookupTableFile() {
		return lookupTableFile;
	}

	/**
	 * Store an object. If the storage attempt fails, quarantine the
	 * input object if a quarantine was defined in the configuration,
	 * and return null to stop further processing.
	 * @param fileObject the object to store.
	 * @return the original FileObject.
	 */
	public FileObject store(FileObject fileObject) {
		//Count all the files
		totalCount++;

		//Store the object
		store.store(fileObject);

		lastFileStored = fileObject.getFile();
		lastTime = System.currentTimeMillis();
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

		sb.append("<tr><td width=\"20%\">Files received:</td>"
			+ "<td>" + totalCount + "</td></tr>");

		sb.append("<tr><td width=\"20%\">Manifests stored:</td>"
			+ "<td>" + store.getManifestCount() + "</td></tr>");

		sb.append("<tr><td width=\"20%\">Instances stored:</td>"
			+ "<td>" + store.getInstanceCount() + "</td></tr>");

		sb.append("<tr><td width=\"20%\">Manifests queued:</td>"
			+ "<td>" + store.getQueuedManifestCount() + "</td></tr>");

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

	//The asynchronous Thread to monitor the queue,
	//process manifests, and create MIRCdocuments.
	class ManifestProcessor extends Thread {

		public boolean running = false;

		public ManifestProcessor() {
			super("TCE Service ManifestProcessor");
		}

		public void run() {
			running = true;
			while (!interrupted() && !stop) {
				try { sleep(5000); }
				catch (Exception ex) { }

				File[] manifestFiles = store.getQueuedManifests();
				if (manifestFiles.length > 0) processManifest(manifestFiles[0]);
			}
			running = false;
		}

		//Process one manifest, creating a new MIRCdocument for it
		//and all its referenced instances.
		private void processManifest(File file) {
			logger.debug("Processing manifest: "+file);
			try {
				MircConfig mc = MircConfig.getInstance();
				Element lib = null;

				//Get the manifest and its referenced instances.
				//Note that a standard TCE manifest is a DicomObject,
				//but MIRC also supports a special manifest in the form
				//of an XML MIRCdocument containing a special element
				//listing the instances. Thus, the following code
				//has to handle both DicomObjects and XmlObjects.
				FileObject manifest = FileObject.getInstance(file);

				//Get the library in which to store the MIRCdocument.
				//If the manifest is a DicomObject and there is a non-zero
				//ssidTag, then get the ssid from the specified element.
				//If the manifest is an XmlObject, get the ssid from the
				//ssid attribute of the root element.
				//If a valid ssid cannot be found, use the default ssid
				//specified in the configuration element.
				//If after all this, the ssid is not valid, use the first
				//enabled library.
				//If no library can be found, forget it.
				if (manifest instanceof DicomObject) {
					DicomObject dicomManifest = (DicomObject)manifest;
					if (ssidTag != 0) {
						String ssidFromTag = dicomManifest.getElementValue(ssidTag).trim();
						lib = mc.getLocalLibrary(ssidFromTag);
					}
				}
				else if (manifest instanceof XmlObject) {
					XmlObject xmlObject = (XmlObject)manifest;
					Element root = xmlObject.getDocument().getDocumentElement();
					String ssidFromAttr = root.getAttribute("ssid").trim();
					if (!ssidFromAttr.equals("")) {
						lib = mc.getLocalLibrary(ssidFromAttr);
					}
				}
				if (lib != null) {
					if (!lib.getAttribute("tceenb").equals("yes")) lib = null;
				}
				if (lib == null) {
					lib = mc.getLocalLibrary(ssid);
					if (lib == null) lib = mc.getFirstEnabledLocalLibrary("tceenb");
					if (lib == null) {
						logger.warn("Unable to find an enabled library in which to create a MIRCdocument.");
						return;
					}
				}

				//Get the ID of the selected library
				String libID = lib.getAttribute("id");

				logger.debug("Selected library: "+libID);

				//Get the library index and find the directory containing the documents
				Index index = Index.getInstance(libID);
				File docs = index.getDocumentsDir();

				//Make a directory for this MIRCdocument
				File dir = new File(docs, StringUtil.makeNameFromDate());
				dir.mkdirs();

				//Make a temp child directory.
				File temp = new File(dir, "temp");
				temp.mkdirs();

				//Move the manifest to the temp directory to get it out of the queue.
				manifest.moveToDirectory(temp, true);

				//Set up to get the template and the referenced objects to insert into it.
				MircDocument md = null;
				String[] refs = null;

				if (manifest instanceof DicomObject) {
					DicomObject dicomManifest = (DicomObject)manifest;
					refs = dicomManifest.getInstanceList();

					//Get the template. Since this is a DICOM TCE manifest,
					//the template must be supplied externally. As in most
					//MIRC services, the template may be in the jar or it
					//may be a file in a directory. In this case, the file,
					//if it exists, must be in the root directory of the library.
					//The root directory of a library is located at storage/ss{n}.
					File template = new File( docs.getParentFile(), "TCEServiceTemplate.xml" );
					template = FileUtil.getFile( template, "/storage/TCEServiceTemplate.xml" );
					md = new MircDocument(template);
				}
				else if (manifest instanceof XmlObject) {
					XmlObject xmlObject = (XmlObject)manifest;
					md = new MircDocument(xmlObject.getDocument());
					refs = md.getInstanceList();
					md.removeManifestElement();
				}

				//Make a file for the MIRCdocument and pass it to the
				//MircDocument object. This will tell it where to save itself.
				File mdFile = new File(dir, "MIRCdocument.xml");
				md.setFile(mdFile);
				logger.debug("MIRCdocument file: "+mdFile);

				//Put in the manifest and all the instances and store the updated document.
				//Note: we have to copy the files to a temp directory to protect them
				//from deletion by the insert method. We don't delete instances at all
				//because they may apply to multiple manifests.

				//First do the manifest, but only if it was a DicomObject.
				//This will capture any information in the ObserverList, which
				//is used to get the author name(s), and in the Key Object Description,
				//which is a MIRC expansion to allow information that would normally
				//be included in an ATFI object to be encapsulated in a manifest. It
				//also allows information not supported by the TCE profile to be
				//provided to the system.
				if (manifest instanceof DicomObject) {
					md.insert((DicomObject)manifest, true);
					logger.debug("Inserted DICOM manifest");
				}

				//Now do the references, which must all be DicomObjects.
				for (String ref : refs) {
					File refFile = store.getInstanceFile(ref);
					File trefFile = new File(temp, ref);
					FileUtil.copy(refFile, trefFile);
					DicomObject dob = new DicomObject(trefFile);

					if (anonymize && !dob.isManifest() && !dob.isAdditionalTFInfo()) {
						DAScript dascript = DAScript.getInstance(scriptFile);
						Properties script = dascript.toProperties();
						Properties lookup = LookupTable.getProperties(lookupTableFile);
						DICOMAnonymizer.anonymize(trefFile, trefFile, script, lookup, intTable, false, false);
						dob = new DicomObject(trefFile);
						dob.renameToUID();
					}

					md.insert(dob, true);
					if (!dob.isAdditionalTFInfo()) md.insertDicomElements(dob);
					logger.debug("Inserted instance: "+dob.getFile());
				}
				md.sortImageSection();
				md.save();
				logger.debug("MIRCdocument saved");

				//Index the document
				index.insertDocument( index.getKey(mdFile) );

				//Record the activity
				ActivityDB.getInstance().increment(libID, "tce", null);

				logger.debug("MIRCdocument indexed");

				//Now we can delete the temp directory.
				FileUtil.deleteAll(temp);

				//Now create an account for the owners of the document, if
				//account creation is enabled and the owners don't exist.
				if (autocreate) {
					//Creation is enabled; get the Users.
					Users users = Users.getInstance();
					if (users instanceof UsersXmlFileImpl) {
						//Get the owners.
						String[] owners = md.getOwners();
						//Create accounts for the owners if necessary.
						for (String owner : owners) {
							User user = users.getUser(owner);
							if ( !owner.equals("") && (user == null) ) {
								user = new User( owner, users.convertPassword(owner) );
								user.addRole("author");
								((UsersXmlFileImpl)users).addUser(user);
							}
						}
					}
				}
			}

			catch (Exception ex) {
				//Something really bad happened; log the event and delete the manifest file.
				logger.info("Unable to process manifest and instances: "+file.getName(), ex);
				file.delete();
			}
		}

	}
}
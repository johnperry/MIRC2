/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.stages;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import mirc.util.MircDocument;
import org.apache.log4j.Logger;
import org.rsna.ctp.objects.DicomObject;
import org.rsna.ctp.objects.FileObject;
import org.rsna.ctp.objects.XmlObject;
import org.rsna.util.FileUtil;

/**
 * A class to encapsulate the storage system for the TCE Service.
 * This class manages the received objects and divides them into
 * two directories, one for manifests and one for other instances. It
 * provides methods for verifying that all the instances required by a
 * manifest are present.
 */
public class TCEStore {

	static final String manifestsName = "manifests";
	static final String instancesName = "instances";
	static final String queueName = "queue";

	static final Logger logger = Logger.getLogger(TCEStore.class);

	File store;
	File manifests;
	File instances;
	File queue;
	int currentCount = Integer.MAX_VALUE;
	GarbageCollector collector;
	String timeout;

	/**
	 * Class constructor; creates a Store and its required subdirectories.
	 * @param store the file pointing to where the Store is to be created.
	 * @throws IOException if the Store cannot be created.
	 */
	public TCEStore(File store) {
		this.store = store;
		manifests = new File(store, manifestsName);
		instances = new File(store, instancesName);
		queue = new File(store,queueName);
		manifests.mkdirs();
		instances.mkdirs();
		queue.mkdirs();
		checkManifests();
		collector = new GarbageCollector();
		collector.start();
	}

	/**
	 * Get a list of the files in the queue, sorted in
	 * order by last modified time.
	 * @return the list of manifests in the queue in the order
	 * in which they should be processed.
	 */
	public synchronized File[] getQueuedManifests() {
		return FileUtil.listSortedFiles(queue);
	}

	/**
	 * Get a File pointing to a named instance.
	 * @param name the name of the file to get.
	 */
	public File getInstanceFile(String name) {
		return new File(instances, name);
	}

	/**
	 * Get the number of manifests in the store.
	 * @return the number of files in the manifests directory.
	 */
	public synchronized int getManifestCount() {
		return countFiles(manifests);
	}

	/**
	 * Get the number of manifests in the store.
	 * @return the number of files in the queue directory.
	 */
	public synchronized int getQueuedManifestCount() {
		return countFiles(queue);
	}

	/**
	 * Get the number of instances in the store.
	 * @return the number of files in the instances directory.
	 */
	public synchronized int getInstanceCount() {
		return countFiles(instances);
	}

	//Count the number of files in a directory.
	private int countFiles(File dir) {
		if (!dir.exists()) return 0;
		return dir.listFiles().length;
	}

	/**
	 * Delete all the files in the store.
	 */
	public synchronized void deleteAllFiles() {
		deleteAllFiles(manifests);
		deleteAllFiles(instances);
		deleteAllFiles(queue);
	}

	private void deleteAllFiles(File dir) {
		if (!dir.exists()) return;
		for (File file : dir.listFiles()) {
			file.delete();
		}
	}

	/**
	 * Store an object in the appropriate store directory,
	 * depending on whether it is an instance or a manifest.
	 * A manifest object can be either a DicomObject or
	 * an XmlObject in the form of a MIRCdocument containing
	 * a manifest element listing the SOPInstanceUIDs of the
	 * instances.
	 * @param fileObject the object to be stored
	 */
	public synchronized void store(FileObject fileObject) {

		if (fileObject instanceof DicomObject) {
			DicomObject dicomObject = (DicomObject)fileObject;
			if (dicomObject.isManifest()) {
				//Put the file in the manifests directory
				File dest = new File(manifests, dicomObject.getSOPInstanceUID());
				dicomObject.copyTo(dest);

				//Check all the manifests, queue any completed ones, and set the currentCount.
				checkManifests();
			}
			else {
				//Put the file in the instances directory
				File dest = new File(instances, dicomObject.getSOPInstanceUID());
				dicomObject.copyTo(dest);

				//Count the instance and, if it is possible that a manifest has been fulfilled,
				//check the manifests, queue any completed ones, and set a new currentCount value.
				currentCount--;
				if (currentCount <= 0) checkManifests();
			}
		}
		else if (fileObject instanceof XmlObject) {
			XmlObject xmlObject = (XmlObject)fileObject;
			try {
				MircDocument md = new MircDocument(xmlObject.getDocument());
				if (md.isManifest()) {
					//Put the file in the manifests directory
					File dest = File.createTempFile("MD-", "", manifests);
					xmlObject.copyTo(dest);

					//Check all the manifests, queue any completed ones, and set the currentCount.
					checkManifests();
				}
			}
			catch (Exception notAManifest) { }
		}
	}

	//Find all the instance names referenced by manifests
	//in a directory and insert them into a Set.
	private void addReferencedInstances(Set<String> set, File manifestDir) {
		for (File manifestFile : manifestDir.listFiles()) {
			FileObject fileObject = FileObject.getInstance(manifestFile);
			String[] refs = null;
			if (fileObject instanceof DicomObject) {
				DicomObject manifest = (DicomObject)fileObject;
				refs = manifest.getInstanceList();
			}
			else if (fileObject instanceof XmlObject) {
				XmlObject xmlObject = (XmlObject)fileObject;
				try {
					MircDocument md = new MircDocument(xmlObject.getDocument());
					refs = md.getInstanceList();
				}
				catch (Exception notAManifest) { }
			}
			if (refs != null) {
				for (String ref : refs) {
					set.add(ref);
				}
			}
		}
	}

	//Check the manifests, queue any complete manifests, and set the currentCount.
	private void checkManifests() {
		int min = Integer.MAX_VALUE;
		File[] manifestList = FileUtil.listSortedFiles(manifests);
		for (File manifestFile : manifestList) {
			try {
				FileObject fileObject = FileObject.getInstance(manifestFile);
				String[] refs = null;
				if (fileObject instanceof DicomObject) {
					DicomObject manifest = (DicomObject)fileObject;
					refs = manifest.getInstanceList();
				}
				else if (fileObject instanceof XmlObject) {
					XmlObject xmlObject = (XmlObject)fileObject;
					MircDocument md = new MircDocument(xmlObject.getDocument());
					refs = md.getInstanceList();
				}
				if (refs != null) {
					int count = countMissingInstances(refs);
					if (count == 0) queueManifest(fileObject);
					else if (count < min) min = count;
				}
			}
			catch (Exception ignore) { }
		}
		currentCount = min;
	}

	//Queue a manifest.
	private void queueManifest(FileObject manifest) {
		manifest.touch();
		manifest.moveToDirectory(queue, true);
		logger.debug("Manifest completed and queued: " + manifest.getFile().getName());
	}

	//Count the number of files in a list that are missing
	//from the instances directory.
	private int countMissingInstances(String[] names) {
		File file;
		int count = 0;
		for (String name : names) {
			file = new File(instances, name);
			if (!file.exists()) count++;
		}
		return count;
	}

	//A garbage collector Thread to remove all expired
	//files from the store after a 60-minute timeout.
	class GarbageCollector extends Thread {
		long timeout = 60L * 60L * 1000L;
		public GarbageCollector() {
			this.setPriority(Thread.MIN_PRIORITY);
		}
		public void run() {
			while (true) {
				try {
					//Sleep first, then remove the files.
					sleep(timeout);
					removeExpiredFiles(timeout);
				}
				catch (Exception e) { }
			}
		}

		private void removeExpiredFiles(long timeoutMillis) {
			boolean deleted = false;

			//Get the earliest time to protect
			long time = System.currentTimeMillis() - timeoutMillis;

			//Remove any expired manifests
			for (File file : manifests.listFiles()) {
				if (file.lastModified() < time) deleted |= file.delete();
			}

			//Make a set of instance names that are referenced by
			//manifests that are either already queued or are unexpired.
			HashSet<String> set = new HashSet<String>();
			addReferencedInstances(set, queue);
			addReferencedInstances(set, manifests);

			//Handle any expired instances that can safely be removed.
			for (File file : instances.listFiles()) {
				if ( (file.lastModified() < time) && !set.contains(file.getName()) ) {
					deleted |= file.delete();
				}
			}
		}

	}

}

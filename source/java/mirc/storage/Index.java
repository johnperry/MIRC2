/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.io.*;
import java.util.*;
import jdbm.btree.BTree;
import jdbm.helper.FastIterator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.htree.HTree;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import mirc.MircConfig;
import mirc.util.MircDocument;
import mirc.util.MircImage;
import org.apache.log4j.Logger;
import org.rsna.server.User;
import org.rsna.util.FileUtil;
import org.rsna.util.JdbmUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.*;

/**
 * The index of documents in a local library.
 */
public class Index {

	static final Logger logger = Logger.getLogger(Index.class);

	private static Hashtable<String,Index> indexTable = new Hashtable<String,Index>();

	private String ssid;
	private RecordManager recman;
	private File indexFile;
	private File documentsDir;
	private HTree pathToID;
	private HTree idToPath;
	private HTree idToMIE;
	private IndexDatabase freetext;
	private Hashtable<String,IndexDatabase> fields;
	private Hashtable<Integer,IndexEntry> idToMIEShadow;
	private static Unfragmented unfragmented = new Unfragmented();
	private static final String docs = "docs";


	/**
	 * Get the singleton Index object for a specified local library.
	 * @param ssid the ID of the local library.
	 * @return the index object, or null if the index does not exist.
	 */
	public static Index getInstance(String ssid) {
		Index index = indexTable.get(ssid);
		if (index == null) {
			MircConfig mc = MircConfig.getInstance();
			Element lib = mc.getLocalLibrary(ssid);
			if (lib != null) {
				 //indexes are in the root of the plugin
				File root = mc.getRootDirectory();
				File indexFile = new File(root, ssid);

				//documents are in /storage/{ssid}/{docs}
				File storage = new File(root, "storage");
				File service = new File(storage, ssid);
				File documentsDir = new File(service, docs);

				//create the index and save it in the table
				try {
					index = new Index(documentsDir, indexFile);
					indexTable.put(ssid, index);
				}
				catch (Exception ex) { logger.warn("Unable to create the index for \""+ssid+"\""); }
			}
		}
		return index;
	}

	/**
	 * Commit and close all the index databases.
	 * This method is intended to be called only
	 * during the final shutdown of the MIRC system..
	 */
	public static void closeAll() {
		for (Index index : indexTable.values()) {
			index.close();
		}
	}

	/**
	 * Instantiate the index database, creating the database file
	 * if it is missing, but not populating it.
	 * @param documentsDir the path to the storage service's documents directory
	 * @param indexFile the path to the index file (without any extension).
	 */
	protected Index(File documentsDir, File indexFile)  throws Exception {
		this.documentsDir = documentsDir;
		this.indexFile = indexFile;
		this.fields = new Hashtable<String,IndexDatabase>();
		openIndex();
	}

	//Get the record manager, find all the tables,
	//and instantiate the index databases
	private void openIndex() throws Exception {
		try {
			recman = JdbmUtil.getRecordManager(indexFile.getPath());
			pathToID = JdbmUtil.getHTree(recman, "PathToID");
			idToPath = JdbmUtil.getHTree(recman, "IDToPath");
			idToMIE = JdbmUtil.getHTree(recman, "IDToMIE");
			freetext = new IndexDatabase(recman, "freetext", null);

			//build the shadow index
			idToMIEShadow = new Hashtable<Integer,IndexEntry>();
			HashSet<Integer> allIDs = freetext.getAllIDs();
			for (Integer id : allIDs) {
				IndexEntry mie = (IndexEntry)idToMIE.get(id);
				idToMIEShadow.put(id, mie);
			}

			//now open the query field databases
			openDatabase("title");
			openDatabase("author");
			openDatabase("abstract");
			openDatabase("keywords");
			openDatabase("history");
			openDatabase("findings");
			openDatabase("diagnosis");
			openDatabase("differential-diagnosis");
			openDatabase("discussion");
			openDatabase("pathology");
			openDatabase("anatomy");
			openDatabase("organ-system");
			openDatabase("code");
			openDatabase("modality");
			openDatabase("patient");
			openDatabase("document-type");
			openDatabase("category");
			openDatabase("level");
			openDatabase("access");
			openDatabase("peer-review");
			openDatabase("language");
			openDatabase("owner");
		}
		catch (Exception ex) {
			logger.warn("Unable to create/open the index");
			throw ex;
		}
	}

	private void openDatabase(String ssid) throws Exception {
		fields.put( ssid, new IndexDatabase(recman, ssid, unfragmented.get(ssid)) );
	}

	/**
	 * Commit any changes that have been made to the index database.
	 */
	public synchronized void commit() {
		if (recman != null) {
			try { recman.commit(); }
			catch (Exception ignore) { }
		}
	}

	/**
	 * Commit any changes that have been made to the index database
	 * and then close the database. This copies the database log
	 * into the database itself.
	 */
	public synchronized void close() {
		if (recman != null) {
			try { recman.commit(); recman.close(); recman = null; }
			catch (Exception ignore) { }
		}
	}

	//Delete the database files to that they can be rebuilt.
	private synchronized void delete() {
		File parent = indexFile.getParentFile();
		String indexName = indexFile.getName();
		(new File(parent, indexName + ".db")).delete();
		(new File(parent, indexName + ".lg")).delete();
	}

	/**
	 * Rebuild the index. This method is equivalent to
	 * <code>rebuild(20)</code>..
	 */
	public synchronized boolean rebuild() {
		return rebuild(20);
	}

	/**
	 * Close the current index if it is open, delete it, and then
	 * rebuild the index by walking the documents directory tree and
	 * finding all the MIRCdocuments.
	 * @return true if the index was rebuilt; false otherwise. If
	 * the operation failed in any way, the index is left in an
	 * indeterminate state.
	 */
	public synchronized boolean rebuild(int interval) {
		try {
			close();
			delete();
			openIndex();
			indexDirectory(documentsDir, 0, interval);
			recman.commit();
			return true;
		}
		catch (Exception ex) {
			logger.warn("Unable to rebuild the index: "+indexFile+".", ex);
			return false;
		}
	}

	//Walk a directory tree and add all the MIRCdocuments to the index.
	private int indexDirectory(File dir, int count, int interval) throws Exception {
		if (!dir.exists()) return count;
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isFile() && file.getName().toLowerCase().endsWith(".xml")) {
				logger.debug("...indexing "+file.getAbsolutePath());
				indexDocument(file);
				if ((count % interval) == 0) {
					System.gc();
					recman.commit();
				}
				count++;
			}
			else if (file.isDirectory()) count = indexDirectory(file, count, interval);
		}
		return count;
	}

	//Check whether a file parses as a MIRCdocument
	//and add it to the index if it does.
	private void indexDocument(File file) {
		try {
			Document doc = XmlUtil.getDocument(file);
			if (doc.getDocumentElement().getTagName().equals("MIRCdocument")) {
				String path = file.getPath();
				path = path.substring(path.indexOf(documentsDir.getName()));
				addDocument(file, path, doc);
			}
		}
		catch (Exception skip) {
			logger.warn("\nException caught while parsing " + file + "\n", skip);
		}
	}

	/**
	 * Get the documents directory for this index.
	 */
	public File getDocumentsDir() {
		return documentsDir;
	}

	/**
	 * Get the number of documents in the index.
	 */
	public int getIndexSize() {
		return idToMIEShadow.size();
	}

	/**
	 * Get the number of words and word fragments in all the documents
	 * in the index.
	 */
	public int getNumberOfWords() {
		return freetext.getNumberOfWords();
	}

	/**
	 * Get the IndexEntry for a specified path.
	 * @param path the path by which the MIRCdocument has
	 * been indexed. The path starts at the documents
	 * subdirectory of the root directory of the library.
	 * @return the IndexEntry corresponding to the path,
	 * or null if no entry has been indexed for that path.
	 */
	public IndexEntry getMircIndexEntry(String path) {
		try {
			Integer id = (Integer)pathToID.get(fixPath(path));
			return (IndexEntry)idToMIEShadow.get(id);
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Get the (unsorted) array of IndexEntry objects
	 * for MIRCdocuments that match a specified query.
	 * This method does a freetext search only.
	 * It does not restrict the results in any other way,
	 * even on a library that is operating in restricted
	 * mode. This query should only be used by admin functions.
	 * @param freetext the freetext query string.
	 */
	public IndexEntry[] query(String freetext) {
		return query(new Query(freetext), true, null);
	}

	/**
	 * Get the (unsorted) array of IndexEntry objects
	 * for MIRCdocuments that match a specified Query.
	 * @param mq the query object containing all the
	 * query fields.
	 */
	public IndexEntry[] query(Query mq, boolean isOpen, User user) {

		if (mq.isBlankQuery && !mq.containsNonFreetextQueries) {

			//Handle this case separately because it can be very fast.
			HashSet<IndexEntry> set = new HashSet<IndexEntry>( idToMIEShadow.values() );
			boolean isAdmin = (user != null) && user.hasRole("admin");
			if (!isOpen && !isAdmin) set = filterOnAccess(set, user);
			if (mq.containsAgeQuery) set = filterOnAge(set, mq);
			return set.toArray(new IndexEntry[set.size()]);
		}

		//Okay, it's not a simple query; do everything but the age
		HashSet<Integer> ids = null;
		if (!mq.isBlankQuery) ids = freetext.getIDsForQueryString(mq.get("freetext"));
		for (String name : mq.keySet()) {
			if (!name.equals("freetext")) {
				IndexDatabase db = fields.get(name);

				//If there is a field in the Query, then
				//it must be non-blank, and if there is no
				//corresponding IndexDatabase, then
				//we must return zero results
				if (db == null) return new IndexEntry[0];

				//Okay, we have a query field and the corresponding
				//IndexDatabase; do the query.
				HashSet<Integer> temp = db.getIDsForQueryString(mq.get(name));

				//If we got no matches on this field, then the final
				//result will have no matches, so we can bail out now.
				if (temp.size() == 0) return new IndexEntry[0];

				//Okay, we got some responses; use them to filter
				//what we have found so far.
				if (ids == null) ids = temp;
				else ids = IndexDatabase.intersection(ids, temp);
			}
		}

		//Now apply the access and age filters, if necessary.
		HashSet<IndexEntry> set = getMIESet(ids);
		boolean isAdmin = (user != null) && user.hasRole("admin");
		if (!isOpen && !isAdmin) set = filterOnAccess(set, user);
		if (mq.containsAgeQuery) set = filterOnAge(set, mq);
		return set.toArray( new IndexEntry[ set.size() ] );
	}

	private HashSet<IndexEntry> filterOnAccess( HashSet<IndexEntry> mieSet, User user ) {
		HashSet<IndexEntry> set = new HashSet<IndexEntry>();
		for (IndexEntry mie : mieSet) {
			if (mie.allows(user)) set.add(mie);
		}
		return set;
	}

	private HashSet<IndexEntry> filterOnAge( HashSet<IndexEntry> mieSet, Query mq ) {
		HashSet<IndexEntry> set = new HashSet<IndexEntry>();
		for (IndexEntry mie : mieSet) {
			if (mie.hasPatientInAgeRange(mq.minAge, mq.maxAge)) set.add(mie);
		}
		return set;
	}

	/**
	 * Get an (unsorted) array of IndexEntry objects
	 * from a HashSet of document ID objects. Document
	 * IDs are Integer objects which are automatically assigned
	 * when documents are indexed. They are reassigned when the
	 * index is rebuilt, so they should not be used for permanent
	 * identification of documents. (The path string is the
	 * preferred identifier for permanent reference.) In the
	 * returned array, any ID values from the HashSet which do
	 * not appear in the index are skipped.
	 */
	private HashSet<IndexEntry> getMIESet(HashSet<Integer> ids) {
		HashSet<IndexEntry> set = new HashSet<IndexEntry>();
		if (ids != null) {
			for (Integer id : ids) {
				try {
					IndexEntry mie = (IndexEntry)idToMIEShadow.get(id);
					set.add( mie );
				}
				catch (Exception skipThisOne) { }
			}
		}
		return set;
	}

	/**
	 * Sort an array of IndexEntry objects in
	 * alphabetical order by title.
	 */
	public static void sortByTitle(IndexEntry[] mies) {
		Arrays.sort(mies, new TitleComparator());
	}

	/**
	 * Sort an array of IndexEntry objects in
	 * reverse chronological order by last modified date.
	 */
	public static void sortByLMDate(IndexEntry[] mies) {
		Arrays.sort(mies, new LMDateComparator());
	}

	/**
	 * Sort an array of IndexEntry objects in
	 * chronological order by publication date.
	 */
	public static void sortByPubDate(IndexEntry[] mies) {
		Arrays.sort(mies, new PubDateComparator());
	}

	/**
	 * Insert a MIRCdocument in the index.
	 * @param path to the document in the form of the relative path
	 * from the parent of the storage services' documents directory
	 * to the MIRCdocument XML file.
	 * @return true if the document was entered into the index; false otherwise.
	 */
	public synchronized boolean insertDocument(String path) {
		try {
			path = fixPath(path);
			removeDocument(path);
			File file = new File( documentsDir.getParentFile(), path.replace("/", File.separator) );
			Document doc = XmlUtil.getDocument(file);
			addDocument(file, path, doc);
			recman.commit();
			return true;
		}
		catch (Exception ex) { return false; }
	}

	/**
	 * Insert a MIRCdocument into the index.
	 * @param file the file containing the MIRCdocument
	 * @param path the path by which the document is to be indexed
	 * @param doc the XML DOM object containing the parsed MIRCdocument
	 */
	private synchronized void addDocument(File file, String path, Document doc) throws Exception {
		//Set up to preserve the last modified date.
		long lastModified = file.lastModified();

		//Insert the RadLex terms and save the file
		Element root = doc.getDocumentElement();
		MircDocument.insertRadLexTerms(root);
		FileUtil.setText(file, XmlUtil.toString(doc));

		//Reset the last modified date after the RadLex terms were inserted.
		//Note: this must be done before creating the IndexEntry object,
		//or the current date will be used as the last modified date. This
		//would be incorrect in the case where the index is being rebuilt.
		file.setLastModified(lastModified);

		//Get the ID for the document
		path = fixPath(path);
		Integer id = getIDForPath(path);

		//Get the index entry for the document
		MircConfig mc = MircConfig.getInstance();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/storage/IndexDocument.xsl" ) );
		IndexEntry mie = new IndexEntry( file, path, doc, xsl );

		//Put the index entry into the index by ID
		idToMIE.put( id, mie );
		idToMIEShadow.put( id, mie );

		//Put everything in the freetext database
		freetext.indexString(id, getText(root));

		//Index the access from the index entry
		fields.get("access").indexString(id, mie.access);

		//Now do all the query fields
		for (String name : fields.keySet()) {
			IndexDatabase db = fields.get(name);
			NodeList nl = root.getElementsByTagName(name);
			db.indexString( id, getText( nl ) );
		}

		//Make sure the image sizes are in place
		if (setImageSizes(file, doc)) {
			FileUtil.setText(file, XmlUtil.toString(root));
		}

		//Reset the lmdate after the image sizes were updated
		file.setLastModified(lastModified);
	}

	//Check that all the image elements have w and h attributes.
	//If the attributes are missing for an image, open it, get the size,
	//and insert the attributes.
	private boolean setImageSizes(File file, Document doc) {
		boolean changed = false;
		try {
			boolean chg = false;
			File dir = file.getParentFile();
			Element root = doc.getDocumentElement();
			NodeList nl = root.getElementsByTagName("image");
			for (int i=0; i<nl.getLength(); i++) {
				Element image = (Element)nl.item(i);
				chg = setImageSize(dir, image);
				changed |= chg;
				NodeList altnl = image.getElementsByTagName("alternative-image");
				for (int k=0; k<altnl.getLength(); k++) {
					Element alt = (Element)altnl.item(k);
					String role = alt.getAttribute("role");
					String srclc = alt.getAttribute("src").toLowerCase();
					if (role.equals("icon")
							|| (role.equals("annotation") && (srclc.endsWith(".jpg") || srclc.endsWith(".jpeg")))
									|| role.equals("original-dimensions")) {
						chg = setImageSize(dir, alt);
						changed |= chg;
					}
				}
			}
		}
		catch (Exception skip) { }
		return changed;
	}

	//Set the w and h attributes for one image, if necessary.
	private boolean setImageSize(File dir, Element img) {
		if (img.getAttribute("w").trim().equals("") || img.getAttribute("h").trim().equals("")) {
			String src = img.getAttribute("src").trim();
			File imageFile = new File(dir, src);
			String srclc = src.toLowerCase();
			if (!src.equals("") && !srclc.startsWith("http://") && !srclc.startsWith("/") && !srclc.startsWith("\\")) {
				try {
					MircImage m = new MircImage(imageFile);
					img.setAttribute("w", Integer.toString(m.getWidth()));
					img.setAttribute("h", Integer.toString(m.getHeight()));
					return true;
				}
				catch (Exception skip) {
					logger.warn("Unable to set sizes for:\nDirectory: \""+dir+ "\"\n"
								+"Image file: \""+imageFile+"\"\n"
								+"src attribute: \""+src+"\"\n"
								+"image element:\n"
								+XmlUtil.toPrettyString(img), skip);
				}
			}
		}
		return false;
	}

	/**
	 * Remove a MIRCdocument from the index.
	 * @param path the path by which the MIRCdocument was indexed.
	 * @return true if the document was found in the index and
	 * successfully removed; false otherwise.
	 */
	public synchronized boolean removeDocument(String path) {
		boolean ok = false;
		path = fixPath(path);
		try {
			Integer id = (Integer)pathToID.get(path);
			if (id != null) {
				ok = freetext.removeDoc(id);
				for (String name : fields.keySet()) {
					IndexDatabase db = fields.get(name);
					ok &= db.removeDoc(id);
				}
				pathToID.remove(path);
				idToPath.remove(id);
				idToMIE.remove(id);
				idToMIEShadow.remove(id);
			}
		}
		catch (Exception failed) { ok = false; }
		if (ok) commit();
		return ok;
	}

	/**
	 * Get the index key for a file stored in the documents tree.
	 * The key starts with the name of the parent directory of all'
	 * the MIRCdocument directories. It is typically of the form:
	 * <tt>docs/yyyymmddhhsssss/MIRCdocument.xml</tt>, but it is possible for
	 * additional levels of directories to occur below <tt>docs</tt> and
	 * above <tt>yyyymmddhhsssss</tt>.
	 * @param file a file path.
	 * @return the index entry for the file, or the empty string
	 * if the file is not in the tree for this index.
	 */
	public String getKey(File file) {
		String docsPath = documentsDir.getParentFile().getAbsolutePath();
		int n = docsPath.length();
		String key = file.getAbsolutePath();
		if (key.startsWith(docsPath)) return fixPath( key.substring(n+1) );
		return "";
	}

	/**
	 * Fix a path which (for backward compatibility) may contain
	 * slashes, backslashes, or exclamation points as path separators.
	 * This method forces all separators to be forward slashes.
	 * @param path a file path.
	 * @return the trimmed path, with backslashes and exclamation
	 * points replaced by forward slashes.
	 */
	public static String fixPath(String path) {
		path = path.replaceAll("[!\\\\]+","/").trim();
		return path;
	}

	//Get an ID for a MIRCdocument identified by a specified path.
	//If no document appears in the index for the path, then create
	//a new ID and update the tables to reflect the new ID.
	private synchronized Integer getIDForPath(String path) throws Exception {
		try {
		path = fixPath(path);
		Integer id = (Integer)pathToID.get(path);
		if (id == null) {
			//get the next available ID
			id = (Integer)pathToID.get("__last");
			if (id == null) id = new Integer(0);
			else id = new Integer( id.intValue() + 1 );
			pathToID.put("__last", id);

			//store the path against the ID
			idToPath.put(id, path);

			//store the id against the path
			pathToID.put(path, id);
		}
		return id;
		}
		catch (Exception ex) { logger.warn("getIDForPath:",ex); throw ex; }
	}

	//Get all the text in a node and its children.
	//Note, to preserve word boundaries, spaces are
	//inserted between text nodes.
	private String getText(Node node) {
		StringBuffer sb = new StringBuffer();
		appendTextNodes(sb, node);
		return sb.toString();
	}

	//Get all the text in a nodelist
	private String getText(NodeList nl) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<nl.getLength(); i++) {
			appendTextNodes(sb, nl.item(i));
		}
		return sb.toString();
	}

	//Add the contents of all the text nodes starting at
	//a specified node to a StringBuffer.
	private void appendTextNodes(StringBuffer sb, Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Node child = ((Element)node).getFirstChild();
			while (child != null) {
				appendTextNodes(sb, child);
				child = child.getNextSibling();
			}
		}
		else if (node.getNodeType() == Node.TEXT_NODE) {
			sb.append(" " + node.getTextContent());
		}
	}

	static class Unfragmented extends Hashtable<String, HashSet<String>> {
		public Unfragmented() {
			super();
			this.put("patient", getSet(new String[] {"male", "female"}));
		}
		private HashSet<String> getSet(String[] words) {
			HashSet<String> set = new HashSet<String>();
			for (String word : words) {
				set.add(word);
			}
			return set;
		}
	}

	/**
	 * Log the state of the index.
	 * @param title a heading for the section of the log containing the state.
	 */
	public void logState(String title) {
		logger.warn("===========================================================================");
		logger.warn("MircIndex State: "+title);
		logger.warn("---------------------------------------------------------------------------");
		try {
			logger.warn("pathToID:");
			logger.warn("--------");
			FastIterator fit = pathToID.keys();
			String path;
			while ((path = (String)fit.next()) != null) {
				Integer id = (Integer)pathToID.get(path);
				logger.warn("..."+path+": "+id.toString());
			}
		}
		catch (Exception ex) { logger.warn("!!!Exception caught in reading pathToID", ex); }
		logger.warn("-------------------------------------");
		try {
			logger.warn("idToPath:");
			logger.warn("--------");
			FastIterator fit = idToPath.keys();
			Integer id;
			while ((id = (Integer)fit.next()) != null) {
				String path = (String)idToPath.get(id);
				logger.warn("..."+id.toString()+": "+path);
			}
		}
		catch (Exception ex) { logger.warn("!!!Exception caught in reading idToPath", ex); }

		logger.warn("-------------------------------------");
		try {
			logger.warn("idToMIE:");
			logger.warn("-------");
			FastIterator fit = idToPath.keys();
			Integer id;
			while ((id = (Integer)fit.next()) != null) {
				IndexEntry mie = (IndexEntry)idToMIE.get(id);
				logger.warn("..."+id.toString()+": "+mie.md.getAttribute("path"));
			}
		}
		catch (Exception ex) { logger.warn("!!!Exception caught in reading idToMIE", ex); }

		logger.warn("-------------------------------------");
		logger.warn("MircIndexDatabases:");
		logger.warn("------------------");
		logger.warn("...freetext: "+freetext.getNumberOfWords());
		for (String name : fields.keySet()) {
			IndexDatabase db = fields.get(name);
			logger.warn("..."+name+": "+db.getNumberOfWords());
		}
		logger.warn("===========================================================================");
	}
}


/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import jdbm.RecordManager;
import jdbm.btree.BTree;
import jdbm.helper.FastIterator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import org.apache.log4j.Logger;
import org.rsna.util.Cache;
import org.rsna.util.FileUtil;
import org.rsna.util.JdbmUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Encapsulates an index of RadLex terms.
 */
public class RadLexIndex {

	static final Logger logger = Logger.getLogger(RadLexIndex.class);

	private static RecordManager recman = null;
	private static final String indexName = "RadLexIndex";
	private static final String xmlName = "radlex.xml";
	private static final String xmlResource = "mirc/"+xmlName;
	private static final String radlexTreeName = "radlex";
	private static BTree index = null;
	private static boolean busy = false;

	/**
	 * Load the RadLex index from the JDBM files,
	 * creating the JDBM files if necessary.
	 */
	public static synchronized void loadIndex(File dir) {
		RadLexIndexLoader loader = new RadLexIndexLoader(dir);
		loader.start();
	}

	static class RadLexIndexLoader extends Thread {
		File dir;
		public RadLexIndexLoader(File dir) {
			super("RadLexIndexLoader");
			this.dir = dir;
		}
		public void run() {
			if (recman == null) {
				try {
					File libraries = new File("libraries");
					File jarFile = new File(libraries, "MIRC.jar");
					long jarFileLM = jarFile.lastModified();
					File dbFile = new File(dir, indexName + ".db");
					File lgFile = new File(dir, indexName + ".lg");

					if ( !dbFile.exists() || !lgFile.exists()
							|| (jarFileLM > dbFile.lastModified())
								|| (jarFileLM > lgFile.lastModified()) ) {
						createIndex(dir);
					}
					else {
						File indexFile = new File(dir, indexName);
						recman = JdbmUtil.getRecordManager(indexFile.getAbsolutePath());
						index = JdbmUtil.getBTree(recman, radlexTreeName);
						if (index.size() == 0) createIndex(dir);
					}
				}
				catch (Exception ignore) { }
			}
		}
	}

	/**
	 * Commit changes and close the index.
	 * No errors are reported and no operations
	 * are available after this call without calling
	 * loadIndex.
	 */
	public static synchronized void close() {
		if (recman != null) {
			try {
				recman.commit();
				recman.close();
				recman = null;
				index = null;
			}
			catch (Exception ignore) { }
		}
	}

	/**
	 * Get the array of terms indexed by a specified key.
	 * @param key the first word of the term
	 * @return the array of terms which have the specified key,
	 * arranged order from longest to shortest, or null if
	 * no term exists for the specified key.
	 */
	public static synchronized Term[] getTerms(String key) {
		if (index != null) {
			try { return (Term[])index.find(key.toLowerCase()); }
			catch (Exception ex) { }
		}
		return null;
	}

	/**
	 * Get an XML Element containing all the terms in the index
	 * that start with a word which starts with the supplied string.
	 * @param keyString the beginning of the first word of the matching terms.
	 * @return an XML element containing suggested terms matching the keyString.
	 */
	public static synchronized Element getSuggestedTerms(String keyString) {
		if (index != null) {
			Tuple tuple = new Tuple();
			keyString = keyString.toLowerCase();
			try {
				Document doc = XmlUtil.getDocument();
				Element root = doc.createElement("RadLexTerms");
				doc.appendChild(root);
				TupleBrowser browser = index.browse(keyString);
				while ( browser.getNext(tuple) && ((String)tuple.getKey()).startsWith(keyString) ) {
					Term[] terms = (Term[])tuple.getValue();
					for (int i=0; i<terms.length; i++) {
						Element term = doc.createElement("term");
						term.setAttribute("id", terms[i].id);
						term.appendChild(doc.createTextNode(terms[i].text));
						root.appendChild(term);
					}
				}
				return root;
			}
			catch (Exception ex) { }
		}
		return null;
	}

	// Create the RadLex index JDBM files from the radlex.xml file.
	// dir is the directory in which to create the RadLex index.
	private static synchronized void createIndex(File dir) {

		logger.info("RadLex index rebuild started");
		File indexFile = new File(dir, indexName);
		String filename = indexFile.getAbsolutePath();

		//First close and delete the existing database if it is present
		close();
		(new File(filename + ".db")).delete();
		(new File(filename + ".lg")).delete();

		int termCount = 0;
		int radCount = 0;
		int obsCount = 0;
		int synCount = 0;
		try {
			//Now get a new Record manager and create the (empty) index.
			recman = JdbmUtil.getRecordManager(filename);
			index = JdbmUtil.getBTree(recman, radlexTreeName);

			//Parse the XML file
			File xmlFile = Cache.getInstance().getFile(xmlResource);
			InputStream is = FileUtil.getStream( xmlFile, xmlResource );
			if (is == null) {
				logger.warn("Unable to get InputStream for "+xmlResource);
				logger.warn("...RadLex XML resource: "+xmlResource);
				logger.warn("...RadLex XML file: "+xmlFile);
			}
			Document radlex = XmlUtil.getDocument( is );

			//Put the terms in the index
			Element root = radlex.getDocumentElement();
			Node child = root.getFirstChild();
			while (child != null) {
				if ((child.getNodeType() == Node.ELEMENT_NODE) && child.getNodeName().equals("term")) {
					Element term = (Element)child;
					String id = term.getAttribute("id");
					String text = term.getTextContent().trim();
					if (!id.equals("") && !text.equals("")) {
						addTerm( new Term(id, text) );
						termCount++;
						String type = term.getAttribute("type");
						if (type.equals("OBS")) obsCount++;
						else if (type.equals("SYN")) synCount++;
						else radCount++;
					}
				}
				child = child.getNextSibling();
			}
		}
		catch (Exception quit) {
			logger.warn("RadLex index rebuild failed.", quit);
		}
		finally {
			try {
				//Commit and close the database to force the database into a clean state.
				close();

				//Now reopen everything
				recman = JdbmUtil.getRecordManager(filename);
				index = JdbmUtil.getBTree(recman, radlexTreeName);
			}
			catch (Exception ignore) { }
		}
		logger.info("RadLex index rebuild complete ("+index.size()+" index entries)");
		logger.info("...Total indexed terms: "+termCount);
		logger.info("...RadLex terms:        "+radCount);
		logger.info("...Synonyms:            "+synCount);
		logger.info("...Obsolete terms:      "+obsCount);
	}

	//Add a term to the index
	private static void addTerm(Term term) {
		try {
			String key = term.getKey();
			Term[] terms = (Term[])index.find(key);
			if (terms == null) {
				terms = new Term[] { term };
				index.insert(key, terms, true);
			}
			else {
				Term[] more = new Term[ terms.length + 1 ];
				for (int i=0; i<terms.length; i++) more[i] = terms[i];
				more[ terms.length ] = term;
				Arrays.sort(more);
				index.insert(key, more, true);
			}
		}
		catch (Exception skip) { }
	}

	static class StringComparator  implements Comparator<String>, Serializable {
		public static final long serialVersionUID = 1;
		public StringComparator() { }
		public int compare( String s1, String s2 ) {
			return s1.compareTo(s2);
		}
		public boolean equals(Object obj) {
			return this.equals(obj);
		}
	}

}

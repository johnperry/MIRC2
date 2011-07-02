/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.io.*;
import java.util.*;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;
import jdbm.btree.BTree;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import org.apache.log4j.Logger;
import org.rsna.util.JdbmUtil;
import org.w3c.dom.*;

public class IndexDatabase {

	static final Logger logger = Logger.getLogger(IndexDatabase.class);

	RecordManager recman;
	String name;
	int minWordSize = 2;
	int minSegmentSize = 4;

	/*
	text:
		The BTree of word fragments in the index. The key is the text of the word fragment.
		The value for an entry is the HashSet of document IDs for documents that contain
		the word fragment.
	*/
	BTree text;

	/*
	docs:
		The HTree of documents in the index. The key is the Integer ID of the document.
		The value for an entry is the HashSet of word fragments contained in the document.
		The purpose of this table is to make it easy to remove a document from the index.
	*/
	HTree docs;

	/*
	unfragmented:
		The set of words for which fragments are not to be indexed.
	*/
	HashSet<String> unfragmented;

	/**
	 * Construct a database indexing a single MIRCquery field.
	 * @param recman the JDBM RecordManager for the database.
	 * @param name the name of the query field
	 * @param unfragmented the set of words for which fragments (substrings of the word) are not to be indexed.
	 */
	public IndexDatabase(
						RecordManager recman,
						String name,
						HashSet<String> unfragmented) throws Exception {
		this.recman = recman;
		this.name = name;
		this.unfragmented = unfragmented;
		text = JdbmUtil.getBTree(recman, name);
		docs = JdbmUtil.getHTree(recman, name+"_docs");
	}

	/**
	 * Get the number of words and word fragments in the index.
	 */
	public int getNumberOfWords() {
		return text.size();
	}

	/**
	 * Get a HashSet of all the document IDs in the index.
	 */
	public HashSet<Integer> getAllIDs() {
		HashSet<Integer> ids = new HashSet<Integer>();
		try {
			FastIterator keys = docs.keys();
			Integer id;
			while ( (id=(Integer)keys.next()) != null ) ids.add(id);
		}
		catch (Exception ex) { return new HashSet<Integer>(); }
		return ids;
	}

	/**
	 * Get a HashSet of all the document IDs for documents
	 * containing a string. The string is split into words,
	 * and all the words must be present in a document for it
	 * to be included in the result HashSet.
	 */
	public HashSet<Integer> getIDsForQueryString(String s) {
		HashSet<Integer> results = null;
		HashSet<Integer> next;
		s = s.replaceAll("\\s+", " ");
		String[] words = s.split(" ");
		for (String w : words) {
			w = fixWord(w);
			if (w.length() >= minWordSize) {
				if (results == null) results = getIDsForFragment(w);
				else {
					next = getIDsForFragment(w);
					results = intersection(results, next);
				}
			}
		}
		if (results == null) results = new HashSet<Integer>();
		return results;
	}

	/**
	 * Remove a document from the index.
	 */
	public boolean removeDoc(Integer id) {
		try {
			HashSet<String> fragments = (HashSet<String>)docs.get(id);
			Tuple tuple = new Tuple();
			if (fragments != null)  {
				for (String f : fragments) {
					HashSet<Integer> docsContainingFragment = (HashSet<Integer>)text.find(f);
					if (docsContainingFragment != null) {
						docsContainingFragment.remove(id);
						text.insert(f, docsContainingFragment, true);
					}
				}
			}
			docs.remove(id);
			return true;
		}
		catch (Exception skip) { return false; }
	}

	/**
	 * Index a specified string, splitting the string into words and
	 * indexing all the word fragments.
	 * @param id the ID of the document
	 * @param s the string to be indexed.
	 * @return true if the indexing was successful; false if
	 * not (indicating an IO error when accessing the database).
	 */
	public boolean indexString(Integer id, String s) {
		try {
			HashSet<String> fragments = new HashSet<String>();
			s = s.trim();
			s = s.replaceAll("\\s+", " ");
			String[] words = s.split(" ");
			for (String w : words) {
				w = fixWord(w);
				if (w.length() >= minWordSize) {
					fragments.add(w);
					if ((unfragmented == null) || !unfragmented.contains(w)) {
						while (w.length() > minSegmentSize) {
							w = w.substring(1);
							fragments.add( fixWord(w) );
						}
					}
				}
			}
			indexSet(id, fragments);
			return true;
		}
		catch (Exception failed) { return false; }
	}

	//Add the document to the index for all the
	//fragments contained in a HashSet<String>.
	private void indexSet(Integer id, HashSet<String> fragments) throws Exception {
		HashSet<String> fragmentsInDoc = (HashSet<String>)docs.get(id);
		if (fragmentsInDoc == null) fragmentsInDoc = new HashSet<String>();

		for (String fragment : fragments) {
			HashSet<Integer> docsContainingFragment = (HashSet<Integer>)text.find(fragment);
			if (docsContainingFragment == null) docsContainingFragment = new HashSet<Integer>();
			docsContainingFragment.add(id);
			text.insert(fragment, docsContainingFragment, true);
			fragmentsInDoc.add(fragment);
		}

		docs.put(id, fragmentsInDoc);
	}

	/**
	 * Get a HashSet containing the document IDs for all
	 * the documents that contain a specified word fragment.
	 */
	public HashSet<Integer> getIDsForFragment(String fragment) {
		HashSet<Integer> set = new HashSet<Integer>();
		try {
			Tuple tuple = new Tuple();
			TupleBrowser browser = text.browse(fragment);
			while (browser.getNext(tuple) && ((String)tuple.getKey()).startsWith(fragment)) {
				set = union( (HashSet<Integer>)tuple.getValue(), set );
			}
			return set;
		}
		catch (Exception ex) { return new HashSet<Integer>(); }
	}

	/**
	 * Modify a string to prepare it for indexing.
	 * The modification includes:
	 * <ol><li>removing all leading or trailing characters in this set:  '"_*-.,;:/+-
	 * <li>trimming
	 * <li>converting to lower case
	 * </ol>
	 * @return the modified string.
	 */
	public static String fixWord(String w) {
		w = w.replaceAll("^['\"_*-,;:/+-\\.]+", "");
		w = w.replaceAll("['\"_*-,;:/+-\\.]+$", "");
		w = w.trim().toLowerCase();
		return w;
	}

	/**
	 * Create a new HashSet that is the intersection of two HashSets.
	 * Neither of the two input HashSets are modified. If either of the
	 * input HashSets are null, an empty HashSet is returned.
	 */
	public static HashSet<Integer> intersection(HashSet<Integer> s1, HashSet<Integer> s2) {
		HashSet<Integer> temp;
		if ((s1 == null) || (s2 == null)) return new HashSet<Integer>();
		if (s1.size() < s2.size()) {
			temp = (HashSet<Integer>)s1.clone();
			temp.retainAll(s2);
		}
		else {
			temp = (HashSet<Integer>)s2.clone();
			temp.retainAll(s1);
		}
		return temp;
	}

	/**
	 * Create a new HashSet that is the union of two HashSets.
	 * Neither of the two input HashSets are modified. If either of the
	 * input HashSets are null, an empty HashSet is returned.
	 */
	public static HashSet<Integer> union(HashSet<Integer> s1, HashSet<Integer> s2) {
		HashSet<Integer> temp;
		if ((s1 == null) || (s2 == null)) return new HashSet<Integer>();
		if (s1.size() < s2.size()) {
			temp = (HashSet<Integer>)s2.clone();
			temp.addAll(s1);
		}
		else {
			temp = (HashSet<Integer>)s1.clone();
			temp.addAll(s2);
		}
		return temp;
	}

}
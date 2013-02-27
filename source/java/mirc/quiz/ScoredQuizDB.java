/*---------------------------------------------------------------
 *  Copyright 2013 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.quiz;

import java.io.File;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import jdbm.RecordManager;
import org.apache.log4j.Logger;
import org.rsna.util.JdbmUtil;

/**
 * Encapsulates a database to track scored quizzes.
 */
public class ScoredQuizDB {

	static final Logger logger = Logger.getLogger(ScoredQuizDB.class);

	private static ScoredQuizDB scoredQuizDB = null;

	private static File dir = null;
	private static RecordManager recman = null;
	private static final String databaseName = "quiz";
	private static final String qTreeName = "questions";
	private static HTree qTree = null;

	/**
	 * Protected constructor.
	 * @param dir the directory in which the database is located.
	 */
	protected ScoredQuizDB(File dir) {
		this.dir = dir;
		File databaseFile = new File(dir, databaseName);
		recman = JdbmUtil.getRecordManager(databaseFile.getAbsolutePath());
		qTree = JdbmUtil.getHTree(recman, qTreeName);
	}

	/**
	 * Load the singleton instance of the database.
	 * @param dir the directory in which the database is located.
	 */
	public static ScoredQuizDB load(File dir) {
		scoredQuizDB = new ScoredQuizDB(dir);
		return scoredQuizDB;
	}

	/**
	 * Get the singleton instance of the database.
	 * This method is intended for normal classes.
	 */
	public static synchronized ScoredQuizDB getInstance() {
		return scoredQuizDB;
	}

	/**
	 * Get the database entry for a specified question, creating it if necessary.
	 * @param id the ID of the question to retrieve.
	 */
	public synchronized Question get(String id) {
		Question entry = null;
		try {
			entry = (Question)qTree.get(id);
			if (entry == null) {
				entry = new Question(id);
				qTree.put(id, entry);
			}
		}
		catch (Exception ignore) { logger.warn("Unable to create Question",ignore); }
		return entry;
	}

	/**
	 * Put an entry in the database.
	 * @param entry the Question to store
	 */
	public synchronized void put(Question entry) {
		try {
			qTree.put(entry.getID(), entry);
			recman.commit();
		}
		catch (Exception ignore) { }
	}

	/**
	 * Commit changes and close the database.
	 * No errors are reported and no operations
	 * are available after this call.
	 */
	public static synchronized void close() {
		JdbmUtil.close(recman);
		recman = null;
		qTree = null;
	}

}

/*---------------------------------------------------------------
 *  Copyright 2013 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.quiz;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Hashtable;
import org.apache.log4j.Logger;

/**
 * Encapsulates a single scored question, containing all the respondents' answers.
 */
public class Question implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(Question.class);

	String id;
	boolean isClosed = false;
	Hashtable<String,Answer> answers;

	/**
	 * Create a ScoredQuiz Question.
	 * @param id the id of the quiz
	 */
	public Question(String id) {
		this.id = id;
		this.isClosed = false;
		this.answers = new Hashtable<String,Answer>();
	}

	/**
	 * Get the id of this question.
	 */
	public String getID() {
		return id;
	}

	/**
	 * Determine whether this question is closed (meaning
	 * that no further changes to user's answers will be
	 * accepted).
	 */
	public boolean isClosed() {
		return isClosed;
	}

	/**
	 * Get the respondent IDs for all respondents to this quiz.
	 */
	public String[] getRespondentIDs() {
		return answers.keySet().toArray(new String[answers.size()]);
	}

	/**
	 * Get the Answer for a specific respondent.
	 * @param respondentID the id of the respondent whose Answer is to be retrieved.
	 * @return the respondent's answer to the question, or null if no answer has been submitted.
	 */
	public Answer get(String respondentID) {
		return answers.get(respondentID);
	}

	/**
	 * Add the Answer for a specific respondent.
	 * @param respondentID the id of the respondent whose answer is to be stored.
	 * @param answer the respondent's answer to the question.
	 */
	public void put(String respondentID, Answer answer) {
		answers.put(respondentID, answer);
		ScoredQuizDB.getInstance().put(this);
	}

}

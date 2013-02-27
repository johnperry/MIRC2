/*---------------------------------------------------------------
 *  Copyright 2013 by the Radiological Society of North America
 *
 *  This source software is released under the terms of the
 *  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
 *----------------------------------------------------------------*/

package mirc.quiz;

import java.io.Serializable;
import org.apache.log4j.Logger;

/**
 * Encapsulates an answer submitted by a respondent to a quiz question.
 */
public class Answer implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(Answer.class);

	String value;
	int score = 0;

	/**
	 * Create an Answer, zeroing the score.
	 * @param value the submitted answer
	 */
	public Answer(String value) {
		this.value = value;
		this.score = 0;
	}

	/**
	 * Get the value.
	 */
	public String getValue() {
		return ((value != null) ? value : "");
	}

	/**
	 * Get the score.
	 */
	public int getScore() {
		return score;
	}

	/**
	 * Assign the score.
	 * @param score the number of points given to the answer.
	 */
	public void setScore(int score) {
		this.score = score;
	}

}

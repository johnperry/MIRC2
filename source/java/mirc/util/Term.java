/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.Serializable;

public class Term implements Comparable, Serializable {
	public static final long serialVersionUID = 1;
	public String id;
	public String text;

	/**
	 * Construct a term, associating a string with an ID.
	 * @param id the ID of the term
	 * @param text the text of the term
	 */
	public Term(String id, String text) {
		this.id = id.trim();
		this.text = text.trim();
	}

	/**
	 * Compare a term with this term, sorting in reverse order by length
	 * (longest terms first).
	 * @param term the Term to compare
	 * @return positive value if the text of this term is longer than the
	 * text of the supplied term.
	 */
	public int compareTo(Object term) {
		if (term instanceof Term) {
			return ((Term)term).text.length() - this.text.length();
		}
		return 0;
	}

	/**
	 * Get the key used by this term in the HTree.
	 * @return the first word of the term text (all the characters
	 * up to the first space.
	 */
	public String getKey() {
		int len = text.length();
		//Find the first letter
		int k = 0;
		while ((k < len) && !Character.isLetter(text.charAt(k))) k++;
		//Find the next non-letter
		int kk = k;
		while ((kk < len) && Character.isLetter(text.charAt(kk))) kk++;
		if (k != kk) return text.substring(k,kk).toLowerCase();
		else return text.toLowerCase();
	}

	/**
	 * Determine whether this term starts at position k in a string.
	 * @return true if the text of this term starts at the specified
	 * point in the supplied string; false otherwise. The comparison
	 * is done ignoring case. Even if the term matches, if the next
	 * character in the string is a letter, the match returns false
	 * in order to avoid matching a partial word. (This prevents the
	 * term "is a" from matching a string containing "is always".
	 */
	public boolean matches(String string, int k) {
		int len = text.length();
		if ((string.length() - k - len) < 0) return false;
		if (!string.substring(k,k+len).equalsIgnoreCase(text)) return false;
		if (k+len == string.length()) return true;
		if (!Character.isLetter(string.charAt(k+len))) return true;
		return false;
	}

	/**
	 * List this term as a string.
	 * @return the contents of the object in string form.
	 */
	public String toString() {
		return "["+id+"] "+getKey()+": \""+text+"\"";
	}

}

/*---------------------------------------------------------------
*  Copyright 2012 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.activity;

import java.io.Serializable;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Encapsulates an index to track activities of a specific type.
 */
public class Tracker implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(Tracker.class);

	String type;
	LinkedList<Counter> counts;

	static final int timeDepth = 30;
	static final long oneDay = 24 * 60 * 60 * 1000;

	public Tracker(String type) {
		this.type = type;
		this.counts = new LinkedList<Counter>();
	}

	/**
	 * Add one to the counter for the current day.
	 */
	public synchronized void increment() {
		removeOldEntries();
		long today = System.currentTimeMillis() / oneDay;
		if ( (counts.size() == 0) || !counts.getLast().isSince(today) ) {
			counts.add(new Counter(today));
		}
		counts.getLast().increment();
	}

	//Remove the entries in the counts list that are older than the timeDepth
	private void removeOldEntries() {
		long today = System.currentTimeMillis() / oneDay;
		long earliest = today - timeDepth;
		Counter c;
		while ( (counts.size() > 0) && !counts.getFirst().isSince(earliest) ) {
			counts.removeFirst();
		}
	}

	/**
	 * Get the time depth of this Tracker in days.
	 */
	public int getTimeDepth() {
		return timeDepth;
	}

	/**
	 * Get the name of the activity type of this Tracker.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the number of tracked events that have occurred in the recent past.
	 * @param days the number of days backward from the present to include in the total.
	 */
	public synchronized int getTotal(int days) {
		removeOldEntries();
		long today = System.currentTimeMillis() / oneDay;
		long earliest = today - days;
		int total = 0;
		for (Counter c : counts) {
			if (c.isSince(earliest)) total += c.getCount();
		}
		return total;
	}

}

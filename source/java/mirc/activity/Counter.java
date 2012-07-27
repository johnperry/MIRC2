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
 * A class to encapsulate a count of activities on a specified day.
 */
public class Counter implements Serializable {

	public static final long serialVersionUID = 1;
	static final Logger logger = Logger.getLogger(Counter.class);

	int count;
	long day;

	public Counter(long day) {
		this.day = day;
		this.count = 0;
	}

	public synchronized int increment() {
		return (++count);
	}

	public synchronized boolean isSince(long day) {
		return (this.day >= day);
	}

	public synchronized int getCount() {
		return count;
	}

	public synchronized long getDay() {
		return day;
	}
}

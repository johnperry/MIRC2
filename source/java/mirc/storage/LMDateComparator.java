/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.storage;

import java.util.*;

/**
 * A Comparator for sorting IndexEntry objects.
 */
public class LMDateComparator implements Comparator {

	static final int up = 1;
	static final int down = -1;
	int dir = down;

	/**
	 * Create a reverse order Comparator for lmDate values.
	 */
	public LMDateComparator() {
		this(down);
	}

	/**
	 * Create a specified order Comparator for lmDate values.
	 */
	public LMDateComparator(int direction) {
		if (direction >= 0) dir = up;
		else dir = down;
	}

	/**
	 * Compare.
	 */
	public int compare(Object o1, Object o2) {
		if ( (o1 instanceof IndexEntry) && (o2 instanceof IndexEntry)) {
			long d1 = ((IndexEntry)o1).lmdate;
			long d2 = ((IndexEntry)o2).lmdate;
			return dir * ( (d1>d2) ? 1 : ((d1<d2) ? -1 : 0) );
		}
		else return 0;
	}

}
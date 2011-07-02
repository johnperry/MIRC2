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
public class TitleComparator implements Comparator {

	static final int up = 1;
	static final int down = -1;
	int dir = down;

	/**
	 * Create a forward order Comparator for title values.
	 */
	public TitleComparator() {
		this(up);
	}

	/**
	 * Create a specified order Comparator for title values.
	 */
	public TitleComparator(int direction) {
		if (direction >= 0) dir = up;
		else dir = down;
	}

	/**
	 * Compare.
	 */
	public int compare(Object o1, Object o2) {
		if ( (o1 instanceof IndexEntry) && (o2 instanceof IndexEntry)) {
			String o1title = ((IndexEntry)o1).title;
			String o2title = ((IndexEntry)o2).title;
			return o1title.compareTo( o2title ) * dir;
		}
		else return 0;
	}

}
/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.download;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;
import jdbm.btree.BTree;
import jdbm.RecordManager;
import org.apache.log4j.Logger;
import org.rsna.util.JdbmUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class to provide singleton access to a database of download entries.
 */
public class DownloadDB {

	static final Logger logger = Logger.getLogger(DownloadDB.class);

	private static DownloadDB downloadDB = null;

	private static File dir = null;
	private static RecordManager recman = null;
	private static final String databaseName = "downloads";
	private static final String downloadsName = "downloads";
	private static BTree downloads = null;

	/**
	 * Protected constructor.
	 * @param dir the directory in which the database is located.
	 */
	protected DownloadDB(File dir) {
		this.dir = dir;
		File databaseFile = new File(dir, databaseName);
		recman = JdbmUtil.getRecordManager(databaseFile.getAbsolutePath());
		downloads = JdbmUtil.getBTree(recman, downloadsName);
	}

	/**
	 * Load the singleton instance of the database.
	 * This method is intended to be called by the init method
	 * of the DownloadServlet for the initial loading of the database
	 * when MIRC starts.
	 * @param dir the directory in which the database is located.
	 */
	public static DownloadDB load(File dir) {
		downloadDB = new DownloadDB(dir);
		return downloadDB;
	}

	/**
	 * Get the singleton instance of the preferences database.
	 * This method is intended for normal classes.
	 */
	public static DownloadDB getInstance() {
		return downloadDB;
	}

	/**
	 * Make an entry in the database for the current time.
	 * @param file the downloaded file.
	 * @param version the version of the downloaded file.
	 * @param ip the IP address of the client.
	 * @param email the email address of the client.
	 * @param pname the name of the person making the request
	 * @param iname the institution name
	 * @param interest personal or institutional
	 * @param sitetype public or private
	 */
	public synchronized void insert(File file,
									String version,
									String ip,
									String email,
									String pname,
									String iname,
									String interest,
									String sitetype
									) {
		try {
			if (downloads != null) {
				Long time = new Long( System.currentTimeMillis() );
				Entry entry = new Entry( file.getName(), version, ip, email, pname, iname, interest, sitetype );
				downloads.insert( time, entry, true );
				recman.commit();
			}
		}
		catch (Exception skip) { logger.warn("unable to insert into downloads",skip); }
	}

	/**
	 * Get an XML Document containing the contents of the database.
	 */
	public synchronized Document getXML() {
		Document doc = null;
		try {
			doc = XmlUtil.getDocument();
			Element root = doc.createElement("report");
			doc.appendChild(root);
			Element summaryElement = doc.createElement("summary");
			root.appendChild(summaryElement);
			Element downloadsElement = doc.createElement("downloads");
			root.appendChild(downloadsElement);

			Counter counter = new Counter();
			Tuple tuple = new Tuple();
			TupleBrowser browser = downloads.browse();
			while (browser.getNext(tuple)) {
				Long time = (Long)tuple.getKey();
				Entry entry = (Entry)tuple.getValue();
				Element dl = doc.createElement("download");
				dl.setAttribute("time", time.toString());
				dl.setAttribute("date", StringUtil.getDateTime(time, " "));
				dl.setAttribute("name", entry.name);
				dl.setAttribute("build", entry.build);
				dl.setAttribute("ip", entry.ip);
				dl.setAttribute("email", entry.email);
				dl.setAttribute("pname", entry.pname);
				dl.setAttribute("iname", entry.iname);
				dl.setAttribute("interest", entry.interest);
				dl.setAttribute("sitetype", entry.sitetype);

				downloadsElement.appendChild(dl);
				counter.add(entry.name);
			}
			String[] names = counter.getNames();
			for (String name : names) {
				Element file = doc.createElement("file");
				file.setAttribute("name", name);
				file.setAttribute("downloads", counter.get(name).toString());
				summaryElement.appendChild(file);
			}
		}
		catch (Exception skip) { }
		return doc;
	}

	class Counter extends Hashtable<String, Integer> {
		public Counter() {
			super();
		}
		public void add(String s) {
			Integer count = get(s);
			if (count == null) count = new Integer(0);
			count = new Integer(1 + count.intValue());
			put(s, count);
		}
		public String[] getNames() {
			String[] names = keySet().toArray(new String[size()]);
			Arrays.sort(names);
			return names;
		}
	}

	/**
	 * Commit changes and close the database.
	 * No errors are reported and no operations
	 * are available after this call.
	 */
	public static synchronized void close() {
		JdbmUtil.close(recman);
		recman = null;
		downloads = null;
	}
}
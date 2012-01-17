/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.net.URL;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;
import org.rsna.ctp.Configuration;
import org.rsna.server.Users;
import org.rsna.util.FileUtil;
import org.rsna.util.JarUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.rsna.ctp.pipeline.PipelineStage;
import org.rsna.ctp.stdstages.DicomAnonymizer;

/**
 * A class to encapsulate the MIRC configuration.
 */
public class MircConfig {

	static volatile File dir = null;
	static volatile File mirc = null;
	static volatile Document mircXML = null;
	static volatile Element mircRoot;
	static volatile int timeout = 10;
	static volatile String siteurl = null;
	volatile File TEMP = null;

	protected static volatile MircConfig mircConfigInstance = null;

	static final Logger logger = Logger.getLogger(MircConfig.class);

	public static volatile Document enumeratedValues = null;
	public static volatile Document speciesValues = null;
	public static volatile Document mircqueryXSL = null;

	static volatile DicomAnonymizer fsDicomAnonymizer = null;
	static volatile Hashtable<String,Element> libraries;

	/**
	 * Private singleton constructor; this class must be
	 * instantiated using the MircConfig.load(File) method.
	 */
	private MircConfig(File mirc) {
		this.mirc = mirc;
		dir = mirc.getParentFile();

		//Delete the old temp directory and create a new one.
		TEMP = new File(dir, "TEMP");
		FileUtil.deleteAll(TEMP);
		TEMP.mkdirs();

		//Load the MIRC configuration
		loadXML();
		setMastheadHeight();
		loadXMLObjects();
	}

	/**
	 * Get the MircConfig instance.
	 */
	public static synchronized MircConfig getInstance() {
		return mircConfigInstance;
	}

	/**
	 * Load the MircConfig instance.
	 * @param mirc the mirc.xml file.
	 * @return the new MircConfig instance
	 */
	public static synchronized MircConfig load(File mirc) {
		mircConfigInstance = new MircConfig(mirc);
		return mircConfigInstance;
	}

	/**
	 * Reload the components.
	 */
	public synchronized void reload() {
		loadXML();
		setMastheadHeight();
		reloadXMLObjects();
	}

	//Load the mirc.xml XML document
	private static void loadXML() {
		try {
			mircXML = XmlUtil.getDocument(mirc);
			mircRoot = mircXML.getDocumentElement();
			siteurl = getSiteURL();

			//Get the version information
			File jar = new File("libraries");
			jar = new File(jar, "MIRC.jar");
			Hashtable<String,String> manifest = JarUtil.getManifestAttributes(jar);
			mircRoot.setAttribute("date", manifest.get("Date"));
			mircRoot.setAttribute("version", manifest.get("Version"));

			//Get the timeout
			try { timeout = Integer.parseInt( mircRoot.getAttribute("timeout") ); }
			catch (Exception useDefault) { }
			timeout = ((timeout>1) && (timeout<200)) ? timeout : 10;

			//Build the libraries table and identify the local servers
			libraries = new Hashtable<String, Element>();
			Node child = mircRoot.getFirstChild();
			while (child != null) {
				if ((child instanceof Element) && child.getNodeName().equals("Libraries")) {
					NodeList nl = ((Element)child).getElementsByTagName("Library");
					for (int i=0; i<nl.getLength(); i++) {
						Element lib = (Element)nl.item(i);
						String address = lib.getAttribute("address").trim();
						lib.setAttribute("local", (isLocal(address) ? "yes" : "no") );
						libraries.put( address, lib );
					}
					break;
				}
				child = child.getNextSibling();
			}
			saveXML();
		}
		catch (Exception ex) {
			logger.warn("Unable to parse the MIRC config file: "+mirc, ex);
			mircXML = null;
		}
	}

	//Set the siteurl from the system IP address if dynamic addressing is enabled.
	private static String getSiteURL() {
		String siteurl = mircRoot.getAttribute("siteurl");
		String oldurl = siteurl;
		boolean dynamic = mircRoot.getAttribute("addresstype").equals("dynamic");
		Configuration ctp = Configuration.getInstance();
		int serverPort = ctp.getServerPort();
		boolean serverSSL = ctp.getServerSSL();
		boolean isNumeric = false;
		boolean isHTTP = true;
		boolean isHTTPS = false;
		int port = 80;

		try {
			URL url = new URL(siteurl);
			String protocol = url.getProtocol();
			String host = url.getHost();
			isNumeric = (host.replaceAll("[\\d\\.\\s]","").trim().length() == 0);
			isHTTP = protocol.equalsIgnoreCase("http");
			isHTTPS = protocol.equalsIgnoreCase("https");

			protocol = "http" + (serverSSL ? "s" : "");
			if (dynamic && isNumeric) {
				siteurl = protocol + "://" + ctp.getIPAddress() + ":" + ctp.getServerPort();
				mircRoot.setAttribute("siteurl", siteurl);
			}
			else if ((serverSSL != isHTTPS) || (serverPort != port)) {
				siteurl = protocol + "://" + host + ":" + ctp.getServerPort();
				mircRoot.setAttribute("siteurl", siteurl);
			}
			logger.info("Site URL: "+siteurl);
		}
		catch (Exception ex) { }
		return siteurl;
	}

	//Load the XML and XSL objects used throughout MIRC
	private static void loadXMLObjects() {
		enumeratedValues	= loadXMLObject("query",	"enumerated-values.xml" );
		speciesValues		= loadXMLObject("query",	"species-values.xml" );
		mircqueryXSL		= loadXMLObject("query",	"MIRCquery.xsl");
	}

	//Reload the XML and XSL objects, only loading the files if
	//they exist. The idea is that only files can change, so there is
	//no point reloading from the jar.
	private static void reloadXMLObjects() {
		enumeratedValues	= reloadXMLObject(enumeratedValues,	"query",	"enumerated-values.xml" );
		speciesValues		= reloadXMLObject(speciesValues,	"query",	"species-values.xml" );
		mircqueryXSL		= reloadXMLObject(mircqueryXSL,		"query",	"MIRCquery.xsl");
	}

	//Get a resource and parse it. If the resource file is missing,
	//load it from the MIRC resources.
	private static Document loadXMLObject(String context, String name) {
		File location = new File(dir, context);
		File file = new File(location, name);
		try { return XmlUtil.getDocument(FileUtil.getStream(file, "/"+context+"/"+name)); }
		catch (Exception ignore) { logger.warn("Unable to parse "+file); return null; }
	}

	//Reload a resource if its file exists; otherwise, return the supplied default object.
	private static Document reloadXMLObject(Document defaultDoc, String context, String name) {
		File location = new File(dir, context);
		File file = new File(location, name);
		if (file.exists()) {
			try { return XmlUtil.getDocument(file); }
			catch (Exception ignore) { logger.warn("Unable to parse "+file); }
		}
		return defaultDoc;
	}

	/**
	 * Save the MIRC configuration XML object.
	 */
	public synchronized void save() {
		saveXML();
	}

	//Static method to save the XML object
	private static synchronized void saveXML() {
		FileUtil.setText(mirc, XmlUtil.toPrettyString( mircXML ));
	}

	/**
	 * Get the MIRC plugin root directory.
	 * @return the MIRC plugin root directory.
	 */
	public synchronized File getRootDirectory() {
		return dir;
	}

	/**
	 * Create a new temporary directory as a child of the TEMP
	 * directory in the MIRC Plugin's root directory.
	 * @return a new child directory of the TEMP directory.
	 */
	public synchronized File createTempDirectory() {
		return FileUtil.createTempDirectory(TEMP);
	}

	/**
	 * Get the MIRC configuration XML object.
	 * @return the MIRC configuration XML DOM object.
	 */
	public synchronized Document getXML() {
		return mircXML;
	}

	//Set the masthead height entity in mirc.xml
	private void setMastheadHeight() {
		try {
			File mhDir = new File(dir, "query");
			mhDir.mkdirs();
			File mhFile = new File(mhDir, mircRoot.getAttribute("masthead"));
			mhFile = FileUtil.getFile(mhFile, "/query/masthead.jpg");
			BufferedImage mh = ImageIO.read(mhFile);
			int height = mh.getHeight();
			mircRoot.setAttribute("mastheadheight", Integer.toString(height));
			saveXML();
		}
		catch (Exception ex) {
			logger.warn("Unable to set the masthead height", ex);
		}
	}

	/**
	 * Get the local address.
	 * @return the URL of Tomcat.
	 */
	public synchronized static String getLocalAddress() {
		return siteurl;
	}

	/**
	 * Get the number of Library elements in the MIRC configuration XML object.
	 * This is the number of Libraries known to the MIRC site.
	 * @return number of Library elements in the MIRC configuration XML object.
	 */
	public synchronized int getNumberOfLibraries() {
		try { return libraries.size(); }
		catch (Exception ex) { return 0; }
	}

	/**
	 * Get the name of the MIRC site.
	 */
	public synchronized String getSiteName() {
		return mircRoot.getAttribute("sitename");
	}

	/**
	 * Get the query timeout in seconds.
	 */
	public synchronized int getQueryTimeout() {
		return timeout;
	}

	/**
	 * Determine whether a URL is local to the MIRC site.
	 * @param url the URL to be tested against the base URL of the Tomcat server.
	 * @return true if the URL is on the same server as the query service.
	 */
	public static boolean isLocal(String url) {
		return ((!url.startsWith("http://") && !url.startsWith("https://")) || url.startsWith(siteurl));
	}

	/**
	 * Set all the primary system parameters.
	 * @param mode the query mode for race/sex/species/breed (rad/vet).
	 * @param sitename the name of the site for display in the masthead.
	 * @param showsitename whether to display the site in the masthead (yes/no).
	 * @param masthead the name of the image to be used for the masthead.
	 * @param showptids whether to display pt name and ID in displayed MIRCdocuments.
	 * @param siteurl the URL of Tomcat (http://ip:port).
	 * @param addresstype whether to obtain the IP address from the OS on startup (dynamic/static).
	 * @param disclaimerurl the URL of the disclaimer page (if blank, no disclaimer is presented).
	 * @param timeout the query timeout in seconds.
	 * @param roles the additional roles defined by the site.
	 */
	public synchronized void setPrimarySystemParameters(
			String mode,
			String sitename,
			String showsitename,
			String masthead,
			String showptids,
			String siteurl,
			String addresstype,
			String disclaimerurl,
			String timeout,
			String roles,
			String ui,
			String popup,
			String downloadenb) {
		mircRoot.setAttribute("mode",mode);
		mircRoot.setAttribute("sitename",sitename);
		mircRoot.setAttribute("showsitename",showsitename);
		mircRoot.setAttribute("masthead",masthead);
		mircRoot.setAttribute("showptids",showptids);
		mircRoot.setAttribute("siteurl",siteurl);
		mircRoot.setAttribute("addresstype",addresstype);
		mircRoot.setAttribute("disclaimerurl",disclaimerurl);
		mircRoot.setAttribute("timeout",timeout);
		mircRoot.setAttribute("roles", roles.trim().replaceAll("[\\s,]+",","));
		mircRoot.setAttribute("UI", ui);
		mircRoot.setAttribute("popup", popup);
		mircRoot.setAttribute("downloadenb", downloadenb);
		saveXML();
		reload();
	}

	/**
	 * Get the MIRC plugin version
	 */
	public synchronized String getVersion() {
		return mircRoot.getAttribute("version");
	}

	/**
	 * Set the RSNA site's MIRC plugin version
	 * @param version the version of the RSNA site's MIRC plugin.
	 */
	public synchronized void setRSNAVersion(String version) {
		mircRoot.setAttribute("rsnaVersion", version);
	}

	/**
	 * Get the mode of the site (rad or vet)
	 */
	public synchronized String getMode() {
		return mircRoot.getAttribute("mode");
	}

	/**
	 * Get the default user interface
	 */
	public synchronized String getUI() {
		return mircRoot.getAttribute("UI");
	}

	/**
	 * Get the extra role names defined for the site
	 */
	public synchronized String[] getDefinedRoles() {
		return mircRoot.getAttribute("roles").split(",");
	}

	/**
	 * Install the extra roles defined for the site
	 */
	public synchronized void setDefinedRoles() {
		Users users = Users.getInstance();
		String[] roles = getDefinedRoles();
		for (String role : roles) {
			role = role.trim();
			if (!role.equals("")) users.addRole(role);
		}
	}

	/**
	 * Get a Libraries element containing all the libraries in sorted
	 * order, alphabetically by name, with all the local libraries first.
	 */
	public synchronized Element getSortedLibraries() {
		try {
			Element[] libs = libraries.values().toArray( new Element[libraries.size()] );
			Arrays.sort( libs, new ElementComparator() );
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Libraries");
			for (int i=0; i<libs.length; i++) root.appendChild( doc.importNode(libs[i], true) );
			return root;
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Get a Libraries element containing all the Library elements.
	 * @param resolve true if URLs for local servers are to
	 * include the siteurl.
	 * @return a copy of the Libraries element (with addresses resolved
	 * if resolve==true), or null if no Libraries element exists in the MircConfig XML.
	 */
	public synchronized Element getLibraries(boolean resolve) {
		try {
			Document doc = XmlUtil.getDocument();
			Element root = doc.createElement("Libraries");
			for (Element lib : libraries.values()) {
				Element el = (Element)root.appendChild( doc.importNode(lib, true) );
				if (resolve) {
					String adrs = el.getAttribute("address");
					if (adrs.startsWith("/")) {
						adrs = siteurl + adrs;
						el.setAttribute("address", adrs);
					}
				}
			}
			return root;
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Get an ID for a new Local Library.
	 * @return an ID for a new Local Library.
	 */
	public synchronized String getNewLocalLibraryID() {
		Set<String> ssids = getLocalLibraryIDs();
		int n = ssids.size() + 1;
		while ( ssids.contains( "ss"+n ) ) n++;
		return "ss"+n;
	}

	/**
	 * Get a Set containing all the local library IDs.
	 * @return all the StorageService IDs.
	 */
	public synchronized Set<String> getLocalLibraryIDs() {
		Set<String> set = new HashSet<String>();
		for (Element lib : libraries.values()) {
			if ( lib.getAttribute("local").equals("yes") ) {
				set.add(lib.getAttribute("id"));
			}
		}
		return set;
	}

	/**
	 * Get a Set containing all the local library IDs
	 * of libraries that are enabled. The Set contains
	 * all the libraries whose <code>enabled</code> attributes
	 * are set to "yes" and whose specified attributes
	 * area also set to "yes".
	 * @param attr the name of the enable attribute to test,
	 * or null if additional enable attributes are not to be tested.
	 * @return all the IDs of local libraries that meet
	 * the enable criteria.
	 */
	public synchronized Set<String> getEnabledLocalLibraryIDs(String attr) {
		Set<String> set = new HashSet<String>();
		for (Element lib : libraries.values()) {
			if ( lib.getAttribute("local").equals("yes") ) {
				if ( lib.getAttribute ("enabled").equals("yes") ) {
					if (attr != null) {
						if ( lib.getAttribute(attr).equals("yes") ) {
							set.add(lib.getAttribute("id"));
						}
					}
					else set.add(lib.getAttribute("id"));
				}
			}
		}
		return set;
	}

	/**
	 * Get a Library element, given its address attribute.
	 * @return the Library element, or null if no element
	 * exists with the specified address attribute.
	 */
	public synchronized Element getLibrary(String address) {
		return libraries.get(address);
	}

	/**
	 * Get the local Library element with the specified id.
	 * @param id the id of the Library.
	 * @return the Library element, or null if no element
	 * exists with the specified id.
	 */
	public synchronized Element getLocalLibrary(String id) {
		return getLibrary( "/storage/" + id );
	}

	/**
	 * Set the enable of a Library element.
	 * @param address the address of the Library.
	 * @param enabled true if the library is to be enabled; false otherwise.
	 */
	public synchronized void setLibraryEnable(String address, boolean enabled) {
		Element lib = getLibrary(address);
		if (lib != null) {
			lib.setAttribute("enabled", (enabled ? "yes" : "no"));
		}
	}

	/**
	 * Get the first local Library element that is enabled.
	 * @return the Library element, or null if no local library
	 * is enabled.
	 */
	public Element getFirstEnabledLocalLibrary() {
		Set<String> idSet = getEnabledLocalLibraryIDs(null);
		if (idSet.size() == 0) return null;
		String[] ids = idSet.toArray( new String[ idSet.size() ] );
		Arrays.sort(ids);
		return getLocalLibrary(ids[0]);
	}

	/**
	 * Get the local Library element that is both enabled
	 * and has the specified attribute enabled as well.
	 * This method is intended for use by author services to
	 * find a default library for authoring. .
	 * @return the Library element, or null if no element
	 * exists with the specified enable.
	 */
	public Element getFirstEnabledLocalLibrary(String attr) {
		Set<String> idSet = getEnabledLocalLibraryIDs(attr);
		if (idSet.size() == 0) return null;
		String[] ids = idSet.toArray( new String[ idSet.size() ] );
		Arrays.sort(ids);
		return getLocalLibrary(ids[0]);
	}

	/**
	 * Test the specified local library to see if it is
	 * enabled, and return it if it is; otherwise, find
	 * the first local library that is both enabled
	 * and has the specified attribute enabled as well.
	 * This method is intended for use by author services to
	 * find a default library for authoring. .
	 * @return the Library element, or null if no element
	 * exists with the specified enable.
	 */
	public Element getEnabledLocalLibrary(String id, String attr) {
		Element lib = getLocalLibrary(id);
		if ( (lib != null) && lib.getAttribute("enabled").equals("yes") ) {
			if (attr != null) {
				if (lib.getAttribute(attr).equals("yes")) return lib;
			}
			else return lib;
		}
		Set<String> idSet = getEnabledLocalLibraryIDs(attr);
		if (idSet.size() == 0) return null;
		String[] ids = idSet.toArray( new String[ idSet.size() ] );
		Arrays.sort(ids);
		return getLocalLibrary(ids[0]);
	}

	/**
	 * Insert a Library element, replacing an existing one with
	 * the same address, if it exists.
	 * Note: this method does not reload the configuration.
	 * @param library the Library element to insert.
	 */
	public synchronized void insertLibrary(Element library) {
		if (library.getNodeName().equals("Library")) {
			library = (Element)mircXML.importNode(library, true);
			libraries.put( library.getAttribute("address"), library);
		}
	}

	/**
	 * Remove the first Library element with the specified address.
	 * Note: this method does not reload the configuration.
	 * @param address the address of the server to remove.
	 */
	public synchronized void removeLibrary(String address) {
		libraries.remove(address);
	}

	/**
	 * Remove the first local Library element with the specified id.
	 * Note: this method does not reload the configuration.
	 * @param id the id of the library to remove.
	 */
	public synchronized void removeLocalLibrary(String id) {
		removeLibrary( "/storage/" + id );
	}

	/**
	 * Create a non-local Library element. (Use <code>insertLibrary</code>
	 * to insert the Library element into the configuration.)
	 * @param title the name of the library.
	 * @param address the address of the library.
	 * @param enabled whether the library is enabled ("yes" or "no").
	 */
	public synchronized Element createLibrary(String title, String address, String enabled) throws Exception {
		Element lib = mircXML.createElement("Library");
		lib.setAttribute( "address", address );
		lib.setAttribute( "enabled", enabled );
		lib.setAttribute( "local", "no" );
		Element ttl = mircXML.createElement("title");
		ttl.setTextContent(title);
		lib.appendChild(ttl);
		return lib;
	}

	/**
	 * Create a local Library element. (Use <code>insertLibrary</code>
	 * to insert the Library element into the configuration.)
	 * @param id the ID of the library (in the form "ss{n}" where {n} starts with 1).
	 * @param title the name of the library.
	 * @param address the address of the library.
	 * @param enabled whether the library is enabled ("yes" or "no").
	 */
	public synchronized Element createLocalLibrary(String id, String title, String address, String enabled) throws Exception {
		Element lib = createLibrary(title, address, enabled);
		lib.setAttribute( "id", id );
		lib.setAttribute( "local", "yes" );
		return lib;
	}

	/**
	 * Sort the libraries in order, local ones first, in alphabetical order second.
	 */
	public synchronized void sortLibraries() {
		Element mirclibs = XmlUtil.getFirstNamedChild(mircRoot, "Libraries");
		Node child;
		while ( (child=mirclibs.getFirstChild()) != null ) mirclibs.removeChild(child);

		Element[] libs = libraries.values().toArray( new Element[libraries.size()] );
		Arrays.sort( libs, new ElementComparator() );

		for (int i=0; i<libs.length; i++) mirclibs.appendChild( libs[i] );

		saveXML();
		loadXML();
	}

	class ElementComparator implements Comparator<Element> {
		public ElementComparator() { }
		public int compare( Element e1, Element e2 ) {
			String e1Local = e1.getAttribute("local");
			String e2Local = e2.getAttribute("local");
			int localCompare = e2Local.compareTo(e1Local);
			if (localCompare != 0) return localCompare;

			Element e1Title = XmlUtil.getFirstNamedChild(e1, "title");
			String e1Text = (e1Title != null) ? e1Title.getTextContent() : "";
			Element e2Title = XmlUtil.getFirstNamedChild(e2, "title");
			String e2Text = (e2Title != null) ? e2Title.getTextContent() : "";
			return e1Text.compareTo(e2Text);
		}
		public boolean equals( Element e1, Element e2) {
			return e1.equals(e2);
		}
	}

	/**
	 * Get the FileService element.
	 * @return a copy of the FileService element, or null if
	 * no FileService element exists in the MircConfig XML.
	 */
	public synchronized Element getFileService() {
		try {
			Document doc = XmlUtil.getDocument();
			Element fs = XmlUtil.getFirstNamedChild(mircRoot, "FileService");
			return (Element)doc.appendChild( doc.importNode(fs, true) );
		}
		catch (Exception ex) { }
		return null;
	}

	/**
	 * Replace the FileService element with a new one.
	 * @param service the replacement FileService element.
	 */
	public synchronized void setFileService(Element service) {
		if (service.getNodeName().equals("FileService")) {
			try {
				Element fs = XmlUtil.getFirstNamedChild(mircRoot, "FileService");
				mircRoot.replaceChild( mircXML.importNode(service, true), fs );
				saveXML();
				loadXML();
			}
			catch (Exception ignore) { }
		}
	}

	/**
	 * Get the news element. The format of the element is:<br><br>
	 * &lt;news&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&lt;title&gt;text to be displayed on the query page&lt;/title&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&lt;image&gt;url of the image to be displayed on the query page&lt;/image&gt;<br>
	 * &nbsp;&nbsp;&nbsp;&lt;url&gt;url of the news document&lt;/url&gt;<br>
	 * &lt;/news&gt;<br>
	 * @return the news element.
	 */
	public synchronized Element getNews() {
		try {
			NodeList nl = mircRoot.getElementsByTagName("news");
			if (nl.getLength() == 0) {
				Document doc = XmlUtil.getDocument();
				return doc.createElement("news");
			}
			return (Element)nl.item(0);
		}
		catch (Exception ex) { return null; }
	}

	/**
	 * Set the news element.
	 * @param title the title of the news item
	 * @param image the URL of the image used as the icon of the news item
	 * @param url the URL of the news item
	 */
	public synchronized void setNews(String title, String image, String url) {
		deleteNews();
		Element news = mircRoot.getOwnerDocument().createElement("news");
		setNewsChild(news, "title", title);
		setNewsChild(news, "image", image);
		setNewsChild(news, "url", url);
		mircRoot.appendChild(news);
		saveXML();
		loadXML();
	}

	private void setNewsChild(Element parent, String childName, String value) {
		if (value != null) {
			value = value.trim();
			if (value.length() != 0) {
				Element child = parent.getOwnerDocument().createElement(childName);
				child.setTextContent(value);
				parent.appendChild(child);
			}
		}
	}

	/**
	 * Delete the news element.
	 */
	public synchronized void deleteNews() {
		NodeList nl = mircRoot.getElementsByTagName("news");
		if (nl.getLength() != 0) {
			mircRoot.removeChild(nl.item(0));
			saveXML();
			loadXML();
		}
	}

	/**
	 * Get the DicomAnonymizer stage for the File Service
	 * @return the DicomAnonymizer stage for the File Service, or null
	 * if the stage cannot be found.
	 */
	public DicomAnonymizer getFileServiceDicomAnonymizer() {
		if (fsDicomAnonymizer == null) {
			Configuration ctp = Configuration.getInstance();
			PipelineStage stage = ctp.getRegisteredStage("FileServiceAnonymizer");
			if ((stage != null) && (stage instanceof DicomAnonymizer)) {
				fsDicomAnonymizer = (DicomAnonymizer)stage;
			}
		}
		return fsDicomAnonymizer;
	}

}


/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.ssadmin;

import java.io.*;
import java.net.*;
import java.util.*;

import mirc.MircConfig;
import mirc.storage.Index;
import mirc.storage.IndexEntry;

import org.apache.log4j.Logger;
import org.rsna.server.HttpRequest;
import org.rsna.server.HttpResponse;
import org.rsna.server.Path;
import org.rsna.servlets.Servlet;
import org.rsna.util.StringUtil;
import org.rsna.util.FileUtil;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The Storage Service Admin servlet.
 * This servlet provides a browser-accessible user interface for
 * configuring the storage services.
 */
public class StorageServiceAdmin extends Servlet {

	static final Logger logger = Logger.getLogger(StorageServiceAdmin.class);

	/**
	 * Construct a FileServiceAdmin.
	 * @param root the root directory of the server.
	 * @param context the path identifying the servlet.
	 */
	public StorageServiceAdmin(File root, String context) {
		super(root, context);
		//Override the root supplied by the HttpServer
		//with the root of the MIRC plugin.
		this.root = MircConfig.getInstance().getRootDirectory();
	}

	/**
	 * Static init method to initialize the static variables.
	 * This method is called by the ServletSelector when the
	 * servlet is added to the list of servlets by the MIRC
	 * plugin. At that point, the MircConfig instance has
	 * been created and is available.
	 */
	public static void init(File root, String context) { }

	/**
	 * The servlet method that responds to an HTTP GET.
	 * This method returns an HTML page containing a form for
	 * changing the parameters of the File Service.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 */
	public void doGet(HttpRequest req, HttpResponse res) throws Exception {

		Path path = req.getParsedPath();
		int pathLength = path.length();
		String function = path.element(1);

		if (req.userHasRole("admin")) {

			MircConfig mc = MircConfig.getInstance();

			if (pathLength == 1) {
				//This is a request for the admin page
				res.disableCaching();
				res.setContentType("html");
				res.write( getPage() );
				res.send();
				return;
			}

			else if ((pathLength == 3) && function.equals("list")) {
				//This is a request for an index listing for a local library.
				String ssid = path.element(2);
				int line = StringUtil.getInt(req.getParameter("line", "0"), 0);
				res.disableCaching();
				res.setContentType("html");
				res.write( listIndex( ssid, line ) );
				res.send();
				return;
			}

			else if ((pathLength == 3) && function.equals("remove")) {
				//This is a request to remove a local library.
				String ssid = path.element(2);
				mc.removeLocalLibrary(ssid);
				mc.sortLibraries();
				res.disableCaching();
				res.setContentType("html");
				res.write( getPage() );
				res.send();
				return;
			}

			else if ((pathLength == 2) && (function.equals("new"))) {
				//This is a request to create a new local library
				String ssid = mc.getNewLocalLibraryID();

				String title = "New Library";
				String address = "/storage/" + ssid;
				String enabled = "yes";

				Element lib = mc.createLocalLibrary(ssid, title, address, enabled);
				mc.insertLibrary( lib );
				mc.sortLibraries();

				//Now return the admin page so the user can tweak the new service.
				res.disableCaching();
				res.setContentType("html");
				res.write( getPage() );
				res.send();
				return;
			}

			else if ((pathLength == 2) && (function.equals("rebuild"))) {
				//This is a request to rebuild all the indexes.
				StorageServiceRebuilder rebuilder = StorageServiceRebuilder.getInstance();
				if (rebuilder != null) {
					rebuilder.start();
				}
				//Return the admin page.
				res.disableCaching();
				res.setContentType("html");
				res.write( getPage() );
				res.send();
				return;
			}

			else if (function.equals("deleteDocument")) {
				String ssid = path.element(2);
				if (ssid.startsWith("ss") && (pathLength > 3)) {
					String docref = path.subpath(3).substring(1); //(remove the leading slash)
					deleteDocument(ssid, docref);
				}
				int line = StringUtil.getInt(req.getParameter("line", "0"), 0);
				res.disableCaching();
				res.setContentType("html");
				res.write( listIndex( ssid, line ) );
				res.send();
				return;
			}

			else { super.doGet(req, res); return; }
		}

		//The user does not have the admin role;
		//redirect to the main query page.
		res.redirect("/query");
	}

	/**
	 * The servlet method that responds to an HTTP POST.
	 * This method uses the posted parameters to update
	 * the FileService configuration.
	 * It then returns an HTML page containing a new form
	 * constructed from the new contents of the file.
	 * @param req The HttpRequest provided by the servlet container.
	 * @param res The HttpResponse provided by the servlet container.
	 */
	public void doPost(HttpRequest req, HttpResponse res) throws Exception {

		if (req.userHasRole("admin")) {

			MircConfig mc = MircConfig.getInstance();

			HashSet<String> ids = new HashSet<String>();
			for (String name : req.params.keySet()) {
				String id = name.substring( 0, name.indexOf("-") );
				ids.add(id);
			}
			for (String id : ids) {
				String title = req.getParameter(id+"-title", "");
				String tagline = req.getParameter(id+"-tagline", "");
				String timeout = Integer.toString( StringUtil.getInt( req.getParameter(id+"-timeout", "0"), 0) );
				String maxsize = Integer.toString( StringUtil.getInt( req.getParameter(id+"-maxsize", "75"), 75) );
				String jpegquality = Integer.toString( StringUtil.getInt( req.getParameter(id+"-jpegquality", "-1"), -1) );
				String autoindex = req.getParameter(id+"-autoindex", "no");
				String authenb = req.getParameter(id+"-authenb", "no");
				String subenb = req.getParameter(id+"-subenb", "no");
				String zipenb = req.getParameter(id+"-zipenb", "no");
				String dcmenb = req.getParameter(id+"-dcmenb", "no");
				String tceenb = req.getParameter(id+"-tceenb", "no");

				Element lib = mc.getLocalLibrary(id);
				lib.setAttribute( "timeout", timeout );
				lib.setAttribute( "maxsize", maxsize );
				lib.setAttribute( "jpegquality", jpegquality );
				lib.setAttribute( "autoindex", autoindex );
				lib.setAttribute( "authenb", authenb );
				lib.setAttribute( "subenb", subenb );
				lib.setAttribute( "zipenb", zipenb );
				lib.setAttribute( "dcmenb", dcmenb );
				lib.setAttribute( "tceenb", tceenb );

				lib.removeAttribute( "acclog" ); //clean up unused attribute

				setChild(lib, "title", title, false);
				setChild(lib, "tagline", tagline, true);
			}
			mc.sortLibraries();

			//Return a new form
			res.disableCaching();
			res.setContentType("html");
			res.write( getPage() );
			res.send();
		}
		else res.redirect("/query");
	}

	private void setChild(Element el, String name, String value, boolean acceptBlank) {
		value = value.trim();
		if (acceptBlank || !value.equals("")) {
			Element child = XmlUtil.getFirstNamedChild( el, name );
			if (child == null) {
				child = el.getOwnerDocument().createElement(name);
				el.appendChild(child);
			}
			child.setTextContent(value);
		}
	}

	private String getPage() throws Exception {
		MircConfig mc = MircConfig.getInstance();

		Document ssparams = XmlUtil.getDocument();
		Element root = ssparams.createElement("ssparams");
		ssparams.appendChild(root);
		Set<String> ssids = mc.getLocalLibraryIDs();
		for (String ssid : ssids) {
			Element ss = ssparams.createElement("ss");
			Index index = Index.getInstance(ssid);
			ss.setAttribute("id", ssid);
			ss.setAttribute("size", Integer.toString(index.getIndexSize()));
			ss.setAttribute("dir", index.getDocumentsDir().getAbsolutePath());
			root.appendChild(ss);
		}

		Document xml = mc.getXML();
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/ssadmin/StorageServiceAdmin.xsl" ) );
		boolean busy = StorageServiceRebuilder.isBusy();
		Object[] params = new Object[] {
			"ssparams", ssparams,
			"rebuildInProgress", (busy ? "yes" : "no")
		};
		return XmlUtil.getTransformedText( xml, xsl, params );
	}

	//Display the storage service index.
	private String listIndex(String ssid, int line) throws Exception {
		Element lib = MircConfig.getInstance().getLocalLibrary(ssid);
		Index index = Index.getInstance(ssid);
		if ((lib == null) || (index == null)) return getPage();

		IndexEntry[] docs = index.query("");
		index.sortByLMDate(docs);

		if (line < 0) line = 0;
		else if (line >= docs.length) line = docs.length - 1;

		Document list = XmlUtil.getDocument();
		Element root = list.createElement("list");
		list.appendChild(root);
		root.setAttribute( "line", Integer.toString(line) );

		root.appendChild( list.importNode( lib, true ) );

		for (IndexEntry doc : docs) {
			Element el = list.createElement("doc");
			el.setAttribute( "path", doc.md.getAttribute("path") );
			appendChild( "title", doc.title, el );
			appendChild( "pubdate", doc.pubdate, el );
			appendChild( "lmdate", StringUtil.getDateTime(doc.lmdate, " "), el );
			root.appendChild(el);
		}
		Document xsl = XmlUtil.getDocument( FileUtil.getStream( "/ssadmin/StorageServiceList.xsl" ) );
		return XmlUtil.getTransformedText(list, xsl, null);
	}

	private void appendChild(String name, String value, Element parent) {
		Element child = parent.getOwnerDocument().createElement(name);
		child.setTextContent(value);
		parent.appendChild(child);
	}

	/**
	 * Delete a document from a local library.
	 * @param ssid the ID of the local library.
	 * @param docref relative path to the document (typically "docs/{directorypath}/MIRCdocument.xml").
	 * @return true if the deletion succeeded; false otherwise.
	 */
	public static boolean deleteDocument(String ssid, String docref) {

		boolean ok = false;
		Index index = Index.getInstance(ssid);
		if (index != null) {
			ok = index.removeDocument(docref);
			index.commit();
		}

		if (ok) {
			//Now delete the directory containing the MIRCdocument.
			//Note: docref points to the MIRCdocument XML file.
			//Get a File pointing to the parent directory.
			MircConfig mc = MircConfig.getInstance();
			File mircRoot = mc.getRootDirectory();
			String path = "storage/" + ssid + "/" + docref;
			path = path.replace( '/', File.separatorChar );
			File dir = new File(mircRoot, path).getParentFile();
			return FileUtil.deleteAll(dir);
		}
		return false;
	}

}

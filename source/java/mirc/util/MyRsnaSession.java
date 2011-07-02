/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.rsna.util.ClientHttpRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Encapsulates a collection of methods for accessing a user's MyRSNA account.
 */
public class MyRsnaSession {

	static final Logger logger = Logger.getLogger(MyRsnaSession.class);

	String username = null;
	String password = null;
	String rsnatoken = null;
	String firstname = "";
	String lastname = "";
	boolean isLoggedIn = false;

	static final String urlString = "http://myrsna.rsna.org/API/cfc/myRSNA.cfc";
	static final String rsnakey = "d68cf020-91b1-42a9-8969-b89641910c73";
	static final String downloadURLString = "http://media.rsna.org/myfile.cfm";

	/**
	 * Creator using names.
	 * @param username the MyRsna account username
	 * @param password the MyRsna account password
	 */
	public MyRsnaSession(String username, String password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Creator using a MyRsnaUser.
	 * @param myRsnaUser the MyRsnaUser object for the user.
	 */
	public MyRsnaSession(MyRsnaUser myRsnaUser) {
		this.username = myRsnaUser.username;
		this.password = myRsnaUser.password;
	}

	/**
	 * Open a myRSNA session.
	 * @return true if the user is logged in; false otherwise.
	 */
	public boolean login() {
		try {
			String args = "<RSNA_UN>" + username + "</RSNA_UN><RSNA_PW>" + password + "</RSNA_PW>";
			String result = get("authenticateRSNAuser", args);
			Document resultDoc = getDocument(result);
			setSessionParams(resultDoc);
		}
		catch (Exception ex) { clearTokens(); }
		return isLoggedIn;
	}

	/**
	 * Log out of the myRSNA session.
	 * @return true if the user is logged in; false otherwise.
	 */
	public boolean logout() {
		try {
			if (rsnatoken == null) return false;
			String result = get("logoutRSNAuser");
			Document resultDoc = getDocument(result);
			setSessionParams(resultDoc);
		}
		catch (Exception ex) { clearTokens(); }
		return isLoggedIn;
	}

	/**
	 * Check whether the myRSNA session is still valid.
	 * @return true if the session is still valid; false otherwise.
	 */
	public boolean isOpen() {
		try {
			if (rsnatoken == null) return false;
			String result = get("checkRSNALogin");
			Document resultDoc = getDocument(result);
			setSessionParams(resultDoc);
		}
		catch (Exception ex) { clearTokens(); }
		return isLoggedIn;
	}

	/**
	 * Get the myRSNA file folders.
	 * @return the folders element, or null if the request failed.
	 */
	public Element getMyRSNAFolders() {
		try {
			if (rsnatoken == null) return null;
			String result = get("getmyFilesFolders");
			Document resultDoc = getDocument(result);
			if (resultDoc != null) {
				boolean success = getValue(resultDoc, "success").equals("true");
				if (success) {
					NodeList nl = resultDoc.getDocumentElement().getElementsByTagName("folders");
					if (nl.getLength() > 0) return (Element)nl.item(0);
				}
			}
		}
		catch (Exception ex) { }
		return null;
	}

	/**
	 * Get the myRSNA files.
	 * @return the folders element, or null if the request failed.
	 */
	public Element getMyRSNAFiles(String folderID) {
		try {
			if (rsnatoken == null) return null;
			folderID = (folderID != null) ? folderID.trim() : "";
			String args = (!folderID.equals("")) ? "<folderid>"+folderID+"</folderid>" : "";
			String result = get("getmyFilesMirc", args);
			Document resultDoc = getDocument(result);
			if (resultDoc != null) {
				boolean success = getValue(resultDoc, "success").equals("true");
				if (success) {
					NodeList nl = resultDoc.getDocumentElement().getElementsByTagName("folders");
					if (nl.getLength() > 0) return (Element)nl.item(0);
				}
			}
		}
		catch (Exception ex) { }
		return null;
	}

	/**
	 * Download a file from the myRSNA site based on its id. Note that since file accesses
	 * are not authenticated on the MyRSNA site, this can be a static method because
	 * a session is not required.
	 * @param file the file in which to store the downloaded data.
	 * @param id the id of the file to download.
	 * @return true if the transfer succeeded; false if it failed.
	 */
	public static boolean getFile(File file, String id) {
		boolean ok = true;
		HttpURLConnection conn = null;
		BufferedOutputStream bos = null;
		BufferedInputStream bis = null;
		URL url = null;
		try {
			url = new URL(downloadURLString + "?q="+id+"&t=ORIGINAL");
			bos = new BufferedOutputStream( new FileOutputStream(file) );
			conn = getConnection(url);
			conn.connect();
			int responseCode = conn.getResponseCode();
			if (responseCode != 200) throw new Exception("Error downloading from "+url);
			bis = new BufferedInputStream( conn.getInputStream() );
			byte[] buffer = new byte[4096];
			int n;
			while ((n=bis.read(buffer, 0, buffer.length)) != -1) bos.write(buffer, 0, n);
			bos.flush();
		}
		catch (Exception ex) {
			logger.warn(ex.getMessage(), ex);
			ok = false;
		}
		finally {
			try { if (bis != null) bis.close(); }
			catch (Exception ignore) { }
			try { if (bos != null) bos.close(); }
			catch (Exception ignore) { }
		}
		return ok;
	}

	/**
	 * Post a file to the myRSNA files root, using the filename as the title.
	 * @param file the file to send.
	 * @return true if the request succeeded; false if the request failed.
	 */
	public boolean postFile(File file) {
		return postFile(file, file.getName());
	}

	/**
	 * Post a file to the myRSNA files root.
	 * @param file the file to send.
	 * @param title the title to be used for the file on myRSNA files.
	 * @return true if the request succeeded; false if the request failed.
	 */
	public boolean postFile(File file, String title) {
		if (rsnatoken == null) return false;
		if (title == null) title = file.getName();
		try {
			URL url = new URL(urlString);
			ClientHttpRequest req = new ClientHttpRequest(url);

			//create the full set of args
			String xmlargs = "<args>"
						   +	"<rsnakey>"+rsnakey+"</rsnakey>"
						   +	"<rsnatoken>"+rsnatoken+"</rsnatoken>"
						   +	"<returntype>XML</returntype>"
						   +	"<filename>" + title + "</filename>"
						   + "</args>";

			//set the parameters
			req.setParameter("wsdl", "");
			req.setParameter("method", "uploadmyFiles");
			req.setParameter("xmlargs", xmlargs);
			req.setParameter("Filedata", file);

			//post the request and get the response
			String result = getResponse(req.post());

			Document resultDoc = getDocument(result);
			if (resultDoc != null) return getValue(resultDoc, "success").equals("true");
			return false;
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	/**
	 * Make an HTTP connection and get a text result.
	 * @param method the myRSNA method to call.
	 * @return the text returned by the myRSNA method.
	 */
	public String get(String method) {
		return get(method, "");
	}

	/**
	 * Make an HTTP connection and get a text result.
	 * @param method the myRSNA method to call.
	 * @param args the parameters to pass to the method, in XML text.
	 * @return the text returned by the myRSNA method.
	 */
	public String get(String method, String args) {
		HttpURLConnection conn;
		try {
			//create the full set of args
			String allArgs = "<args>"
							+ "<rsnakey>"+rsnakey+"</rsnakey>"
							+ ((rsnatoken != null) ? "<rsnatoken>"+rsnatoken+"</rsnatoken>" : "")
							+ args
							+ "<returntype>XML</returntype>"
							+"</args>";

			//Construct the URL
			String query = "wsdl"
							+ "&method=" + URLEncoder.encode(method, "UTF-8")
							+ "&xmlargs=" + URLEncoder.encode(allArgs, "UTF-8");
			URL url = new URL(urlString + "?" + query);

			//Establish the connection
			conn = getConnection(url);
			conn.connect();
/*
			//log the headers on an authenticate method, just for testing
			if (method.equals("authenticateRSNAuser")) {
				Map<String, List<String>> headers = conn.getHeaderFields();
				String[] keys = headers.keySet().toArray(new String[headers.size()]);
				for (int i=0; i<keys.length; i++) {
					List header = headers.get(keys[i]);
					Iterator<String> it = header.iterator();
					while (it.hasNext()) {
						logger.warn("Header: "+keys[i]+" = "+it.next());
					}
				}
			}
*/
			//Get the response
			return getResponse(conn.getInputStream());
		}
		catch (Exception e) { return null; }
	}

	private String getResponse(InputStream is) throws Exception {
		int n;
		BufferedReader svrrdr;
		InputStreamReader isr = new InputStreamReader(is, "UTF-8");
		svrrdr = new BufferedReader(isr);
		StringWriter svrsw = new StringWriter();
		char[] cbuf = new char[1024];
		while ((n = svrrdr.read(cbuf,0,cbuf.length)) != -1) svrsw.write(cbuf,0,n);
		svrrdr.close();
		//return the result
		return svrsw.toString();
	}


	private static HttpURLConnection getConnection(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(false);
		conn.setDoInput(true);
		conn.setRequestMethod("GET");
		setProxy(conn);
		return conn;
	}

	private static void setProxy(HttpURLConnection conn) {
/*
		//If the proxy is enabled and authentication credentials
		//are available, set them in the request.
		if (proxy.getProxyEnabled() && proxy.authenticate()) {
			conn.setRequestProperty(
				"Proxy-Authorization",
				"Basic "+proxy.getEncodedProxyCredentials());
		}
*/
	}

	private static Document getDocument(String xmlString) throws Exception {
		StringReader sr = new StringReader(xmlString);
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		return db.parse(new InputSource(sr));
	}

	private boolean setSessionParams(Document resultDoc) throws Exception {
		if (resultDoc != null) {
			boolean success = getValue(resultDoc, "success").equals("true");
			if (success) {
				rsnatoken = getValue(resultDoc, "RSNAtoken");
				firstname = getValue(resultDoc, "firstname");
				lastname = getValue(resultDoc, "lastname");
				isLoggedIn = getValue(resultDoc, "isLoggedin").equals("true");
				return isLoggedIn;
			}
		}
		clearTokens();
		return false;
	}

	private void clearTokens() {
		rsnatoken = null;
		isLoggedIn = false;
	}

	private static String getValue(Document doc, String elementName) throws Exception {
		Element root = doc.getDocumentElement();
		NodeList nl = root.getElementsByTagName(elementName);
		if (nl.getLength() == 0) return null;
		return nl.item(0).getTextContent();
	}

	private void printResult(String result) {
		result = result.replaceAll("><", ">\n<");
		System.out.println(result);
	}

}
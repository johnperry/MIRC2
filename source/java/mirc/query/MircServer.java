/*---------------------------------------------------------------
*  Copyright 2005 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.query;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.apache.log4j.Logger;
import org.rsna.server.User;
import org.rsna.util.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The thread that queries one storage service. One instance of this thread is
 * instantiated by the query service for each storage service (server) to be queried.
 */
public class MircServer extends Thread {

	private String urlString;
	private User user;
	private String serverName;
	private String mircQuery;
	private QueryService queryService;

	static final Logger logger = Logger.getLogger(MircServer.class);

	/**
	 * Class constructor.
	 * @param urlString the URL of the MIRC storage service to be queried.
	 * @param user the user making the request, or null if the user is not authenticated.
	 * @param serverName the name of the MIRC storage service to be queried
	 * (used as a heading in the results list).
	 * @param mircQuery the MIRCquery XML string.
	 * @param queryService the QueryService to notify when results are available.
	 */
	public MircServer(
			String urlString,
			User user,
			String serverName,
			String mircQuery,
			QueryService queryService) {
		this.urlString = urlString;
		this.user = user;
		this.serverName = serverName;
		this.mircQuery = mircQuery;
		this.queryService = queryService;
	}

	/**
	 * The thread's run code that sends a query to the server,
	 * waits for a response, and returns it to the Query Service.
	 */
	public void run() {

		logger.debug("Sending query to "+urlString);

		String serverResponse = "";
		try {
			URL url = new URL(urlString);
			if (url.getUserInfo() != null) Authenticator.setDefault(new QueryAuthenticator(url));
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type","text/xml; charset=\"UTF-8\"");

			//If the user is authenticated and the server is local, pass the user's credentials.
			if (user != null) conn.setRequestProperty("Authorization", user.getBasicAuthorization());

			conn.setDoOutput(true);
			conn.connect();

			//Send the query to the server
			BufferedWriter svrbw =
				new BufferedWriter(
					new OutputStreamWriter( conn.getOutputStream(), FileUtil.utf8 ) );
			svrbw.write(mircQuery);
			svrbw.flush();
			svrbw.close();

			//Get the response
			BufferedReader svrrdr =
				new BufferedReader(
					new InputStreamReader( conn.getInputStream(),FileUtil.utf8 ) );
			StringWriter svrsw = new StringWriter();
			char[] cbuf = new char[1024];
			int n;
			boolean hcf = false;
			while (((n = svrrdr.read(cbuf,0,1024)) != -1) && !(hcf = interrupted())) svrsw.write(cbuf,0,n);
			svrrdr.close();
			if (hcf) serverResponse = makeExceptionResponse("No response from the server.");
			else serverResponse = svrsw.toString();
		}
		catch (MalformedURLException e) {
			serverResponse = makeExceptionResponse("Malformed URL: "+urlString);
		}
		catch (IOException e) {
			serverResponse =
				makeExceptionResponse(
					"IO Error during connection: " + urlString + "<br/>" + e.getMessage() );
		}
		catch (IllegalStateException e) {
			serverResponse = makeExceptionResponse("Programming error in the MircServer class.");
		}
		catch (NullPointerException e) {
			serverResponse = makeExceptionResponse("Programming error in the MircServer class.");
		}

		//Add the server name and URL to the MIRCqueryresult
		Document result = null;
		try {
			result = XmlUtil.getDocument(serverResponse);
			Element root = result.getDocumentElement();
			root.setAttribute("url", urlString);
			Element server = result.createElement("server");
			server.setTextContent(serverName);
			root.insertBefore( server, root.getFirstChild() );
		}
		catch (Exception e) {
			try {
				result = XmlUtil.getDocument( makeExceptionResponse(
							StringUtil.makeReadableTagString(serverName) +
							"<br/>Error processing storage service response:" +
							"<br/>Server Response:<br/>" +
							StringUtil.makeReadableTagString(serverResponse) )
						);
			}
			catch (Exception returnNull) { }
		}

		//Return the result
		queryService.acceptQueryResult(this, result);

		logger.debug("Response from "+urlString+" returned to the QueryService");
	}

	//Make an error response as a MIRCqueryresult..
	private String makeExceptionResponse(String s) {
		return "<MIRCqueryresult><preamble><font color=\"red\"><b>"
						+ s + "</b></font></preamble></MIRCqueryresult>";
	}
}


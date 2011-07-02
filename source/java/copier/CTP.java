/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package copier;

import java.io.*;
import java.net.*;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.jar.*;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.swing.JOptionPane;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

public class CTP {

	public static HttpURLConnection getConnection(URL url) throws Exception {
		String protocol = url.getProtocol().toLowerCase();
		if (!protocol.startsWith("https") && !protocol.startsWith("http")) {
			throw new Exception("Unsupported protocol ("+protocol+")");
		}
		HttpURLConnection conn;
		if (protocol.startsWith("https")) {
			HttpsURLConnection httpsConn = (HttpsURLConnection)url.openConnection();
			httpsConn.setHostnameVerifier(new AcceptAllHostnameVerifier());
			httpsConn.setUseCaches(false);
			httpsConn.setDefaultUseCaches(false);
			conn = httpsConn;
		}
		else conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		return conn;
	}

	static class AcceptAllHostnameVerifier implements HostnameVerifier {
		public boolean verify(String urlHost, SSLSession ssls) {
			return true;
		}
	}

    public static void shutdown(boolean ssl, int port) {
		try {
			String protocol = "http" + (ssl?"s":"");
			URL url = new URL( protocol, "127.0.0.1", port, "shutdown");

			HttpURLConnection conn = getConnection( url );
			conn.setRequestMethod("GET");
			conn.setRequestProperty("servicemanager","shutdown");
			conn.connect();

			StringBuffer sb = new StringBuffer();
			BufferedReader br = new BufferedReader( new InputStreamReader(conn.getInputStream(), "UTF-8") );
			int n; char[] cbuf = new char[1024];
			while ((n=br.read(cbuf, 0, cbuf.length)) != -1) sb.append(cbuf,0,n);
			br.close();
		}
		catch (Exception ex) { }
	}

	public static boolean isRunning(boolean ssl, int port) {
		try {
			URL url = new URL("http" + (ssl?"s":"") + "://127.0.0.1:"+port);
			HttpURLConnection conn = getConnection(url);
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(500);
			conn.connect();
			int length = conn.getContentLength();
			StringBuffer text = new StringBuffer();
			InputStream is = conn.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			int size = 256; char[] buf = new char[size]; int len;
			while ((len=isr.read(buf,0,size)) != -1) text.append(buf,0,len);
			return true;
		}
		catch (Exception ex) { return false; }
	}

}


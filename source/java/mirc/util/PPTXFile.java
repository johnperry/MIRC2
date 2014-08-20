/*---------------------------------------------------------------
*  Copyright 2014 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.File;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import org.apache.log4j.Logger;
import org.rsna.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
  * A class to encapsulate a PPTX file.
  */
public class PPTXFile {

	static final Logger logger = Logger.getLogger(PPTXFile.class);
    static final Pattern pattern = Pattern.compile("slide[0-9]+\\.xml$");

    File file = null;

	public PPTXFile(File file) throws Exception {
		this.file = file;
		ZipFile zipFile = new ZipFile(file);
		zipFile.close();
	}

	public String getSlideText() throws Exception {
		ZipFile zipFile = new ZipFile(file);
		StringBuffer sb = new StringBuffer();
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements() ) {
			ZipEntry entry = entries.nextElement();
			String name = entry.getName();
			Matcher matcher = pattern.matcher(name);
			if (matcher.find()) {
				try {
					Document doc = XmlUtil.getDocument(zipFile.getInputStream(entry));
					Element root = doc.getDocumentElement();
					NodeList nl = root.getElementsByTagName("a:t");
					for (int i=0; i<nl.getLength(); i++) {
						sb.append( nl.item(i).getTextContent() );
						sb.append(" ");
					}
				}
				catch (Exception skip) { }
			}
		}
		zipFile.close();
		return sb.toString();
	}

}
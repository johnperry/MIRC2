/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package copier;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import org.w3c.dom.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import mirc.MircConfig;
import mirc.prefs.Preferences;
import mirc.storage.Index;

import org.rsna.util.FileUtil;
import org.rsna.util.StringUtil;
import org.rsna.util.XmlUtil;

import org.rsna.server.*;

/**
 * The MIRC Copier program. This program copies information
 * from a Tomcat/MIRC instance to a CTP/MIRC instance.
 */
public class MircSiteCopier extends JFrame {

	MainPanel mainPanel;
	ColorPane cp;
	Color bgColor = new Color(0xc6d8f9);
	JCheckBox indexDocuments;

	static final int interval = 200;
	static final boolean forceCopy = true;

	public static void main(String args[]) {
		new MircSiteCopier();
	}

	/**
	 * Class constructor; creates a new Launcher object, displays a JFrame
	 * providing the GUI for configuring and launching CTP.
	 */
	public MircSiteCopier() {
		super();

		//Initialize Log4J
		File logs = new File("logs");
		logs.mkdirs();
		File logProps = new File("log4j.properties");
		PropertyConfigurator.configure(logProps.getAbsolutePath());

		setTitle("MIRC Tomcat to CTP Copier");

		mainPanel = new MainPanel();
		cp = new ColorPane();
		cp.setScrollableTracksViewportWidth(false);

		JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT);
		splitPane.setContinuousLayout(true);
		splitPane.setResizeWeight(0.0D);
		splitPane.setTopComponent(mainPanel);

		JScrollPane jsp = new JScrollPane();
		jsp.setViewportView(cp);
		jsp.getViewport().setBackground(Color.white);
		splitPane.setBottomComponent(jsp);

		this.getContentPane().add( splitPane, BorderLayout.CENTER );
		this.getContentPane().setBackground(bgColor);
		this.addWindowListener(new WindowCloser(this));

		pack();
		positionFrame();
		setVisible(true);
	}

	private void positionFrame() {
		Toolkit tk = getToolkit();
		Dimension scr = tk.getScreenSize ();
		setSize( 500, 600 );
		int x = (scr.width - getSize().width)/2;
		int y = (scr.height - getSize().height)/2;
		setLocation( new Point(x,y) );
	}

    class WindowCloser extends WindowAdapter {
		public WindowCloser(JFrame parent) { }
		public void windowClosing(WindowEvent evt) {
			System.exit(0);
		}
    }

	class MainPanel extends JPanel implements ActionListener {

		JButton browse;
		JLabel tomcat;
		JButton start;
		File tomcatDir = null;
		File ctpDir = null;

		public MainPanel() {
			super();
			setBackground(bgColor);

			Box box = new Box(BoxLayout.Y_AXIS);
			box.setBackground(bgColor);
			this.add(box);

			box.add( Box.createVerticalStrut(15) );

			JLabel title = new JLabel("MIRC Tomcat to CTP Copier");
			title.setFont( new Font( "SansSerif", Font.BOLD, 24 ) );
			title.setForeground( Color.BLUE );
			Box titleBox = new Box(BoxLayout.X_AXIS);
			titleBox.add(title);
			box.add( titleBox );

			box.add( Box.createVerticalStrut(15) );

			browse = new JButton("Browse...");
			browse.addActionListener(this);
			tomcat = new JLabel("Search:");
			tomcat.setFont( new Font( "Monospaced", Font.BOLD, 14 ) );
			Box tcBox = new Box(BoxLayout.X_AXIS);
			tcBox.add( new JLabel("Tomcat location:") );
			tcBox.add( Box.createHorizontalStrut(5) );
			tcBox.add( tomcat );
			tcBox.add( Box.createHorizontalStrut(10) );
			tcBox.add( browse );
			box.add( tcBox );

			box.add( Box.createVerticalStrut(15) );

			indexDocuments = new JCheckBox("Index documents");
			indexDocuments.setSelected(true);
			indexDocuments.setOpaque(false);
			Box indexBox = new Box(BoxLayout.X_AXIS);
			indexBox.add( indexDocuments );
			box.add( indexBox );

			box.add( Box.createVerticalStrut(15) );

			Box copyBox = new Box(BoxLayout.X_AXIS);
			start = new JButton("Copy Tomcat MIRC Site to CTP");
			start.setEnabled(false);
			start.addActionListener(this);
			copyBox.add(start);
			box.add( copyBox );

			box.add( Box.createVerticalStrut(15) );

			ctpDir = new File(System.getProperty("user.dir"));
			tomcatDir = Tomcat.findTomcat();
			if (tomcatDir != null) {
				tomcat.setText( tomcatDir.getAbsolutePath() );
				start.setEnabled(true);
			}
		}

		public void actionPerformed(ActionEvent event) {
			if (event.getSource().equals(browse)) {
				File dir = Tomcat.getDirectory(this);
				if (Tomcat.contentsCheck(dir)) {
					tomcatDir = dir;
					tomcat.setText( tomcatDir.getAbsolutePath() );
					start.setEnabled(true);
				}
				else {
					tomcatDir = null;
					tomcat.setText("Search:");
				}
			}
			else if (event.getSource().equals(start)) {
				start.setEnabled(false);
				Copier copier = new Copier(tomcatDir, ctpDir, cp);
				copier.start();
			}
		}
	}

	class Copier extends Thread {
		File tomcat;
		File ctp;
		ColorPane cp;

		File ctpConfigFile = null;
		File mircsiteRoot = null;
		File mircConfigFile = null;

		File[] tcSS = null;

		Document ctpConfigDoc = null;
		Element ctpRoot = null;
		MircConfig mircConfig = null;

		public Copier(File tomcat, File ctp, ColorPane cp) {
			this.tomcat = tomcat;
			this.ctp = ctp;
			this.cp = cp;

			try {
				ctpConfigFile = new File("config.xml");
				ctpConfigDoc = XmlUtil.getDocument(ctpConfigFile);
				ctpRoot = ctpConfigDoc.getDocumentElement();
				Element mircPlugin = getSpecifiedChild(ctpRoot, "Plugin", "class", "mirc.MIRC");
				String rootAttr = mircPlugin.getAttribute("root");
				mircsiteRoot = new File(rootAttr);
				mircsiteRoot.mkdirs();
				File mircConfigFile = new File(mircsiteRoot, "mirc.xml");
				if (!mircConfigFile.exists()) {
					FileUtil.getFile(mircConfigFile, "/mirc/mirc.xml");
				}
				stopCTP(ctpRoot);
				mircConfig = MircConfig.load(mircConfigFile);
				tcSS = Tomcat.getStorageServices(tomcat);
			}
			catch (Exception ex) {
				cp.println(Color.red, "Unable to parse the CTP config files");
				cp.print(StringUtil.getStackTrace(ex));
			}
		}

		private void stopCTP(Element ctpRoot) {
			Element serverElement = XmlUtil.getFirstNamedChild(ctpRoot, "Server");
			int port = StringUtil.getInt( serverElement.getAttribute("port").trim(), 80 );
			boolean ssl = serverElement.getAttribute("ssl").trim().equals("yes");
			if (CTP.isRunning(ssl, port)) {
				cp.print("Stopping CTP... ");
				CTP.shutdown(ssl, port);
				cp.println(" done\n");
			}
		}

		public void run() {
			copyUsers();
			copyPrefs();
			copyStorageServices();
			cp.println("\n...done");
		}

		private void copyUsers() {
			cp.println("Copying users");
			File tcUsersFile = new File(tomcat, "conf");
			tcUsersFile = new File(tcUsersFile, "tomcat-users.xml");
			try {
				Document tcUsers = XmlUtil.getDocument(tcUsersFile);
				Element tcUsersRoot = tcUsers.getDocumentElement();
				Element server = XmlUtil.getFirstNamedChild(ctpRoot, "Server");

				HashSet<String> extraRoles = new HashSet<String>();

				//Get the users class name
				String usersClassName = server.getAttribute("usersClassName");
				if (usersClassName.equals("")) usersClassName = "org.rsna.server.UsersXmlFileImpl";

				//Only copy users if this is a UsersXmlFileImpl
				if (usersClassName.equals("org.rsna.server.UsersXmlFileImpl")) {
					UsersXmlFileImpl users = (UsersXmlFileImpl)Users.getInstance(usersClassName, null);
					NodeList nl = tcUsersRoot.getElementsByTagName("user");
					for (int i=0; i<nl.getLength(); i++) {
						Element tcUser = (Element)nl.item(i);
						String username = tcUser.getAttribute("username");
						User ctpUser = users.getUser(username);
						if (ctpUser == null) {
							cp.println("   adding user: \""+username+"\"");
							ctpUser = new User(username, tcUser.getAttribute("password"));
							String[] roles = tcUser.getAttribute("roles").replaceAll("\\s","").split(",");

							for (String role : roles) {
								if (role.contains("admin")) ctpUser.addRole("admin");
								else if (role.contains("author")) ctpUser.addRole("author");
								else if (role.contains("publisher")) ctpUser.addRole("publisher");
								else if (role.contains("manager")) ; //ignore
								else if (!role.contains("-")) {
									ctpUser.addRole(role);
									if (!extraRoles.contains(role)) {
										extraRoles.add(role);
										cp.println("   adding site-specific role: "+role);
									}
								}
							}
							if (!users.addUser(ctpUser)) {
								cp.println("   ***UNABLE TO ADD USER: \""+username+"\"");
								System.out.println("UNABLE TO ADD USER: \""+username+"\"");
							}
						}
						else cp.println("   skipping pre-existing user: \""+username+"\"");
					}
					if (extraRoles.size() != 0) {
						Element mircRoot = mircConfig.getXML().getDocumentElement();
						String[] currentRoles = mircRoot.getAttribute("roles").split(",");
						for (String role : currentRoles) extraRoles.add(role);
						currentRoles = extraRoles.toArray( new String[extraRoles.size()] );
						StringBuffer sb = new StringBuffer();
						for (String role : currentRoles) {
							if (sb.length() != 0) sb.append(",");
							sb.append(role);
						}
						mircRoot.setAttribute("roles", sb.toString());
						mircConfig.save();
					}
				}
				else {
					cp.println("   Unable to copy users because the CTP site");
					cp.println("   uses the wrong Users class ("+usersClassName+").");
				}
			}
			catch (Exception ex) {
				cp.print(StringUtil.getStackTrace(ex));
			}
		}

		private void copyPrefs() {
			cp.println("\nCopying preferences");
			try {
				Preferences prefs = Preferences.load(mircsiteRoot);
				for (File ss : tcSS) {
					cp.println("   Storage Service: "+ss);
					File authors = new File(ss, "authors.xml");
					if (authors.exists()) {
						Document xml = XmlUtil.getDocument(authors);
						Element root = xml.getDocumentElement();
						Node child = root.getFirstChild();
						while (child != null) {
							if ((child instanceof Element) && child.getNodeName().equals("author")) {
								Element a = (Element)child;
								String username = a.getAttribute("user");
								if (!prefs.hasUser(username)) {
									cp.println("      setting preferences for "+username);
									String name = XmlUtil.getValueViaPath(a, "author/name").trim();
									String affiliation = XmlUtil.getValueViaPath(a, "author/affiliation").trim();
									String contact = XmlUtil.getValueViaPath(a, "author/contact").trim();
									prefs.setAuthorInfo(username, name, affiliation, contact);
								}
								else cp.println("      skipping pre-existing user preferences ("+username+")");
							}
							child = child.getNextSibling();
						}
					}
					else cp.println("      authors.xml file does not exist (skipping)");
				}
				prefs.close();
			}
			catch (Exception ex) {
				cp.print(StringUtil.getStackTrace(ex));
			}
		}

		private void copyStorageServices() {
			cp.println("\nCopying storage services");
			File ctpStorage = new File(mircsiteRoot, "storage");
			for (File ss : tcSS) {
				try {
					File storage = new File(ss, "storage.xml");
					Document storageXML = XmlUtil.getDocument(storage);
					Element storageRoot = storageXML.getDocumentElement();
					Element sitenameElement = XmlUtil.getFirstNamedChild(storageRoot, "sitename");
					String title = sitenameElement.getTextContent();
					String ssid = getLocalLibraryID(title);
					File ctpDocs = new File(ctpStorage, ssid);
					ctpDocs = new File(ctpDocs, "docs");
					File tcDocs = new File(ss, "documents");
					copyStorageService(ssid, tcDocs, ctpDocs);

					//set the parameters
					String jpegquality = "-1";
					String timeout = "0";
					String tagline = "";
					String maxsize = "75";
					String subenb = "yes";
					String zipenb = "yes";
					String authenb = "yes";
					String dcmenb = "yes";
					String tceenb = "yes";
					String autoindex = "no";

					String x;
					int submax = 0;
					int zipmax = 0;

					Element serviceE = XmlUtil.getFirstNamedChild(storageRoot, "service");

					if (serviceE != null) {
						x = serviceE.getAttribute("jpegquality").trim();
						if (!x.equals("")) jpegquality = x;

						x = serviceE.getAttribute("ddtimeout").trim();
						if (!x.equals("")) timeout = x;
					}

					Element taglineE = XmlUtil.getFirstNamedChild(storageRoot, "tagline");
					if (taglineE != null) tagline = taglineE.getTextContent().trim();

					Element submitE = XmlUtil.getFirstNamedChild(storageRoot, "submit-service");
					if (submitE != null) {
						Element docE = XmlUtil.getFirstNamedChild(submitE, "doc");
						x = docE.getAttribute("enabled").trim();
						if (!x.equals("")) subenb = x;
						submax = StringUtil.getInt(docE.getAttribute("maxsize"), 0);

						Element zipE = XmlUtil.getFirstNamedChild(submitE, "zip");
						x = zipE.getAttribute("enabled").trim();
						if (!x.equals("")) zipenb = x;
						zipmax = StringUtil.getInt(zipE.getAttribute("maxsize"), 0);

						if ((submax > 0) || (zipmax > 0)) {
							maxsize = Integer.toString( Math.max(submax, zipmax) );
						}
					}

					Element authorE = XmlUtil.getFirstNamedChild(storageRoot, "author-service");
					if (authorE != null) {
						x = authorE.getAttribute("enabled").trim();
						if (!x.equals("")) authenb = x;

						x = authorE.getAttribute("autoindex").trim();
						if (!x.equals("")) autoindex = x;
					}

					Element dicomE = XmlUtil.getFirstNamedChild(storageRoot, "dicom-service");
					if (dicomE != null) {
						x = dicomE.getAttribute("enabled").trim();
						if (!x.equals("")) dcmenb = x;
					}

					Element tceE = XmlUtil.getFirstNamedChild(storageRoot, "tce-service");
					if (tceE != null) {
						x = tceE.getAttribute("enabled").trim();
						if (!x.equals("")) tceenb = x;
					}

					Element lib = mircConfig.getLocalLibrary(ssid);
					if (lib != null) {
						lib.setAttribute("jpegquality", jpegquality);
						lib.setAttribute("timeout", timeout);
						lib.setAttribute("maxsize", maxsize);
						lib.setAttribute("subenb", subenb);
						lib.setAttribute("zipenb", zipenb);
						lib.setAttribute("authenb", authenb);
						lib.setAttribute("dcmenb", dcmenb);
						lib.setAttribute("tceenb", tceenb);
						lib.setAttribute("autoindex", autoindex);
						taglineE = XmlUtil.getFirstNamedChild(lib, "tagline");
						if (taglineE ==  null) {
							taglineE = lib.getOwnerDocument().createElement("tagline");
							lib.appendChild(taglineE);
						}
						taglineE.setTextContent(tagline);
					}
					mircConfig.insertLibrary(lib);
					mircConfig.sortLibraries();
				}
				catch (Exception ex) {
					cp.print(StringUtil.getStackTrace(ex));
				}
			}
		}

		private void copyStorageService(String ssid, File tcDocs, File ctpDocs) {
			cp.println(Color.black, "   copying "+tcDocs.getParentFile().getName()+" to "+ctpDocs.getParentFile().getName());

			copyDir(tcDocs, ctpDocs, 0);
			if (indexDocuments.isSelected()) {
				cp.print("   indexing "+ssid+"...");
				Index index = Index.getInstance(ssid);
				if (index != null) {
					cp.print(" got index instance...");
					boolean ok = index.rebuild(interval);
					if (ok) cp.println(" OK");
					else cp.println(Color.red, " FAILED");
				}
				else {
					cp.println(Color.red, " UNABLE TO OBTAIN THE INDEX INSTANCE");
					cp.print(Color.black, "");
				}
			}
			else cp.println("   skipping the index step for "+ssid);
		}

		private int copyDir(File in, File out, int count) {
			if (forceCopy || !out.exists()) {
				out.mkdirs();
				File[] files = in.listFiles();
				for (File inFile : files) {
					File outFile = new File(out, inFile.getName());
					if (inFile.isFile()) {
						long lmdate = inFile.lastModified();
						if (inFile.getName().toLowerCase().endsWith(".xml")) {
							try {
								Document xml = XmlUtil.getDocument(inFile);
								Element root = xml.getDocumentElement();
								if (root.getNodeName().equals("MIRCdocument")) {
									count++;
									Element title = XmlUtil.getFirstNamedChild(root, "title");
									if (title != null) {
										String s = title.getTextContent().replaceAll("\\s+"," ").trim();
										cp.println("      " + count + ": " + s);
									}
								}
							}
							catch (Exception ignore) { }
						}
						FileUtil.copy(inFile, outFile);
						outFile.setLastModified(lmdate);
					}
					else {
						count = copyDir(inFile, outFile, count);
					}
				}
			}
			return count;
		}

		private String getLocalLibraryID(String title) throws Exception {
			title = title.trim();
			Set<String> ssids = mircConfig.getLocalLibraryIDs();

			//See if the requested title already exists
			for (String ssid: ssids) {
				Element localLibrary = mircConfig.getLocalLibrary(ssid);
				Element titleElement = XmlUtil.getFirstNamedChild(localLibrary, "title");
				if (titleElement.getTextContent().trim().equals(title)) return ssid;
			}

			//If ss1 is empty, then use it
			File mircsite = new File(ctp, "mircsite");
			File storage = new File(mircsite, "storage");
			File ss1 = new File(storage, "ss1");
			File docs = new File(ss1, "docs");
			if (!docs.exists() || (docs.listFiles().length == 0)) return "ss1";

			//No luck, create a new library
			String ssid = mircConfig.getNewLocalLibraryID();
			String address = "/storage/" + ssid;
			String enabled = "yes";
			Element lib = mircConfig.createLocalLibrary(ssid, title, address, enabled);

			//Insert it in the configuration
			mircConfig.insertLibrary( lib );
			mircConfig.sortLibraries();
			return ssid;
		}


		private Element getSpecifiedChild(Element parent, String eName, String aName, String aValue) {
			Node child = parent.getFirstChild();
			while (child != null) {
				if (child instanceof Element) {
					Element e = (Element)child;
					if (e.getTagName().equals(eName)) {
						if (e.getAttribute(aName).equals(aValue)) return e;
					}
				}
				child = child.getNextSibling();
			}
			return null;
		}

	}

}

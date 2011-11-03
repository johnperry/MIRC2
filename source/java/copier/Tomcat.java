/*---------------------------------------------------------------
*  Copyright 2011 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package copier;

import java.awt.Component;
import java.io.File;
import java.util.HashSet;
import javax.swing.JFileChooser;

public class Tomcat {

	static JFileChooser chooser = null;

	//Get a directory from the file chooser.
	public static File getDirectory(Component parent) {
		if (chooser == null) {
			chooser = new JFileChooser();
			File dir = new File(System.getProperty("user.dir"));
			chooser.setCurrentDirectory(dir);
		}
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile();
		}
		return null;
	}

	//Find the Tomcat instance.
	public static File findTomcat() {
		File root = new File(System.getProperty("user.dir"));
		root = new File(root.getAbsolutePath());
		File parent = root;
		while ( (parent = root.getParentFile()) != null) root = parent;
		return findTomcat(root, 3);
	}

	private static File findTomcat(File dir, int level) {
		//First look in the suggested place
		File tomcat = walkTree(dir, level);
		if (tomcat != null) return tomcat;

		//No luck there, try C:/Program Files if it's there.
		File programFiles = new File("C:/Program Files");
		if (programFiles.exists()) {
			tomcat = walkTree(programFiles, 2);
		}
		if (tomcat != null) return tomcat;

		//No luck there, try C:/Program Files (x86) if it's there.
		programFiles = new File("C:/Program Files (x86)");
		if (programFiles.exists()) return walkTree(programFiles, 2);
		else return null;
	}

	//Walk the tree under a directory and find Tomcat.
	private static File walkTree(File dir, int level) {
		if (dir == null) return null;
		if (!dir.exists()) return null;
		if (!dir.isDirectory()) return null;
		if (dir.getName().toLowerCase().contains("windows")) return null;

		//See if this directory is Tomcat.
		String name = dir.getName().toLowerCase();
		if (name.contains("tomcat") || name.contains("instance")) {
			boolean check = contentsCheck(dir, names);
			if (check) return dir;
		}

		//It's not; see if it contains Tomcat
		if (level > 0) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (int i=0; i<files.length; i++) {
					if ((files[i] != null) && files[i].exists() && files[i].isDirectory()) {
						File tomcat = walkTree(files[i], level-1);
						if (tomcat != null) return tomcat;
					}
				}
			}
		}
		return null;
	}

	public static File[] getStorageServices(File tomcat) {
		HashSet<File> ssSet = new HashSet<File>();
		File webapps = new File(tomcat, "webapps");
		File[] files = webapps.listFiles();
		for (File file : files) {
			if (contentsCheck(file, ssnames)) {
				ssSet.add(file);
			}
		}
		return ssSet.toArray( new File[ssSet.size()] );
	}

	//The list of directories which must be present to believe
	//that this is an instance of Tomcat's root directory.
	static final String[] names = new String[] {"conf", "webapps"};

	//The list of directories which must be present to believe
	//that a directory is an instance of a storage service.
	static final String[] ssnames = new String[] {"MIRCdocument.xsl", "documents"};

	//An empty array of Strings.
	static final String[] empty = new String[] {};

	//See if a directory contains an array of filenames
	public static boolean contentsCheck(File dir) {
		return contentsCheck(dir, names, empty);
	}

	//See if a directory contains an array of filenames
	public static boolean contentsCheck(File dir, String[] include) {
		return contentsCheck(dir, include, empty);
	}

	//See if a directory contains an array of filenames and doesn't contain another array of filenames
	public static boolean contentsCheck(File dir, String[] include, String[] exclude) {
		if (!dir.isDirectory()) return false;
		if (include != null) {
			for (int i=0; i<include.length; i++) {
				File file = new File(dir, include[i]);
				if (!file.exists()) {
					return false;
				}
			}
		}
		if (exclude != null) {
			for (int i=0; i<exclude.length; i++) {
				File file = new File(dir, exclude[i]);
				if (file.exists()) {
					return false;
				}
			}
		}
		return true;
	}
}

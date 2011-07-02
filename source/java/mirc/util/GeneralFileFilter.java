/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.util;

import java.io.*;

/**
 * An implementation of java.util.FileFilter that matches all directories
 * plus files ending with specific strings, including an asterisk wildcard,
 * which matches all files. It also provides methods for setting
 * and retrieving the matchable extensions.
 * <ul>
 * <li>Although the word "extension" is used, it really means the last part of the filename.</li>
 * <li>The extension "data.dat" matches "main_data.dat" but not "abc.dat".</li>
 * <li>The extension "dat" matches both "data.dat" and "xyz_dat",
 * but not "xyz_dat.new".</li>
 * <li>The extension ".*" matches all filenames, not just those that have extensions.
 * It has the same effect as "*".</li>
 * <li>The filtering operation is implemented by "endsWith", not by a regex match.</li>
 * </ul>
 */
public class GeneralFileFilter implements FileFilter {

	String[] extensions;

	/**
	 * Class constructor creating a GeneralFileFilter that accepts only directories.
	 */
	public GeneralFileFilter() {
		extensions = new String[0];
	}

	/**
	 * Class constructor creating a GeneralFileFilter from a String containing a list
	 * of extensions.
	 * @param extensionString the String containing the list of extensions.
	 */
	public GeneralFileFilter(String extensionString) {
		setExtensions(extensionString);
	}

	/**
	 * Class constructor creating a GeneralFileFilter that matches an array of extensions.
	 * @param extensions the array of extensions.
	 */
	public GeneralFileFilter(String extensions[]) {
		this.extensions = extensions;
	}

	/**
	 * Install a new array of extensions.
	 * @param extensions the array of extensions.
	 */
	public void setExtensions(String extensions[]) {
		this.extensions = extensions;
	}

	/**
	 * Install a new array of extensions by splitting a comma-separated String.
	 * @param extensionString the String containing the list of extensions.
	 */
	public void setExtensions(String extensionString) {
		this.extensions = extensionString.split(",");
	}

	/**
	 * Get the current array of extensions.
	 * @return the array of extensions.
	 */
	public String[] getExtensions() {
		return extensions;
	}

	/**
	 * Get the current array of extensions as a comma-separated String.
	 * @return the list of extensions as a comma-separated String.
	 */
	public String getExtensionString() {
		String string = extensions[0];
		for (int i=1; i<extensions.length; i++) string += "," + extensions[i];
		return string;
	}

	/**
	 * Add an extension to the current array of extensions.
	 * @param extension the extension to add to the current array.
	 */
	public void addExtension(String extension) {
		String[] temp = new String[extensions.length + 1];
		for (int i=0; i<extensions.length; i++) {
			temp[i] = extensions[i];
		}
		temp[extensions.length] = extension;
		extensions = temp;
	}

	/**
	 * Determine whether a file matches the filter. All directories
	 * except WINDOWS and WINNT match. Any file ending with one of the extensions
	 * matches. If any extension includes an asterisk, the file matches. Note that
	 * this is not an implementation of a regex match.
	 * @param file the file to test for a match
	 * @return whether the file matches the filter.
	 */
	public boolean accept(File file) {
		String name = file.getName();
		if (file.isDirectory()) {
			if (name.equals("WINDOWS")) return false;
			if (name.equals("WINNT")) return false;
			return true;
		}
		name = name.toLowerCase();
		for (int i=0; i<extensions.length; i++) {
			if (extensions[i].indexOf("*") != -1) return true;
			if (name.endsWith(extensions[i])) return true;
		}
		return false;
	}

	/**
	 * Return a String describing the filter for display by a chooser.
	 * @return the comma-separated list of extensions or, if the list is empty.
	 * the text "Directories only".
	 */
	public String getDescription() {
		if (extensions.length == 0) return "Directories only";
		return getExtensionString();
	}

}

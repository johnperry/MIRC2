/*---------------------------------------------------------------
*  Copyright 2010 by the Radiological Society of North America
*
*  This source software is released under the terms of the
*  RSNA Public License (http://mirc.rsna.org/rsnapubliclicense)
*----------------------------------------------------------------*/

package mirc.files;

import java.io.File;
import org.rsna.server.Path;

/**
 * A helper class to find a File Service directory from a Path.
 */
public class FilePath {

	/**
	 * The root of the tree of directories identified by the path.
	 * This will either be Personal/{username} or Shared.
	 */
	public File baseDir = null;

	/**
	 * The root of the tree of subdirectories identified by the path
	 * for files. This will either be Personal/{username}/Files or Shared/Files.
	 */
	public File filesDir = null;

	/**
	 * The root of the tree of subdirectories identified by the path
	 * for icons. This will either be Personal/{username}/Icons or Shared/Icons.
	 */
	public File iconsDir = null;

	/**
	 * The URL path pointing to the identified resource file.
	 */
	public String filesURL = null;

	/**
	 * The URL path pointing to the identified resource icon.
	 */
	public String iconsURL = null;

	/**
	 * The title of the identified resource.
	 */
	public String dirTitle = null;

	boolean isShared = false;
	boolean isPersonal = false;

	/**
	 * Construct a FilePath, load the public fields and create the
	 * necessary directories.
	 * @param path the path to the resource in the form of a URL path.
	 * The path does not include the Files or Icons path element because
	 * a path identifies a resource, which has an entry in both the Files
	 * and Icons trees. A path to a resource starts with <code>Personal/...</code>
	 * or <code>Shared/...</code>
	 * @param username the username of the user. Note: if the username
	 * does not correspond to that of a user, no check is made and the personal
	 * fields do not correspond to real resources.
	 * @param shared the FileService's Shared directory.
	 * @param personal the FileService's Personal directory, under which
	 * all user directories are located.
	 */
	public FilePath(Path path, String username, File shared, File personal) {

		username = (username != null) ? username.trim() : "";

		String category = path.element(0);
		path = new Path( path.subpath(1) );
		String rest = path.path().substring(1);

		if (username.equals("")) category = "Shared";

		if (category.equals("Shared")) baseDir = shared;
		else  baseDir = new File(personal, username);

		isShared = category.equals("Shared");
		isPersonal = category.equals("Personal");

		filesDir = new File(baseDir, "Files");
		iconsDir = new File(baseDir, "Icons");

		if (!rest.equals("")) {
			filesDir = new File(filesDir, rest);
			iconsDir = new File(iconsDir, rest);
			rest = "/" + rest;
		}

		dirTitle = category + rest;
		filesURL = category + "/Files" + rest;
		iconsURL = category + "/Icons" + rest;

		if (isShared || !username.equals("")) {
			filesDir.mkdirs();
			iconsDir.mkdirs();
		}
	}

	/**
	 * Determine whether the identified resource is shared or personal.
	 * @return true if the resource is shared; false otherwise.
	 */
	public boolean isShared() {
		return isShared;
	}

	/**
	 * Determine whether the identified resource is shared or personal.
	 * @return true if the resource is personal; false otherwise.
	 */
	public boolean isPersonal() {
		return isPersonal;
	}
}

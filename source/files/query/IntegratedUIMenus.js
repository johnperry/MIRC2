var fileItems = new Array(
		new Item("New folder", newFolder, "newfolder"),
		new Item("Rename folder", renameFolder, "renamefolder"),
		new Item("Delete folder", deleteFolder, "deletefolder"),
		new Item("", null),
		new Item("Upload file", uploadFile, "uploadfile"),
		new Item("Rename file", renameFile, "renamefile"),
		new Item("Delete file(s)", deleteFiles, "deletefiles"),
		new Item("Export file(s)", exportFiles, "exportfiles"),
		new Item("", null),
		new Item("Select all", selectAllFiles, "selectall"),
		new Item("Deselect all", deselectAllFiles, "deselectall") );

var fileMenu = new Menu("File Cabinets", fileItems, "filemenu");
var closeboxURL = "/icons/closebox.gif";

//Set the enables on the File menu
function setFileEnables() {

	var nFiles = currentFileTreeNode ? getCabinetFiles().length : 0;
	var nSelected = currentFileTreeNode ? getSelectedFilesCount() : 0;
	var nChildDirs = currentFileTreeNode ? currentFileTreeNode.trees.length : -1;
	var root = isRootFolder(currentFileTreeNode);

	fileMenuBar.setEnable("newfolder", currentFileTreeNode);
	fileMenuBar.setEnable("deletefolder", currentFileTreeNode && !root);
	fileMenuBar.setEnable("renamefolder", currentFileTreeNode && !root);
	fileMenuBar.setEnable("uploadfile", currentFileTreeNode);
	fileMenuBar.setEnable("renamefile", (nSelected == 1));
	fileMenuBar.setEnable("deletefiles", (nSelected > 0));
	fileMenuBar.setEnable("exportfiles", (nSelected > 0));
	fileMenuBar.setEnable("selectall", (nFiles > 0));
	fileMenuBar.setEnable("deselectall", (nSelected > 0));
}

function isRootFolder(node) {
	if (node) {
		var path = node.getPath();
		return (path == "Shared") || (path == "Personal");
	}
	return false;
}

//Handlers for the File Menu
//
function newFolder() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);

	var div = document.createElement("DIV");
	div.className = "content";
	var p = document.createElement("P");
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("Folder name:\u00A0"));
	var text = document.createElement("INPUT");
	text.id = "etext1";
	text.className = "textbox";
	p.appendChild(text);
	div.appendChild(p);

	showDialog("ediv", 400, 200, "New Folder", closeboxURL, "Create New Folder", div, doNewFolder, hidePopups);
	window.setTimeout("document.getElementById('etext1').focus()", 500);
}
function doNewFolder(event) {
	var name = document.getElementById("etext1").value;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	name = trim(name);
	if (name.length == 0) return;
	name = name.replace(/\s+/g,"_");
	var path = currentFileTreeNode.getPath();
	var req = new AJAX();
	req.GET("/files/createFolder/"+path+"/"+name, req.timeStamp(), null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		var parent = currentFileTreeNode.parent;
		var treeManager = currentFileTreeNode.treeManager;
		var level = currentFileTreeNode.level
		var tree = new Tree(treeManager, parent, level);
		tree.load(child);
		var state = treeManager.getState();
		currentFileTreeNode = currentFileTreeNode.replaceChildren(tree);
		treeManager.display(state);
		currentFileTreeNode.expand();
		currentFileTreeNode.showPath();
	}
	else alert("The attempt to create the directory failed.");
}

function renameFolder() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);

	var div = document.createElement("DIV");
	div.className = "content";
	var p = document.createElement("P");
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("New name:\u00A0"));
	var text = document.createElement("INPUT");
	text.id = "etext1";
	text.className = "textbox";
	p.appendChild(text);
	div.appendChild(p);

	showDialog("ediv", 400, 200, "Rename Folder", closeboxURL, "Rename Folder", div, doRenameFolder, hidePopups);
	window.setTimeout("document.getElementById('etext1').focus()", 500);
}
function doRenameFolder(event) {
	var name = document.getElementById("etext1").value;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	name = trim(name);
	if (name.length == 0) return;
	name = name.replace(/\s+/g,"_");
	var treeManager = currentFileTreeNode.treeManager;
	var path = currentFileTreeNode.getPath();
	var req = new AJAX();
	req.GET("/files/renameFolder/"+path, "newname="+name+"&"+req.timeStamp(), null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		if ((root != null) && (root.tagName == "ok")) {
			currentFileTreeNode.name = name;
			var state = treeManager.getState();
			treeManager.display(state);
			currentFileTreeNode.showPath();
			return;
		}
	}
	alert("The attempt to rename the directory failed.");
}

function renameFile() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);

	var div = document.createElement("DIV");
	div.className = "content";
	var p = document.createElement("P");
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("New name:\u00A0"));
	var text = document.createElement("INPUT");
	text.id = "etext1";
	text.className = "textbox";
	p.appendChild(text);
	div.appendChild(p);

	showDialog("ediv", 400, 200, "Rename File", closeboxURL, "Rename File", div, doRenameFile, hidePopups);
	window.setTimeout("document.getElementById('etext1').focus()", 500);
}
function doRenameFile(event) {
	var name = document.getElementById("etext1").value;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	name = trim(name);
	if (name.length == 0) return;
	if (getSelectedCount() != 1) return;
	var currentName = getSelected();
	name = name.replace(/\s+/g,"_");
	var req = new AJAX();
	req.GET("/files/renameFile/"+currentPath,"oldname="+currentName+"&newname="+name+"&"+req.timeStamp(), null);
	if (req.success()) showCurrentFileDirContents();
	else alert("The attempt to rename the file failed.");
}

function deleteFolder() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	showTextDialog("ediv", 400, 210, "Are you sure?", closeboxURL, "Delete?",
		"Are you sure you want to delete the selected folder and all its files and subfolders?",
		deleteFolderHandler, hidePopups);
}

function deleteFolderHandler() {
	hidePopups();
	var treeManager = currentFileTreeNode.treeManager;
	var path = currentFileTreeNode.getPath();
	var req = new AJAX();
	req.GET("/files/deleteFolder/"+path, req.timeStamp(), null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		var parent = currentFileTreeNode.parent;
		var tree = new Tree(parent.treeManager, parent.parent, parent.level);
		tree.load(child);
		var state = treeManager.getState();
		currentFileTreeNode = parent.replaceChildren(tree);
		treeManager.display(state);
		currentFileTreeNode.expand();
		showCurrentFileDirContents();
		currentFileTreeNode.showPath();
	}
	else alert("The attempt to delete the directory failed.");
}

function uploadFile() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);

	var div = document.createElement("DIV");
	div.className = "content";

	var path = currentFileTreeNode.getPath();

	var form = document.createElement("FORM");
	form.method = "post";
	form.target = "_self";
	form.encoding = "multipart/form-data";
	form.acceptCharset = "UTF-8";
	form.action = "/files/uploadFile/"+path;
	div.appendChild(form);

	var p = document.createElement("P");
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("Upload to: "));
	p.appendChild(document.createTextNode(path));
	form.appendChild(p);

	var input = document.createElement("INPUT");
	input.style.width = "80%";
	input.name = "filecontent";
	input.id = "selectedFile";
	input.type = "file";
	form.appendChild(input);

	form.appendChild(document.createElement("BR"));
	form.appendChild(document.createElement("BR"));

	var cb = document.createElement("INPUT");
	cb.name = "anonymize";
	cb.type = "checkbox";
	cb.value = "yes";
	form.appendChild(cb);
	form.appendChild(document.createTextNode(" Anonymize DICOM Files"));

	form.appendChild(document.createElement("BR"));
	form.appendChild(document.createElement("BR"));

	var submit = document.createElement("INPUT");
	submit.type = "submit";
	submit.value = "Submit File";
	form.appendChild(submit);

	showDialog("ediv", 475, 270, "Upload File", closeboxURL, "Upload File", div);
}

function deleteFiles() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	if (getSelectedCount() > 0) {
		showTextDialog("ediv", 400, 195, "Are you sure?", closeboxURL, "Delete?",
			"Are you sure you want to delete the selected files?",
			deleteFilesHandler, hidePopups);
	}
}

function deleteFilesHandler() {
	hidePopups();
	var selected = getSelected();
	if (selected != "") {
		selected = encodeURIComponent(selected);
		var req = new AJAX();
		req.GET("/files/deleteFiles/"+currentPath, "list="+selected+"&"+req.timeStamp(), null);
		if (req.success()) {
			showCurrentFileDirContents();
		}
		else alert("The attempt to delete the files failed.");
	}
}

function exportFiles() {
	var selected = getSelected();
	if (selected != "") {
		var ts = new AJAX().timeStamp();
		selected = encodeURIComponent(selected);
		window.open("/files/exportFiles/"+currentPath+"?list="+selected+"&"+ts,"_self");
	}
}

function selectAllFiles() {
	var files = getCabinetFiles();
	for (var i=0; i<files.length; i++) files[i].className = "sel";
	setFileEnables();
}

function deselectAllFiles() {
	var files = getCabinetFiles();
	for (var i=0; i<files.length; i++) files[i].className = "desel";
	setFileEnables();
}

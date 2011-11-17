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
function isSharedFolder(path) {
	return (path.indexOf("Shared") == 0);
}
function isPersonalFolder(path) {
	return (path.indexOf("Personal") == 0);
}

var currentFileTreeNode = null;
var lastFileClicked = -1;

//Handlers for tree selection
//
function showFileDirContents(event) {
	var source = getSource(getEvent(event));
	currentFileTreeNode = source.treenode;
	showCurrentFileDirContents();
}

function showCurrentFileDirContents() {
	deselectAll();
	var currentFileTreePath = currentFileTreeNode.getPath();
	fileTreeManager.closePaths();
	currentFileTreeNode.showPath();
	queryIsActive = false;
	var req = new AJAX();
	req.GET("/files/mirc/"+currentFileTreePath, req.timeStamp(), null);
	if (req.success()) {
		var right = document.getElementById("right");
		right.style.overflow = "auto";
		while (right.firstChild) right.removeChild(right.firstChild);
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		while (child) {
			if ((child.nodeType == 1) && (child.tagName == "file")) {
				var img = document.createElement("IMG");
				img.className = "desel";
				img.ondblclick = cabinetFileDblClicked;
				img.onmousedown = startImageDrag;
				img.setAttribute("src", "/files/"+child.getAttribute("iconURL"));
				img.setAttribute("title", child.getAttribute("title"));
				img.xml = child;
				right.appendChild(img);
			}
			child = child.nextSibling;
		}
		setFileEnables();
	}
	else alert("The attempt to get the directory contents failed.");
}

//Handlers for MIRC file cabinet selection in the right pane
//
function cabinetFileClicked(theEvent) {
	theEvent = getEvent(theEvent)
	stopEvent(theEvent);
	var source = getSource(theEvent);
	var currentFileClicked = getClickedFile(source);
	if (currentFileClicked == -1) return;

	if (!theEvent.shiftKey && !theEvent.ctrlKey) {
		deselectAllFiles();
		source.className = "sel";
		lastFileClicked = currentFileClicked;
	}

	else if (getEvent(theEvent).ctrlKey) {
		if (source.className == "sel") source.className = "desel";
		else source.className ="sel";
		lastFileClicked = currentFileClicked;
	}

	else {
		if (lastFileClicked == -1) {
			lastFileClicked = currentFileClicked;
			source.className = "sel";
		}
		else {
			var files = getCabinetFiles();
			var start = Math.min(lastFileClicked,currentFileClicked);
			var stop = Math.max(lastFileClicked,currentFileClicked);
			for (var i=start; i<=stop; i++) files[i].className = "sel";
		}
	}
	setFileEnables();
}

function cabinetFileDblClicked(theEvent) {
	var theEvent = getEvent(theEvent)
	stopEvent(theEvent);
	var source = getSource(theEvent);
	var currentFileClicked = getClickedFile(source);
	if (currentFileClicked == -1) return;
	var fileURL = "/files/" + source.xml.getAttribute("fileURL");
	deselectAllFiles();
	source.className = "sel";
	lastFileClicked = currentFileClicked;

	if (theEvent.altKey) {
		var filename = source.getAttribute("title");
		if ((filename.toLowerCase().lastIndexOf(".dcm") == filename.length - 4) ||
					(filename.replace(/[\.\d]/g,"").length == 0)) {
			fileURL += "?list";
		}
	}
	window.open(fileURL, "_blank");
}

function getClickedFile(file) {
	var files = getCabinetFiles();
	for (var i=0; i<files.length; i++) {
		if (file === files[i]) {
			return i;
		}
	}
	return -1;
}

function getCabinetFiles() {
	var right = document.getElementById("right");
	return right.getElementsByTagName("IMG");
}

function getSelectedFiles() {
	var files = getCabinetFiles();
	var list = "";
	for (var i=0; i<files.length; i++) {
		if (files[i].className == "sel") {
			if (list != "") list += "|";
			list += files[i].getAttribute("title");
		}
	}
	return list;
}

function getSelectedFilesCount() {
	var files = getCabinetFiles();
	var n = "";
	for (var i=0; i<files.length; i++) {
		if (files[i].className == "sel") n++;
	}
	return n;
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
	if (getSelectedFilesCount() != 1) return;
	var currentName = getSelectedFiles();
	var path = currentFileTreeNode.getPath();
	name = name.replace(/\s+/g,"_");
	var req = new AJAX();
	req.GET("/files/renameFile/"+path,"oldname="+currentName+"&newname="+name+"&"+req.timeStamp(), null);
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

	var hidden = document.createElement("INPUT");
	hidden.type = "hidden";
	hidden.name = "ui";
	hidden.value = "integrated";
	form.appendChild(hidden);

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
	var selected = getSelectedFiles();
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
	var selected = getSelectedFiles();
	if (selected != "") {
		var ts = new AJAX().timeStamp();
		selected = encodeURIComponent(selected);
		var path = currentFileTreeNode.getPath();
		window.open("/files/exportFiles/"+path+"?list="+selected+"&"+ts,"_self");
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

//**************************************
//**** Drag/drop handler for images ****
//**************************************

//This handler starts the drag of selected images.
function startImageDrag(event) {
	event = getEvent(event);
	if (!event.altKey) {
		var right = document.getElementById("right");
		var scrollTop = right.scrollTop;
		var node = getSource(event);
		var list = new Array();
		list[list.length] = getDragableImgDiv(node, event.clientX, event.clientY, scrollTop);
		var files = right.getElementsByTagName("IMG");
		for (var i=0; i<files.length; i++) {
			if ((files[i].className == "sel") && !(files[i] === node)) {
				list[list.length] = getDragableImgDiv(files[i], event.clientX, event.clientY, scrollTop);
			}
		}
		dragImage(list, event);
	}
	cabinetFileClicked(event);
}

//Make a div containing an image to drag.
function getDragableImgDiv(node, clientX, clientY, scrollTop) {
	var pos = findObject(node);
	setStatusLine(pos.y + "; " + scrollTop);
	var div = document.createElement("DIV");
	div.deltaX = clientX - parseInt(pos.x);
	div.deltaY = clientY - parseInt(pos.y);
	div.style.position = "absolute";
	div.style.width = pos.w;
	div.style.height = pos.h;
	div.style.left = pos.x;
	div.style.top = pos.y - scrollTop;
	div.style.visibility = "visible";
	div.style.display = "block";
	div.style.overflow = "hidden";
	div.style.zIndex = 5;
	div.style.backgroundColor = "transparent";
	div.style.filter = "alpha(opacity=40, style=0)";
	div.parentScrollTop = scrollTop;
	var img = document.createElement("IMG");
	img.src = node.src;
	div.appendChild(img);
	div.title = node.title;
	document.body.appendChild(div);
	return div;
}

//Drag and drop an image
function dragImage(list, event) {
	var node = list[0];
	if (document.addEventListener) {
		document.addEventListener("mousemove", dragIt, false);
		document.addEventListener("mouseup", dropIt, false);
	}
	else {
		node.attachEvent("onmousemove", dragIt);
		node.attachEvent("onmouseup", dropIt);
		node.setCapture();
	}
	if (event.stopPropagation) event.stopPropagation();
	else event.cancelBubble = true;
	if (event.preventDefault) event.preventDefault();
	else event.returnValue = false;

	function dragIt(evt) {
		if (!evt) evt = window.event;
		for (var i=0; i<list.length; i++) {
			list[i].style.left = (evt.clientX - list[i].deltaX) + "px";
			list[i].style.top = (evt.clientY - list[i].deltaY - list[i].parentScrollTop) + "px";
		}
		if (evt.stopPropagation) event.stopPropagation();
		else evt.cancelBubble = true;
	}

	function dropIt(evt) {
		if (!evt) evt = window.event;
		handleDrop(evt);
		if (document.addEventListener) {
			document.removeEventListener("mouseup", dropIt, true);
			document.removeEventListener("mousemove", dragIt, true);
		}
		else {
			node.detachEvent("onmousemove", dragIt);
			node.detachEvent("onmouseup", dropIt);
			node.releaseCapture();
		}
		if (evt.stopPropagation) event.stopPropagation();
		else evt.cancelBubble = true;
	}

	function handleDrop(evt) {
		var files = "";
		for (var i=0; i<list.length; i++) {
			if (files != "") files += "|";
			files += list[i].title;
			document.body.removeChild(list[i]);
		}
		var left = document.getElementById("left");
		var scrollTop = left.scrollTop;
		var pos = findObject(left);
		if (evt.clientX > pos.w) return false;
		var sourcePath = currentFileTreeNode.getPath();
		var destTree = fileTreeManager.getTreeForCoords(evt.clientX, evt.clientY + scrollTop);
		if (destTree) {
			var destPath = destTree ? destTree.getPath() : "null";
			if (isSharedFolder(destPath) || isPersonalFolder(destPath)) {
				var req = new AJAX();
				var qs = "sourcePath="+sourcePath+"&destPath="+destPath+"&files="+files+"&"+req.timeStamp();
				req.GET("/files/copyFiles", qs, null);
				if (req.success()) {
					//currentNode = destTree;
					//showCurrentFileDirContents();
					alert("The files were copied successfully to\n"+destPath);
				}
				else alert("The attempt to copy the files failed.");
			}
		}
	}
}

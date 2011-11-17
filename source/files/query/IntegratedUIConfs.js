var confItems = new Array(
		new Item("New conference", newConference, "newconference"),
		new Item("Rename conference", renameConference, "renameconference"),
		new Item("Delete conference", deleteConference, "deleteconference"),
		new Item("", null),
		new Item("New agenda item", newAgendaItem, "newagendaitem"),
		new Item("Delete agenda items", deleteAgendaItems, "deleteagendaitems"),
		new Item("", null),
		new Item("Show normal titles", normalTitles, "normaltitles"),
		new Item("Show unknown titles", unknownTitles, "unknowntitles") );

var confMenu = new Menu("Conferences", confItems);

var currentConfTreeNode = null;
var lastConfClicked = -1;
var showNormalTitles = (getCookie("showNormalTitles") != "NO");

//Set the enables on the Conferences menu
function setConfEnables() {

	var nSelected = currentConfTreeNode ? getSelectedItemsCount() : 0;
	var root = isRootConf(currentConfTreeNode);

	confMenuBar.setEnable("newconference", user.isLoggedIn && currentConfTreeNode);
	confMenuBar.setEnable("deleteconference", user.isLoggedIn && currentConfTreeNode && !root);
	confMenuBar.setEnable("renameconference", user.isLoggedIn && currentConfTreeNode && !root);
	confMenuBar.setEnable("newagendaitem", user.isLoggedIn && currentConfTreeNode);
	confMenuBar.setEnable("deleteagendaitems", user.isLoggedIn && (nSelected > 0));
	confMenuBar.setEnable("normaltitles", !showNormalTitles);
	confMenuBar.setEnable("unknowntitles", showNormalTitles);
}

function isRootConf(node) {
	if (node) {
		var path = node.getPath();
		return (path == "Shared") || (path == "Personal");
	}
	return false;
}
function isSharedConf(path) {
	return (path.indexOf("Shared") == 0);
}
function isPersonalConf(path) {
	return (path.indexOf("Personal") == 0);
}
function getSelectedItemsCount() {
	if (scrollableTable) {
		var count = 0;
		var rows = scrollableTable.tbody.rows;
		for (var i=0; i<rows.length; i++) {
			var cb = rows[i].firstChild.firstChild;
			if (cb.checked) count++;
		}
		return count;
	}
	else return 0;
}

//Handlers for tree selection
//

//Handlers for the Menu
//
function normalTitles() {
	showNormalTitles = true;
	setCookie("showNormalTitles", "YES");
	setConfEnables();
	showCurrentConferenceContents();
}

function unknownTitles() {
	showNormalTitles = false;
	setCookie("showNormalTitles", "NO");
	setConfEnables();
	showCurrentConferenceContents();
}

function newConference() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);

	var div = document.createElement("DIV");
	div.className = "content";
	var p = document.createElement("P");
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("Conference name:\u00A0"));
	var text = document.createElement("INPUT");
	text.id = "etext1";
	text.className = "textbox";
	p.appendChild(text);
	div.appendChild(p);

	showDialog("ediv", 400, 200, "New Conference", closeboxURL, "Create New Conference", div, doNewConference, hidePopups);
	window.setTimeout("document.getElementById('etext1').focus()", 500);
}
function doNewConference(event) {
	var name = document.getElementById("etext1").value;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	name = trim(name);
	if (name.length == 0) return;
	name = name.replace(/\s+/g,"_");
	var req = new AJAX();
	var qs = "id="+currentConfTreeNode.nodeID+"&name="+encodeURIComponent(name)+"&"+req.timeStamp();
	req.GET("/confs/createConference", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		var parent = currentConfTreeNode.parent;
		var treeManager = currentConfTreeNode.treeManager;
		var level = currentConfTreeNode.level;
		var tree = new Tree(treeManager, parent, level);
		tree.load(child);
		var state = treeManager.getState();
		currentConfTreeNode = currentConfTreeNode.replaceChildren(tree);
		treeManager.display(state);
		currentConfTreeNode.expand();
		currentConfTreeNode.showPath();
	}
	else {
		alert("The attempt to create the conference failed.");
		window.open("/query", "_self");
	}
}

function renameConference() {
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

	showDialog("ediv", 400, 200, "Rename Conference", closeboxURL, "Rename Conference", div, doRenameConference, hidePopups);
	window.setTimeout("document.getElementById('etext1').focus()", 500);
}
function doRenameConference(event) {
	var name = document.getElementById("etext1").value;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	name = trim(name);
	if (name.length == 0) return;
	name = name.replace(/\s+/g,"_");
	var req = new AJAX();
	var qs = "id="+currentConfTreeNode.nodeID+"&name="+encodeURIComponent(name)+"&"+req.timeStamp();
	req.GET("/confs/renameConference", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		if ((root != null) && (root.tagName == "ok")) {
			currentConfTreeNode.name = name;
			var treeManager = currentConfTreeNode.treeManager;
			var state = treeManager.getState();
			treeManager.display(state);
			currentConfTreeNode.showPath();
			return;
		}
	}
	alert("The attempt to rename the conference failed.");
	window.open("/query", "_self");
}

function newAgendaItem() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);

	var div = document.createElement("DIV");
	div.className = "content";
	var p = document.createElement("P");
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("URL:\u00A0"));
	var text = document.createElement("INPUT");
	text.id = "etext1";
	text.className = "textbox";
	p.appendChild(text);
	p.appendChild(document.createElement("BR"));
	p.className = "centeredblock";
	p.appendChild(document.createTextNode("Title:\u00A0"));
	text = document.createElement("INPUT");
	text.id = "etext2";
	text.className = "textbox";
	p.appendChild(text);

	div.appendChild(p);

	showDialog("ediv", 400, 240, "New Agenda Item", closeboxURL, "New Agenda Item", div, doNewAgendaItem, hidePopups);
	window.setTimeout("document.getElementById('etext1').focus()", 500);
}
function doNewAgendaItem(event) {
	var url = document.getElementById("etext1").value;
	var title = document.getElementById("etext2").value;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	url = trim(url);
	title = trim(title);
	if (url.length == 0) return;
	url = url.replace(/\s+/g,"_");
	var req = new AJAX();
	var qs = "nodeID="+currentConfTreeNode.nodeID+"&url="+encodeURIComponent(url)+"&title="+encodeURIComponent(title)+"&"+req.timeStamp();
	req.GET("/confs/appendAgendaItem", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		if ((root != null) && (root.tagName == "ok")) {
			showCurrentConferenceContents();
			return;
		}
	}
	alert("The attempt to create the agenda item failed.");
}

function deleteConference() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	showTextDialog("ediv", 400, 210, "Are you sure?", closeboxURL, "Delete?",
		"Are you sure you want to delete the selected conference and all its subconferences?",
		deleteConferenceHandler, hidePopups);
}
function deleteConferenceHandler() {
	hidePopups();
	var req = new AJAX();
	var qs = "id="+currentConfTreeNode.nodeID+"&"+req.timeStamp();
	req.GET("/confs/deleteConference", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		var parent = currentConfTreeNode.parent;
		var treeManager = parent.treeManager;
		var tree = new Tree(treeManager, parent.parent, parent.level);
		tree.load(child);
		var state = treeManager.getState();
		currentConfTreeNode = currentConfTreeNode.parent.replaceChildren(tree);
		treeManager.display(state);
		currentConfTreeNode.expand();
		showCurrentConferenceContents();
		currentConfTreeNode.showPath();
	}
	else {
		alert("The attempt to delete the conference failed. req.status="+req.status);
		window.open("/query", "_self");
	}
}

function deleteAgendaItems() {
	alert("deleteAgendaItems");
}
function selectAllAI() {
	alert("selectAllAI");
}
function deselectAllAI() {
	alert("deselectAllAI");
}

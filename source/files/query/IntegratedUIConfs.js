var confItems = new Array(
		new Item("New conference", newConference, "newconference"),
		new Item("Rename conference", renameConference, "renameconference"),
		new Item("Delete conference", deleteConference, "deleteconference"),
		new Item("", null),
		new Item("New agenda item", newAgendaItem, "newagendaitem"),
		new Item("Delete agenda items", deleteAgendaItems, "deleteagendaitems"),
		new Item("", null),
		new Item("Show normal titles", normalTitles, "normaltitles"),
		new Item("Show unknown titles", unknownTitles, "unknowntitles"),
		new Item("", null),
		new Item("Conference Center", conferenceCenter, "conferenceCenter") );

var confMenu = new Menu("Conferences", confItems);

var currentConfTreeNode = null;
var lastConfClicked = -1;
var showNormalTitles = (getCookie("showNormalTitles") != "NO");

//Set the enables on the Conferences menu
function setConfEnables() {

	var nSelected = currentConfTreeNode ? getSelectedAICount() : 0;
	var root = isRootConf(currentConfTreeNode);

	confMenuBar.setEnable("newconference", user.isLoggedIn && currentConfTreeNode);
	confMenuBar.setEnable("deleteconference", user.isLoggedIn && currentConfTreeNode && !root);
	confMenuBar.setEnable("renameconference", user.isLoggedIn && currentConfTreeNode && !root);
	confMenuBar.setEnable("newagendaitem", user.isLoggedIn && currentConfTreeNode);
	confMenuBar.setEnable("deleteagendaitems", user.isLoggedIn && (nSelected > 0));
	confMenuBar.setEnable("normaltitles", !showNormalTitles);
	confMenuBar.setEnable("unknowntitles", showNormalTitles);
	confMenuBar.setEnable("conferenceCenter", user.isLoggedIn);

	var ctrls = document.getElementById("ConfControls");
	if (!ctrls) {
		ctrls = document.createElement("DIV");
		ctrls.id = "ConfControls";
		var parent = document.getElementById("right");
		parent.insertBefore(ctrls, parent.firstChild);
	}
	else while (ctrls.firstChild) ctrls.removeChild(ctrls.firstChild);

	insertConfDragControl(ctrls, startAgendaItemDrag, "/icons/bullet.gif", "Drag the selected agenda items", user.isLoggedIn && (nSelected > 0));
	insertConfButtonControl(ctrls, newConference, "New Conference", "Create a new conference in the current conference", user.isLoggedIn && currentConfTreeNode);
	insertConfButtonControl(ctrls, renameConference, "Rename Conference", "Rename the current conference", user.isLoggedIn && currentConfTreeNode && !root);
	insertConfButtonControl(ctrls, deleteConference, "Delete Conference", "Delete the current conference", user.isLoggedIn && currentConfTreeNode && !root);
	insertConfButtonControl(ctrls, newAgendaItem, "New Agenda Item", "Create a new agenda item in the current conference", user.isLoggedIn && currentConfTreeNode);
	insertConfButtonControl(ctrls, deleteAgendaItems, "Delete Agenda Items", "Delete the selected agenda items", user.isLoggedIn && (nSelected > 0));
	insertConfButtonControl(ctrls, normalTitles, "Normal Titles", "Display titles as knowns", !showNormalTitles);
	insertConfButtonControl(ctrls, unknownTitles, "Unknown Titles", "Display titles as unknowns", showNormalTitles);
	insertConfImgControl(ctrls, displayCN, "/mirc/images/film-projector.gif", "Display the selected agenda items in the Case Navigator", (nSelected > 0));
}

function insertConfDragControl(ctrls, func, src, title, enb) {
	if (enb) {
		var ctrl = document.createElement("IMG");
		ctrl.src = src;
		ctrl.title = title;
		ctrl.onmouseDown = func;
		ctrls.appendChild(ctrl);
	}
}
function insertConfImgControl(ctrls, func, src, title, enb) {
	if (enb) {
		var ctrl = document.createElement("IMG");
		ctrl.src = src;
		ctrl.title = title;
		ctrl.onclick = func;
		ctrls.appendChild(ctrl);
	}
}
function insertConfButtonControl(ctrls, func, src, title, enb) {
	if (enb) {
		var ctrl = document.createElement("INPUT");
		ctrl.type = "button";
		ctrl.className = "FileControl";
		ctrl.value = src;
		ctrl.title = title;
		ctrl.onclick = func;
		ctrls.appendChild(ctrl);
	}
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
function getSelectedAICount() {
	var count = 0;
	if (scrollableTable) {
		var rows = scrollableTable.tbody.rows;
		for (var i=0; i<rows.length; i++) {
			var cb = rows[i].firstChild.firstChild;
			if (cb.checked) count++;
		}
	}
	return count;
}

function getSelectedAIs() {
	var list = "";
	if (scrollableTable) {
		var rows = scrollableTable.tbody.rows;
		for (var i=0; i<rows.length; i++) {
			var row = rows[i];
			var cb = row.firstChild.firstChild;
			if (cb.checked) {
				if (list != "") list += "|";
				var a = row.firstChild.nextSibling.firstChild;
				list += a.getAttribute("href");;
			}
		}
	}
	return list;
}

//Handlers for tree selection
//
function showConferenceContents(event) {
	var source = getSource(getEvent(event));
	currentConfTreeNode = source.treenode;
	showCurrentConferenceContents();
}

function showCurrentConferenceContents() {
	if (!currentConfTreeNode) return;
	deselectAll();
	confTreeManager.closePaths();
	currentConfTreeNode.showPath();
	queryIsActive = false;
	var req = new AJAX();
	req.GET("/confs/getAgenda", "nodeID="+currentConfTreeNode.nodeID+"&"+req.timeStamp(), null);
	if (req.success()) {
		var pane = document.getElementById("right");
		while (pane.firstChild) pane.removeChild(pane.firstChild);

		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;

		var items = root.getElementsByTagName("item");
		if (items.length > 0) {
			scrollableTable = setScrollableTable(pane, conferenceTableHeadings);
			for (var i=0; i<items.length; i++) {
				var item = items[i];
				appendAgendaItem(scrollableTable.tbody, item);
			}
			selectAll();
			resizeScrollableTable();
			scrollableTable.bodyTable.parentNode.onresize = resizeScrollableTable;
		}
		else {
			right.appendChild(document.createTextNode("The conference is empty."));
			scrollableTable = null;
		}
	}
	resize();
	setConfEnables();
}

function conferenceTableHeadings(tr) {
	appendTHCB(tr, toggleConfSelect);
	appendTH(tr, "Title");
	appendTH(tr, "Author");
}

function appendAgendaItem(tbody, item) {
	var tr = document.createElement("TR");

	appendTDCB(tr, setConfEnables);

	var td = document.createElement("TD");
	var a = document.createElement("A");
	a.href = item.getAttribute("url");
	a.target = "shared";
	a.className = "TitleLink";
	if (showNormalTitles) {
		a.appendChild( document.createTextNode(item.getAttribute("title") ) );
	}
	else {
		a.appendChild( document.createTextNode(item.getAttribute("alttitle") ) );
	}

	td.appendChild(a);
	tr.appendChild(td);

	var td = document.createElement("TD");
	td.appendChild( document.createTextNode(item.getAttribute("subtitle") ) );
	tr.appendChild(td);

	tbody.appendChild(tr);
}

//Handlers for the Menu
//
function conferenceCenter() {
	window.open("/confs", "_self");
}

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
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	if (getSelectedAICount() > 0) {
		showTextDialog("ediv", 400, 210, "Are you sure?", closeboxURL, "Delete?",
			"Are you sure you want to delete the selected agenda items?",
			deleteAgendaItemsHandler, hidePopups);
	}
}

function deleteAgendaItemsHandler() {
	hidePopups();
	var selected = getSelectedAIs();
	if (selected != "") {
		selected = encodeURIComponent(selected);
		var req = new AJAX();
		req.GET("/confs/deleteAgendaItems", "nodeID="+currentConfTreeNode.nodeID+"&list="+selected+"&"+req.timeStamp(), null);
		if (req.success()) {
			var xml = req.responseXML();
			var root = xml ? xml.firstChild : null;
			if ((root != null) && (root.tagName == "ok")) {
				showCurrentConferenceContents();
			}
		}
		else {
			alert("The attempt to delete the agenda items failed.");
			window.open("/query", "_self");
		}
	}
}
function toggleConfSelect() {
	var cb = document.getElementById("tableHeaderCB");
	if (cb) {
		if (cb.checked) selectAllAgendaItems();
		else deselectAllAgendaItems();
	}
	setConfEnables();
}

function selectAllAgendaItems() {
	if (scrollableTable) {
		var tbody = scrollableTable.tbody;
		var cbs = tbody.getElementsByTagName("INPUT");
		for (var i=0; i<cbs.length; i++) {
			var cb = cbs[i];
			if (cb.type == "checkbox") cb.checked = true;
		}
	}
}

function deselectAllAgendaItems() {
	if (scrollableTable) {
		var tbody = scrollableTable.tbody;
		var cbs = tbody.getElementsByTagName("INPUT");
		for (var i=0; i<cbs.length; i++) {
			var cb = cbs[i];
			if (cb.type == "checkbox") cb.checked = false;
		}
	}
}

//Drag Agenda Items
function startAgendaItemDrag() {
	alert("startAgendaItemDrag");
}
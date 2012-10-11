var confItems = new Array(
		new Item("New conference", newConference, "newconference"),
		new Item("Rename conference", renameConference, "renameconference"),
		new Item("Delete conference", deleteConference, "deleteconference"),
		new Item("", null),
		new Item("New case", newAgendaItem, "newagendaitem"),
		new Item("Delete cases", deleteAgendaItems, "deleteagendaitems") );

var viewItems = new Array(
		new Item("Show normal titles", normalTitles, "normaltitles"),
		new Item("Show unknown titles", unknownTitles, "unknowntitles"),
		new Item("", null),
		new Item("View in Case Navigator", caseNavigator, "casenavigator") );

var selectItems = new Array(
		new Item("Select all cases", selectAllAI, "selectallai"),
		new Item("Deselect all cases", deselectAllAI, "deselectallai") );

var helpItems = new Array (
		new Item("MIRC Wiki", showWiki) );

var confMenu = new Menu("Conferences", confItems);
var viewMenu = new Menu("View", viewItems);
var selectMenu = new Menu("Select", selectItems);
var helpMenu = new Menu("Help", helpItems);

var menuBar = new MenuBar("menuBar", new Array (confMenu, viewMenu, selectMenu, helpMenu));
var treeManager;

window.onload = load;
window.onresize = doResize;

var currentNode = null;
var currentPath = "";
var lastClicked = -1;
var nAgendaItems = 0;
var showNormalTitles = (getCookie("showNormalTitles") != "NO");
var homeURL = "/icons/home.png";
var closeboxURL = "/icons/closebox.gif";
var closeboxTitle = "Return to the home page";
var split;

function doResize() {
	resize();
	split.positionSlider();
}

function load() {
	var pad = "\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0";
	if (suppressHome != "yes") {
		setPageHeader(pad+"Conference Center", username, homeURL, closeHandler);
	}
	else {
		setPageHeader(pad+"Conference Center", username);
	}
	menuBar.display();
	split = new HorizontalSplit("left", "center", "right", true);
	doResize();
	treeManager =
		new TreeManager(
			"left",
			"/confs/tree",
			"/icons/plus.gif",
			"/icons/minus.gif");
	treeManager.load();
	treeManager.display();
	if (openpath != "") currentNode = treeManager.expandPath(openpath);
	else treeManager.expandAll();
	if (currentNode != null) {
		treeManager.closePaths();
		currentNode.showPath();
		showCurrentFileDirContents();
	};
	setEnables();
}

function isConference() {
	return (currentPath != "");
}
function isRootConference() {
	return (currentPath == "Shared") || (currentPath == "Personal");
}
function isSharedConference(path) {
	return (path.indexOf("Shared") == 0);
}
function isPersonalConference(path) {
	return (path.indexOf("Personal") == 0);
}

function setEnables() {
	var nSelected = getSelectedAICount();
	var pathIsConference = isConference();
	menuBar.setEnable("newconference", pathIsConference);
	menuBar.setEnable("deleteconference", pathIsConference && !isRootConference());
	menuBar.setEnable("renameconference", pathIsConference && !isRootConference());
	menuBar.setEnable("newagendaitem", pathIsConference);
	menuBar.setEnable("deleteagendaitems", (nSelected > 0));

	menuBar.setEnable("normaltitles", !showNormalTitles);
	menuBar.setEnable("unknowntitles", showNormalTitles);
	menuBar.setEnable("casenavigator", (nAgendaItems > 0));

	menuBar.setEnable("selectallai", (nAgendaItems > 0));
	menuBar.setEnable("deselectallai", (nAgendaItems > 0));
}

//Handlers for tree selection
//
function showConferenceContents(event) {
	var source = getSource(getEvent(event));
	currentNode = source.treenode;
	showCurrentConferenceContents();
}

function showCurrentConferenceContents() {
	currentPath = currentNode.getPath();
	treeManager.closePaths();
	currentNode.showPath();
	menuBar.setText(currentPath);
	var req = new AJAX();
	req.GET("/confs/getAgenda", "nodeID="+currentNode.nodeID+"&"+req.timeStamp(), null);
	if (req.success()) {
		var right = document.getElementById("right");
		while (right.firstChild) right.removeChild(right.firstChild);
		removeCNButton();
		nAgendaItems = 0;
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		while (child) {
			if ((child.nodeType == 1) && (child.tagName == "item")) {
				setCNButton();
				right.appendChild(
						makeItemTable(
								child.getAttribute("url"),
								child.getAttribute("title"),
								child.getAttribute("alturl"),
								child.getAttribute("alttitle"),
								child.getAttribute("subtitle") ) );
				nAgendaItems++;
			}
			child = child.nextSibling;
		}
		lastClicked = -1;
		nSelected = 0;
		setEnables();
	}
	else window.open("/query", "_self");
}

function setCNButton() {
	var cnb = document.getElementById("cnbdiv");
	if (cnb == null) {
		var parent = document.body;
		var cnbdiv = document.createElement("DIV");
		cnbdiv.className = "cnbdiv";
		cnbdiv.id = "cnbdiv";
		var img = document.createElement("IMG");
		img.src = "/icons/start.png";
		img.title = "Play Conference in Case Navigator";
		img.onclick = caseNavigator;
		cnbdiv.appendChild(img);
		parent.insertBefore(cnbdiv, parent.firstChild);
	}
}

function removeCNButton() {
	var cnbdiv = document.getElementById("cnbdiv");
	if (cnbdiv) cnbdiv.parentNode.removeChild(cnbdiv);
}

function makeItemTable(url, title, alturl, alttitle, subtitle) {
	var div = document.createElement("DIV");
	div.className = "agenda";
	div.url = url;
	var table = document.createElement("TABLE");
	table.className = "AIdesel"
	var tbody = document.createElement("TBODY");
	var tr = document.createElement("TR");
	var td = document.createElement("TD");
	td.className = "AIbullet";
	var bullet = document.createElement("IMG");
	//bullet.onclick = agendaItemClicked;
	bullet.onmousedown = startAgendaItemDrag;

	bullet.src = "/icons/bullet.gif";
	bullet.url = url;
	td.appendChild(bullet);
	tr.appendChild(td);
	td = document.createElement("TD");
	td.className = "agendaitem";
	var anchor = document.createElement("A");
	anchor.target = "agendaitem";
	if (showNormalTitles) {
		anchor.href = url;
		anchor.appendChild(document.createTextNode(title));
	}
	else {
		anchor.href = alturl;
		anchor.appendChild(document.createTextNode(alttitle));
	}
	td.appendChild(anchor);
	if (subtitle != "") {
		td.appendChild(document.createElement("BR"));
		td.appendChild(document.createTextNode("\u00A0\u00A0\u00A0\u00A0" + subtitle));
	}
	tr.appendChild(td);
	tbody.appendChild(tr);
	table.appendChild(tbody);
	div.appendChild(table);
	return div;
}

//Handlers for Agenda Item selection in the right pane
//
function agendaItemClicked(theEvent) {
	theEvent = getEvent(theEvent)
	stopEvent(theEvent);
	var source = getSource(theEvent);
	var ai = source.parentNode;
	while ((ai != null) && (ai.tagName != "TABLE")) ai = ai.parentNode;

	var currentAIClicked = getAIClicked(ai);
	if (currentAIClicked == -1) return;

	if (!theEvent.shiftKey && !theEvent.ctrlKey) {
		deselectAllAI();
		ai.className = "AIsel";
		lastClicked = currentAIClicked;
	}

	else if (getEvent(theEvent).ctrlKey) {
		if (ai.className == "AIsel") ai.className = "AIdesel";
		else ai.className ="AIsel";
		lastClicked = currentAIClicked;
	}

	else {
		if (lastClicked == -1) {
			lastClicked = currentAIClicked;
			ai.className = "AIsel";
		}
		else {
			var ais = getAIs();
			var start = Math.min(lastClicked,currentAIClicked);
			var stop = Math.max(lastClicked,currentAIClicked);
			for (var i=start; i<=stop; i++) ais[i].className = "AIsel";
		}
	}
	setEnables();
}

function getAIs() {
	var right = document.getElementById("right");
	return right.getElementsByTagName("TABLE");
}

function getAIClicked(ai) {
	var ais = getAIs();
	for (var i=0; i<ais.length; i++) {
		if (ai === ais[i]) {
			return i;
		}
	}
	return -1;
}

function selectAllAI() {
	var ais = getAIs();
	for (var i=0; i<ais.length; i++) ais[i].className = "AIsel";
	setEnables();
}

function deselectAllAI() {
	var ais = getAIs();
	for (var i=0; i<ais.length; i++) ais[i].className = "AIdesel";
	setEnables();
}

function getSelectedAICount() {
	var ais = getAIs();
	var n = 0;
	for (var i=0; i<ais.length; i++) {
		if (ais[i].className == "AIsel") n++;
	}
	return n;
}

function getSelectedAIs() {
	var ais = getAIs();
	var list = "";
	for (var i=0; i<ais.length; i++) {
		var bullet = ais[i].getElementsByTagName("IMG");
		var url = bullet[0].url;
		if (ais[i].className == "AIsel") {
			if (list != "") list += "|";
			list += url;
		}
	}
	return list;
}

function deleteAgendaItems() {
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	if (getSelectedAICount() > 0) {
		showTextDialog("ediv", 400, 210, "Are you sure?", closeboxURL, "Delete?",
			"Are you sure you want to delete the selected cases?",
			deleteAgendaItemsHandler, hidePopups);
	}
}

function deleteAgendaItemsHandler() {
	hidePopups();
	var selected = getSelectedAIs();
	if (selected != "") {
		selected = encodeURIComponent(selected);
		var req = new AJAX();
		req.GET("/confs/deleteAgendaItems", "nodeID="+currentNode.nodeID+"&list="+selected+"&"+req.timeStamp(), null);
		if (req.success()) {
			var xml = req.responseXML();
			var root = xml ? xml.firstChild : null;
			if ((root != null) && (root.tagName == "ok")) {
				showCurrentConferenceContents();
			}
		}
		else {
			alert("The attempt to delete the cases failed.");
			window.open("/query", "_self");
		}
	}
}

function trim(text) {
	if (text == null) return "";
	text = text.replace( /^\s+/g, "" );// strip leading
	return text.replace( /\s+$/g, "" );// strip trailing
}

//Handlers for the View Menu
function normalTitles() {
	showNormalTitles = true;
	setCookie("showNormalTitles", "YES");
	setEnables();
	showCurrentConferenceContents();
}
function unknownTitles() {
	showNormalTitles = false;
	setCookie("showNormalTitles", "NO");
	setEnables();
	showCurrentConferenceContents();
}
function caseNavigator() {
	var req = new AJAX();
	var qs = "nodeID="+currentNode.nodeID+"&"+req.timeStamp();
	window.open("/confs/caseNavigator?"+qs, "_self");
}

//Handlers for the Conferences Menu
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
	var qs = "id="+currentNode.nodeID+"&name="+encodeURIComponent(name)+"&"+req.timeStamp();
	req.GET("/confs/createConference", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		var tree = new Tree(currentNode.treeManager, currentNode.parent, currentNode.level);
		tree.load(child);
		var state = treeManager.getState();
		currentNode = currentNode.replaceChildren(tree);
		treeManager.display(state);
		currentNode.expand();
		currentNode.showPath();
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
	var qs = "id="+currentNode.nodeID+"&name="+encodeURIComponent(name)+"&"+req.timeStamp();
	req.GET("/confs/renameConference", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		if ((root != null) && (root.tagName == "ok")) {
			currentNode.name = name;
			currentPath = currentNode.getPath();
			var state = treeManager.getState();
			treeManager.display(state);
			currentNode.showPath();
			menuBar.setText(currentPath);
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

	showDialog("ediv", 400, 240, "New Case", closeboxURL, "New Case", div, doNewAgendaItem, hidePopups);
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
	var qs = "nodeID="+currentNode.nodeID+"&url="+encodeURIComponent(url)+"&title="+encodeURIComponent(title)+"&"+req.timeStamp();
	req.GET("/confs/appendAgendaItem", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		if ((root != null) && (root.tagName == "ok")) {
			showCurrentConferenceContents();
			return;
		}
	}
	alert("The attempt to create the case failed.");
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
	var qs = "id="+currentNode.nodeID+"&"+req.timeStamp();
	req.GET("/confs/deleteConference", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		var parent = currentNode.parent;
		var tree = new Tree(parent.treeManager, parent.parent, parent.level);
		tree.load(child);
		var state = treeManager.getState();
		currentNode = currentNode.parent.replaceChildren(tree);
		treeManager.display(state);
		currentNode.expand();
		showCurrentConferenceContents();
		currentNode.showPath();
	}
	else {
		alert("The attempt to delete the conference failed. req.status="+req.status);
		window.open("/query", "_self");
	}
}

//Handler for closing the page
//
function closeHandler() {
	hidePopups();
	window.open("/query","_self");
}

//Handlers for the Help menu
//
function showWiki(event, item) {
	window.open("http://mircwiki.rsna.org/index.php?title=Main_Page","help");
}

//********************************************
//**** Drag/drop handler for agenda items ****
//********************************************

//This handler starts the drag of selected agenda items.
function startAgendaItemDrag(event) {
	event = getEvent(event);
	var right = document.getElementById("right");
	var scrollTop = right.scrollTop;
	var node = getSource(event);
	var url = node.url;

	agendaItemClicked(event);

	while (node.tagName != "TABLE") node = node.parentNode;
	if ((getSelectedAICount() == 1) && (node.className == "AIsel")) {
		var dragableAIDiv = getDragableAIDiv(node.parentNode, event.clientX, event.clientY, scrollTop);
		dragableAIDiv.url = url;
		dragAgendaItem(dragableAIDiv, event);
	}
}

//Make a div containing an agenda item to drag.
function getDragableAIDiv(node, clientX, clientY, scrollTop) {
	var pos = findObject(node);
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
	div.style.filter = "alpha(opacity=75, style=0)";
	div.parentScrollTop = scrollTop;
	var table = node.firstChild.cloneNode(true);
	table.className = "AIsel";
	div.appendChild(table);
	document.body.appendChild(div);
	return div;
}

//Drag and drop agenda items
function dragAgendaItem(dragableAIDiv, event) {
	var node = dragableAIDiv;
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
		node.style.left = (evt.clientX - node.deltaX) + "px";
		node.style.top = (evt.clientY - node.deltaY - node.parentScrollTop) + "px";
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
		document.body.removeChild(node);

		var left = document.getElementById("left");
		var scrollTop = left.scrollTop;
		var pos = findObject(left);
		if (evt.clientX > pos.w) {
			//This is a rearrangement within the conference
			var right = document.getElementById("right");
			var ais = right.getElementsByTagName("DIV");
			var skipNext = false;
			for (var i=0; i<ais.length; i++) {
				if (ais[i].url != node.url) {
					if (!skipNext) {
						var aiPos = findObject(ais[i]);
						if ((evt.clientY >= aiPos.y) && (evt.clientY < aiPos.y + aiPos.h)) {
							var req = new AJAX();
							var qs = "nodeID="+currentNode.nodeID
										+"&sourceURL="+encodeURIComponent(node.url)
											+"&targetURL="+encodeURIComponent(ais[i].url)
												+"&"+req.timeStamp();
							req.GET("/confs/moveAgendaItem", qs, null);
							if (req.success()) showCurrentConferenceContents()
							else alert("The attempt to move the agenda item failed.");
							return false;
						}
					}
					skipNext = false;
				}
				else skipNext = true;
			}
		}
		else {
			//This is a move to another conference (maybe)
			var sourcePath = currentPath;
			var destTree = treeManager.getTreeForCoords(evt.clientX, evt.clientY + scrollTop);
			if (destTree) {
				var destPath = destTree ? destTree.getPath() : "null";
				if (isSharedConference(destPath) || isPersonalConference(destPath)) {
					var req = new AJAX();

					var qs = "sourceID="+currentNode.nodeID
								+"&targetID="+destTree.nodeID
									+"&list="+encodeURIComponent(node.url)
										+"&"+req.timeStamp();

					req.GET("/confs/transferAgendaItem", qs, null);
					if (req.success()) showCurrentConferenceContents();
					else {
						alert("The attempt to transfer the agenda item failed.");
						window.open("/query", "_self");
					}
				}
			}
		}
	}
}

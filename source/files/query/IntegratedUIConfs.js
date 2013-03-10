var confItems = new Array(
		new Item("New conference", newConference, "newconference"),
		new Item("Rename conference", renameConference, "renameconference"),
		new Item("Delete conference", deleteConference, "deleteconference"),
		new Item("", null),
		new Item("Add a case", newAgendaItem, "newagendaitem"),
		new Item("Remove cases", deleteAgendaItems, "deleteagendaitems"),
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

	if (currentConfTreeNode) {
		var ctrls = document.getElementById("ConfControls");
		if (!ctrls) {
			ctrls = document.createElement("DIV");
			ctrls.id = "ConfControls";
			var parent = document.getElementById("right");
			parent.insertBefore(ctrls, parent.firstChild);
		}
		else while (ctrls.firstChild) ctrls.removeChild(ctrls.firstChild);

		insertConfDragControl(ctrls, startAgendaItemDrag, "/icons/bullet.gif", "Drag the selected casess", user.isLoggedIn && (nSelected > 0));
		insertConfImgControl(ctrls, newConference, "/mirc/images/newfolder.png", "Create a new conference in the current conference", user.isLoggedIn && currentConfTreeNode);
		insertConfImgControl(ctrls, renameConference, "/mirc/images/renamefolder.png", "Rename the current conference", user.isLoggedIn && currentConfTreeNode && !root);
		insertConfImgControl(ctrls, deleteConference, "/mirc/images/deletefolder.png", "Delete the current conference", user.isLoggedIn && currentConfTreeNode && !root);
		insertConfImgControl(ctrls, newAgendaItem, "/mirc/images/newitem.png", "Create a link to a new case in the current conference", user.isLoggedIn && currentConfTreeNode);
		insertConfImgControl(ctrls, deleteAgendaItems, "/mirc/images/deleteitem.png", "Remove the selected cases from the conference", user.isLoggedIn && (nSelected > 0));
		insertConfImgControl(ctrls, normalTitles, "/mirc/images/toggletitles.png", "Display titles as knowns", !showNormalTitles);
		insertConfImgControl(ctrls, unknownTitles, "/mirc/images/toggletitles.png", "Display titles as unknowns", showNormalTitles);
		insertConfImgControl(ctrls, displayCN, "/mirc/images/film-projector.gif", "Display the selected cases in the Case Navigator", (nSelected > 0));
		insertConfImgControl(ctrls, getQuizSummary, "/mirc/images/quizsummary.png", "Quiz summary for the selected local cases", user.isLoggedIn && user.hasRole("admin"));
		/*
		insertConfButtonControl(ctrls, newConference, "New Conference", "Create a new conference in the current conference", user.isLoggedIn && currentConfTreeNode);
		insertConfButtonControl(ctrls, renameConference, "Rename Conference", "Rename the current conference", user.isLoggedIn && currentConfTreeNode && !root);
		insertConfButtonControl(ctrls, deleteConference, "Delete Conference", "Delete the current conference", user.isLoggedIn && currentConfTreeNode && !root);
		insertConfButtonControl(ctrls, newAgendaItem, "New Agenda Item", "Create a new agenda item in the current conference", user.isLoggedIn && currentConfTreeNode);
		insertConfButtonControl(ctrls, deleteAgendaItems, "Delete Agenda Items", "Delete the selected agenda items", user.isLoggedIn && (nSelected > 0));
		insertConfButtonControl(ctrls, normalTitles, "Normal Titles", "Display titles as knowns", !showNormalTitles);
		insertConfButtonControl(ctrls, unknownTitles, "Unknown Titles", "Display titles as unknowns", showNormalTitles);
		*/
	}
}

function insertConfDragControl(ctrls, func, src, title, enb) {
	if (enb) {
		var ctrl = document.createElement("IMG");
		ctrl.src = src;
		ctrl.title = title;
		ctrl.onmousedown = func;
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
				list += a.getAttribute("href");
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
	a.target = "shared";
	a.className = "TitleLink";
	if (showNormalTitles) {
		a.href = item.getAttribute("url");
		a.appendChild( document.createTextNode(item.getAttribute("title") ) );
	}
	else {
		a.href = item.getAttribute("url")+"?unknown=yes";
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

	showDialog("ediv", 400, 240, "New Case Link", closeboxURL, "New Case Link", div, doNewAgendaItem, hidePopups);
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
	var name = currentConfTreeNode.name;
	showTextDialog("ediv", 400, 225, "Are you sure?", closeboxURL, "Delete Conference?",
		"Are you sure you want to delete the \""+name+"\" conference and all its subconferences?",
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
		showTextDialog("ediv", 400, 210, "Are you sure?", closeboxURL, "Remove Cases?",
			"Are you sure you want to remove the selected cases from the conference?",
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
			alert("The attempt to remove the cases failed.");
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

//********************************************
//**** Drag/drop handler for agenda items ****
//********************************************
//This handler starts the drag of selected agenda items.
//In the Integrated UI, we just drag the bullet IMG.
function startAgendaItemDrag(event) {
	event = getEvent(event);
	var right = document.getElementById("right");
	var scrollTop = right.scrollTop;
	var node = getSource(event);
	var dragableDiv = getDragableDiv(node, event.clientX, event.clientY, scrollTop);
	dragAgendaItem(dragableDiv, event);
}

//Make a div containing something to drag.
function getDragableDiv(node, clientX, clientY, scrollTop) {
	var pos = findObject(node);
	var div = document.createElement("DIV");
	div.deltaX = clientX - parseInt(pos.x);
	div.deltaY = clientY - parseInt(pos.y);
	div.style.position = "absolute";
	//div.style.width = pos.w;
	//div.style.height = pos.h;
	div.style.left = pos.x;
	div.style.top = pos.y - scrollTop;
	div.style.visibility = "visible";
	div.style.display = "block";
	div.style.overflow = "hidden";
	div.style.zIndex = 5;
	div.style.backgroundColor = "transparent";
	div.style.filter = "alpha(opacity=75, style=0)";
	div.parentScrollTop = scrollTop;
	var img = node.cloneNode(true);
	img.removeAttribute("title");
	img.style.height = 24;
	img.style.width = 24;
	div.appendChild(img);
	insertSelectedAITitles(div);
	document.body.appendChild(div);
	return div;
}

function insertSelectedAITitles(div) {
	if (scrollableTable) {
		var rows = scrollableTable.tbody.rows;
		var foundOne = false;
		for (var i=0; i<rows.length; i++) {
			var row = rows[i];
			var cb = row.firstChild.firstChild;
			if (cb.checked) {
				var a = row.firstChild.nextSibling.firstChild;
				var title = a.firstChild.cloneNode(true);
				if (!foundOne) {
					div.appendChild( document.createTextNode("\u00A0\u00A0") );
					foundOne = true;
				}
				else {
					div.appendChild( document.createElement("BR") );
					div.appendChild( document.createTextNode("\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0\u00A0") );
				}
				div.appendChild( title );
			}
		}
	}
}

//Drag and drop agenda items
function dragAgendaItem(dragableDiv, event) {
	var lastTreeNode = null;
	var dropped = false;
	var node = dragableDiv;
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

		if (!dropped) {
			node.style.left = (evt.clientX - node.deltaX) + "px";
			node.style.top = (evt.clientY - node.deltaY - node.parentScrollTop) + "px";

			if (lastTreeNode) {
				var namespan = lastTreeNode.namespan;
				namespan.style.color = "black";
				lastTreeNode = null;
			}
			var left = document.getElementById("left");
			var scrollTop = left.scrollTop;
			lastTreeNode = confTreeManager.getTreeForCoords(evt.clientX, evt.clientY + scrollTop);
			if (lastTreeNode) {
				var namespan = lastTreeNode.namespan;
				namespan.style.color = "white";
			}
		}

		if (evt.stopPropagation) evt.stopPropagation();
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
		if (evt.stopPropagation) evt.stopPropagation();
		else evt.cancelBubble = true;
		dropped = true;
	}

	function handleDrop(evt) {
		document.body.removeChild(node);
		if (lastTreeNode) {
			var namespan = lastTreeNode.namespan;
			namespan.style.color = "black";
			lastTreeNode = null;
		}

		var left = document.getElementById("left");
		var scrollTop = left.scrollTop;
		var pos = findObject(left);
		var list = getSelectedAIs();
		if ((list != "") && (evt.clientX < pos.w)) {
			//This is a move to another conference (maybe)
			var sourcePath = currentConfTreeNode.getPath();;
			var destTree = confTreeManager.getTreeForCoords(evt.clientX, evt.clientY + scrollTop);
			if (destTree) {
				var destPath = destTree ? destTree.getPath() : "null";
				if (isSharedConf(destPath) || isPersonalConf(destPath)) {
					var req = new AJAX();

					var qs = "sourceID="+currentConfTreeNode.nodeID
								+"&targetID="+destTree.nodeID
									+"&list="+encodeURIComponent(list)
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

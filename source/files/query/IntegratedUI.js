//The Integrated UI implementation
//
var user = null;
var encodedUsername = ""
var prefs = null;
var split = null;
var collectionID = null;
var collectionQuery = null;
var confTreeManager = null;
var fileTreeManager = null;
var currentTable = "";
var firstResult = 1;
var maxResults = 25;

var expandURL = "/mirc/images/expand.png";
var collapseURL = "/mirc/images/collapse.png";

function loaded() {
	user = new User();
	if (user.isLoggedIn) {
		encodedUsername = encodeURIComponent(user.name);
		prefs = new Prefs();
		replaceContent("usernameSpan", prefs.name);
		replaceContent("loginoutAnchor", "Logout");
		show("myAccount", "inline");
	}
	else {
		replaceContent("usernameSpan", "Guest");
		hide("myAccount");
		replaceContent("loginoutAnchor", "Login");
	}
	split = new HorizontalSplit("left", "center", "right", true, 185);

	setVisibility("MyDocuments", user.hasRole("author"));
	setVisibility("AuthorTools", user.hasRole("author"));
	setVisibility("FileCabinets", user.isLoggedIn);
	setVisibility("ApprovalQueue", user.hasRole("publisher"));
	setVisibility("Admin", user.hasRole("admin"));
	setVisibility("CaseOfTheDay", (codURL != ""));

	var freetext = document.getElementById("freetext");
	freetext.focus();
	freetext.onclick = freetextClick;
	window.onresize = resize;
	createModifiersPopup();
	createServersPopup();
	setState();
	setModifierValues();
	loadConferences();
	if (user.isLoggedIn) loadFileCabinets();
	queryMine();
	queryMethod = repeatSearch;
	showSessionPopup();
}
window.onload = loaded;

function resize() {
	split.positionSlider();
}

//************************************************
//Utilities
//************************************************
function replaceContent(id, text) {
	var parent = document.getElementById(id);
	replaceNodeContent(parent, text);
}
function replaceNodeContent(parent, text) {
	if (parent) {
		while (parent.firstChild) parent.removeChild(parent.firstChild);
		parent.appendChild( document.createTextNode(text) );
	}
}

function setVisibility(id, showObject) {
	if (showObject) show(id, "block");
	else hide(id);
}
function hide(id) {
	var node = document.getElementById(id);
	hideNode(node);
}
function hideNode(node) {
	if (node) {
		node.style.visibility = "hidden";
		node.style.display = "none";
	}
}
function show(id, type) {
	var node = document.getElementById(id);
	showNode(node, type);
}
function showNode(node, type) {
	if (node) {
		node.style.visibility = "visible";
		if (type == null) type="block"
		node.style.display = type;
	}
}

function toggle(div, components) {
	var mods = document.getElementById(div);
	var img = mods.getElementsByTagName("IMG")[0];
	var visible = (img.src.indexOf("plus") == -1);
	var comps = document.getElementById(components);
	if (visible) {
		hideNode(comps);
		img.src = "/mirc/images/plus.gif";
	}
	else {
		showNode(comps, "block");
		img.src = "/mirc/images/minus.gif";
	}
}

//************************************************
//Login/Logout
//************************************************
function loginLogout() {
	if (user.isLoggedIn) logout('/query');
	else showLoginPopup('/query');
}

//************************************************
//Modifiers
//************************************************
function clearModifiers() {
	firstResult = 1;
	var freetext = document.getElementById("freetext");
	freetext.value = "";
	repeatSearch();
}

function getModifiers() {
	var mods = "";
	mods +=	"&firstresult=" + firstResult;
	mods += "&maxresults=" + pageSize;
	mods += "&orderby=" + sortOrder;
	if (displayFormat != "") mods += "&display=" + displayFormat;
	if (backgroundColor != "") mods += "&bgcolor=" + backgroundColor;
	if (unknown) mods += "&unknown=yes";
	mods +=	getServerIDs();

	var freetext = document.getElementById("freetext");
	text = trim(freetext.value);
	if (text == "Search...") text = "";
	if (text != "") mods += "&document=" + encodeURIComponent(text);
	return mods;
}

function freetextClick() {
	var freetext = document.getElementById("freetext");
	text = freetext.value;
	text = trim(text);
	if (text == "Search...")  freetext.value = "";
}

function keyClicked(event) {
	event = event ? event : window.event;
	if (event.keyCode == 13) repeatSearch();
}

function getServerIDs() {
	var ids = "";
	var sel = document.getElementById("serverselect");
	if (sel) {
		var ids = getOptions(sel);
		if (ids != "") ids = "&server=" + ids;
	}
	return ids;
}

var queryMethod = null;
var pageSize = "25";
var sortOrder = "lmdate";
var displayFormat = "";
var backgroundColor = "";
var unknown = false;

function createModifiersPopup() {
	var id = "modifiersPopupID";
	var title = "Query Modifiers";
	var closebox = "/icons/closebox.gif";
	var div = getModifiersDiv();
	showDialog(id, 375, 230, title, closebox, null, div, null, null, true);
}

function showModifiersPopup() {
	var id = "modifiersPopupID";
	var title = "Query Modifiers";
	var closebox = "/icons/closebox.gif";
	var w = 375;
	var h = 230;
	var pop = document.getElementById(id);
	if (pop) {
		showPopup(id, w, h, title, closebox);
	}
	else {
		var div = getModifiersDiv();
		showDialog(id, w, h, title, closebox, null, div, null, null);
	}
}

function getModifiersDiv() {
	var div = document.createElement("DIV");
	div.className = "content";

	var table = document.createElement("TABLE");
	var tbody = document.createElement("TBODY");
	table.appendChild(tbody);
	div.appendChild(table);

	var sel = setSelect(tbody, "maxresults", "Page Size:", "Choose the number of cases from each library");
	setSelectOption(sel, "10", "10", false);
	setSelectOption(sel, "25", "25", true);
	setSelectOption(sel, "150", "50", false);
	setSelectOption(sel, "100", "100", false);
	setSelectOption(sel, "500", "500", false);

	sel = setSelect(tbody, "orderby", "Sort Order:", "Choose the order of the cases");
	setSelectOption(sel, "lmdate", "Last modified date", true);
	setSelectOption(sel, "pubdate", "Creation date", false);
	setSelectOption(sel, "title", "Title", false);

	sel = setSelect(tbody, "display", "Display Format:", "Choose the format in which cases will be displayed");
	setSelectOption(sel, "", "Case", true);
	setSelectOption(sel, "page", "Page", false);
	setSelectOption(sel, "tab", "Tab", false);
	setSelectOption(sel, "mstf", "MSTF", false);

	sel = setSelect(tbody, "bgcolor", "Background Color:", "Choose the background shade for display");
	setSelectOption(sel, "", "Case", true);
	setSelectOption(sel, "light", "Light", false);
	setSelectOption(sel, "dark", "Dark", false);

	el = setCheckbox(tbody, "unknown", "Display as Unknowns:", "");

	div.appendChild( document.createElement("BR") );
	div.appendChild(makeServerButton("OK", setModifierValues));
	return div;

	function setSelect(tbody, id, heading, title) {
		var tr = document.createElement("TR");

		var td = document.createElement("TD");
		td.appendChild( document.createTextNode( heading ) );
		tr.appendChild(td);

		td = document.createElement("TD");
		var sel = document.createElement("SELECT");
		sel.id = id;
		sel.title = title;
		sel.style.width = 150;
		td.appendChild(sel);
		tr.appendChild(td);

		tbody.appendChild(tr);
		return sel;
	}

	function setSelectOption(sel, value, text, selected) {
		var opt = document.createElement("OPTION");
		opt.value = value;
		if (selected) opt.selected = true;
		opt.appendChild( document.createTextNode( text ) );
		sel.appendChild(opt);
	}

	function setCheckbox(parent, id, heading, title) {
		var tr = document.createElement("TR");

		var td = document.createElement("TD");
		td.appendChild( document.createTextNode( heading ) );
		tr.appendChild(td);

		td = document.createElement("TD");
		var cb = document.createElement("INPUT");
		cb.type = "checkbox";
		cb.id = id;
		cb.value = "yes";
		cb.title = title;
		td.appendChild(cb);
		tr.appendChild(td);

		tbody.appendChild(tr);
		return cb;
	}
}

function setModifierValues() {
	pageSize = getSelectedOption("maxresults");
	sortOrder = getSelectedOption("orderby");
	displayFormat = getSelectedOption("display");
	backgroundColor = getSelectedOption("bgcolor");
	unknown = getCBValue("unknown");
	if (queryMethod) queryMethod();
	hidePopups();

	function getSelectedOption(id) {
		var sel = document.getElementById(id);
		if (sel) return sel.value;
		else return "";
	}

	function getCBValue(id) {
		var cb = document.getElementById(id);
		if (cb) return cb.checked;
		else return false;
	}
}

//************************************************
//Admin tools
//************************************************

function toggleCTPAdmin() {
	toggle("CTPAdmin", "CTPAdminComponents");
}

//************************************************
//Collection queries
//************************************************
function deselectAll() {
	deselectCollection("MyDocuments");
	deselectCollection("AllDocuments");
	if (confTreeManager) confTreeManager.closePaths();
	if (fileTreeManager) fileTreeManager.closePaths();
}

function deselectCollection(id) {
	if (id == collectionID) {
		collectionID = null;
		collectionQuery = null;
	}
	var x = document.getElementById(id);
	if (x) x.firstChild.style.fontWeight = "normal";
}

function selectCollection( theCollectionQuery, theCollectionID ) {
	var x = document.getElementById(theCollectionID);
	if (x) {
		x.firstChild.style.fontWeight = "bold";
		collectionQuery = theCollectionQuery;
		collectionID = theCollectionID;
	}
}

function repeatSearch() {
	if (collectionQuery) collectionQuery();
	else queryAll();
}

function getBaseQuery() {
	return "xml=yes";
}

function queryMine() {
	if (user.isLoggedIn) {
		doQuery(getBaseQuery() + "&owner="+encodedUsername);
		selectCollection(queryMine, "MyDocuments");
	}
	else queryAll();
}

function queryAll() {
	doQuery(getBaseQuery());
	selectCollection(queryAll, "AllDocuments");
}

function firstPage() {
	if (collectionQuery) {
		firstResult =1;
		repeatSearch();
	}
}

function nextPage() {
	if (collectionQuery) {
		firstResult += maxResults;
		repeatSearch();
	}
}

function prevPage() {
	if (collectionQuery) {
		firstResult -= maxResults;
		if (firstResult < 1) firstResult = 1;
		repeatSearch();
	}
}

//************************************************
//Main query
//************************************************
function doQuery(query) {
	setCookies();
	deselectAll();
	query += getModifiers();
	var req = new AJAX();
	setStatusLine("Searching...");
	req.POST("/query", query+"&"+req.timeStamp(), processQueryResults);
}

//************************************************
//Query results
//************************************************
function processQueryResults(req) {
	if (req.success()) {
		var xml = req.responseXML();
		var qr = xml ? xml.firstChild : null;
		if (qr) {
			var pane = document.getElementById("right");
			while (pane.firstChild) pane.removeChild(pane.firstChild);

			var mds = qr.getElementsByTagName("MIRCdocument");
			if (mds.length > 0) {
				pane.appendChild( makeLinks(true) );
				var tbody = setTable(pane);
				for (var i=0; i<mds.length; i++) {
					var md = mds[i];
					appendDocument(tbody, md);
				}
				//pane.appendChild( makeLinks(true) );
				selectAll();
			}
			else right.appendChild(document.createTextNode("No results found."));
		}
		resize();
		setStatusLine("");
	}
}

function appendDocument(tbody, doc) {
	var tr = document.createElement("TR");
	tr.md = doc;
	appendTDCB(tr);
	appendTDA(tr, doc, "title", doc.getAttribute("docref"));
	appendTDLocation(tr, doc);
	appendTDAuthor(tr, doc);
	appendTD(tr, doc, "category")
	appendTD(tr, doc, "lmdate", "center")
	appendTDText(tr, " ");
	appendTD(tr, doc, "access");
	tbody.appendChild(tr);
}

function appendTDCB(tr) {
	var td = document.createElement("TD");
	td.className = "tableCB";
	var cb = document.createElement("INPUT");
	cb.type = "checkbox";
	td.appendChild(cb);
	tr.appendChild(td);
}
function appendTDA(tr, doc, tag, href) {
	var td = document.createElement("TD");
	var img = document.createElement("IMG");
	img.src = expandURL;
	img.className = "spaceRight";
	img.onclick = toggleDetails;
	td.appendChild(img);
	var a = document.createElement("A");
	a.href = href;
	a.target = "shared";
	a.className = "TitleLink";
	var text = ""
	var node = doc.getElementsByTagName(tag);
	if (node.length) {
		node = node[0].firstChild;
		if (node) text = node.nodeValue;
	}
	a.appendChild( document.createTextNode(text) );
	td.appendChild(a);
	tr.appendChild(td);
}
function appendTDText(tr, text) {
	var td = document.createElement("TD");
	td.appendChild( document.createTextNode(text) );
	tr.appendChild(td);
}
function appendTDAuthor(tr, doc) {
	var td = document.createElement("TD");
	var text = ""
	var authors = doc.getElementsByTagName("author");
	for (var i=0; i<authors.length; i++) {
		var names = authors[i].getElementsByTagName("name");
		for (var k=0; k<names.length; k++) {
			var name = names[k].firstChild;
			if (name) {
				var text = name.nodeValue;
				td.appendChild( document.createTextNode(text) );
				td.appendChild( document.createElement("BR") );
			}
		}
	}
	tr.appendChild(td);
}
function appendTDLocation(tr, doc) {
	var td = document.createElement("TD");
	var parent = doc.parentNode;
	var server = parent.getElementsByTagName("server");
	var text = ""
	if (server.length != 0) text = server[0].firstChild.nodeValue;
	td.appendChild( document.createTextNode( text ) );
	tr.appendChild(td);
}
function appendTD(tr, doc, tag, className) {
	var td = document.createElement("TD");
	if (className) td.className = className;
	var text = ""
	var node = doc.getElementsByTagName(tag);
	if (node.length) {
		node = node[0].firstChild;
		if (node) text = node.nodeValue;
	}
	td.appendChild( document.createTextNode(text) );
	tr.appendChild(td);
}

function setTable(pane) {
	pane.appendChild( document.createElement("BR") );
	var table = document.createElement("TABLE");
	pane.appendChild(table);
	pane.appendChild( document.createElement("BR") );

	var thead = document.createElement("THEAD");
	table.appendChild(thead);
	var tr = document.createElement("TR");
	thead.appendChild(tr);
	appendTHCB(tr);
	appendTHTitle(tr);
	appendTH(tr, "Library");
	appendTH(tr, "Author");
	appendTH(tr, "Specialty");
	appendTH(tr, "Date Modified");
	appendTH(tr, "Rating");
	appendTH(tr, "Access");
	thead.appendChild(tr);
	var tbody = document.createElement("TBODY");
	tbody.id = "tableBody";
	table.appendChild(tbody);
	return tbody;
}
function appendTH(tr, text) {
	var th = document.createElement("TH");
	th.appendChild(document.createTextNode(text));
	tr.appendChild(th);
}
function appendTHTitle(tr) {
	var th = document.createElement("TH");
	var img = document.createElement("IMG");
	img.src = expandURL;
	img.className = "spaceRight";
	img.onclick = toggleExpandCollapse;
	img.id = "expandCollapseImg";
	img.title = "Expand/Collapse";
	th.appendChild(img);
	th.appendChild(document.createTextNode("Title"));
	tr.appendChild(th);
}
function appendTHCB(tr) {
	var th = document.createElement("TH");
	var cb = document.createElement("INPUT");
	cb.type = "checkbox";
	cb.id = "tableHeaderCB";
	cb.onclick = toggleSelect;
	cb.title = "Select/Deselect";
	th.appendChild(cb);
	tr.appendChild(th);
}

//************************************************
//Command links
//************************************************
function makeLinks(includeNextPrev) {
	var div = document.createElement("DIV");
	div.className = "links";
	if (includeNextPrev) {
		div.appendChild( makeLink("firstPage", "First Page") );
		div.appendChild( document.createTextNode( " | " ) );
		div.appendChild( makeLink("nextPage", "Next Page") );
		div.appendChild( document.createTextNode( " | " ) );
		div.appendChild( makeLink("prevPage", "Previous Page") );
		div.appendChild( document.createTextNode( " | " ) );
	}
	div.appendChild( makeLink("displayCN", "Display in Case Navigator") );
	return div;
}
function makeLink(func, text) {
	var a = document.createElement("A");
	a.href = "javascript:"+func+"()";
	a.appendChild( document.createTextNode(text) );
	return a;
}

function toggleExpandCollapse() {
	var img = document.getElementById("expandCollapseImg");
	if (img.src.indexOf(expandURL) == -1) {
		collapseAll();
		img.src = expandURL;
	}
	else {
		expandAll();
		img.src = collapseURL;
	}
}

function setExpandCollapse() {
	var tbody = document.getElementById("tableBody");
	var cbs = tbody.getElementsByTagName("INPUT");
	var isExpanded = false;
	for (var i=0; i<cbs.length; i++) {
		var cb = cbs[i];
		if (cb.type == "checkbox") {
			var td = cb.parentNode.nextSibling;
			var tr = td.parentNode;
			var img = td.firstChild;
			isExpanded |= (img.src.indexOf(expandURL) == -1);
		}
	}
	var img = document.getElementById("expandCollapseImg");
	if (!isExpanded) {
		img.src = expandURL;
	}
	else {
		img.src = collapseURL;
	}
}

function expandAll() {
	var tbody = document.getElementById("tableBody");
	var cbs = tbody.getElementsByTagName("INPUT");
	for (var i=0; i<cbs.length; i++) {
		var cb = cbs[i];
		if (cb.type == "checkbox") {
			var td = cb.parentNode.nextSibling;
			var tr = td.parentNode;
			var img = td.firstChild;
			var tbody = tr.parentNode;
			expandDetails(tbody, tr, td, img);
		}
	}
}

function collapseAll() {
	var tbody = document.getElementById("tableBody");
	var cbs = tbody.getElementsByTagName("INPUT");
	for (var i=0; i<cbs.length; i++) {
		var cb = cbs[i];
		if (cb.type == "checkbox") {
			var td = cb.parentNode.nextSibling;
			var tr = td.parentNode;
			var img = td.firstChild;
			var tbody = tr.parentNode;
			collapseDetails(tbody, tr, td, img);
		}
	}
}

function toggleDetails(event) {
	var evt = getEvent(event);
	var img = getSource(evt);
	var visible = (img.src.indexOf(expandURL) == -1);
	var td = img.parentNode;
	var tr = td.parentNode;
	var tbody = tr.parentNode;

	if (visible) collapseDetails(tbody, tr, td, img);
	else expandDetails(tbody, tr, td, img);
}

function expandDetails(tbody, tr, td, img) {
	var visible = (img.src.indexOf(expandURL) == -1);
	if (!visible) {
		var nTD = tr.getElementsByTagName("TD").length;
		var newTR = document.createElement("TR");
		var blankTD = document.createElement("TD");
		blankTD.className = "blankTD";
		newTR.appendChild(blankTD);
		var detailsTD = document.createElement("TD");
		detailsTD.className = "detailsTD";
		detailsTD.colSpan = nTD - 1;

		var abs = tr.md.getElementsByTagName("abstract");
		if (abs.length > 0) {
			importSpecial(detailsTD, abs[0]);
		}

		var docref = tr.md.getAttribute("docref");
		var basepath = docref.substring( 0, docref.lastIndexOf("/")+1 );
		var images = tr.md.getElementsByTagName("images");
		if (images.length > 0) {
			images = images[0].getElementsByTagName("image");
			if (images.length > 0) {
				var p = document.createElement("P");
				for (var i=0; i<images.length; i++) {
					var image = document.createElement("IMG");
					image.className = "detailsIMG";
					image.src = basepath + images[i].getAttribute("src");
					detailsTD.appendChild(image);
				}
			}
		}

		newTR.appendChild(detailsTD);
		tbody.insertBefore(newTR, tr.nextSibling);
		img.src = collapseURL;
		var titleimg = document.getElementById("expandCollapseImg");
		titleimg.src = collapseURL;
	}
}

function collapseDetails(tbody, tr, td, img) {
	var visible = (img.src.indexOf("expand") == -1);
	if (visible) {
		tbody.removeChild(tr.nextSibling);
		img.src = expandURL;
	}
	setExpandCollapse();
}

function importSpecial(dest, node) {
	var type = node.nodeType;
	if (type == 1) {
		var tag = node.tagName.toLowerCase();
		var target = dest;
		if (tag == "p") {
			var p = document.createElement("P");
			dest.appendChild(p);
			target = p;
		}
		else if (tag == "a") {
			var a = document.createElement("A");
			a.href = node.getAttribute("href");
			dest.appendChild(a);
			target = a;
		}
		var child = node.firstChild;
		while (child) {
			importSpecial(target, child);
			child = child.nextSibling;
		}
	}
	else if (type == 3) {
		dest.appendChild( document.createTextNode( node.nodeValue ) );
	}
}

function selectAll() {
	var right = document.getElementById("right");
	var cbs = right.getElementsByTagName("INPUT");
	for (var i=0; i<cbs.length; i++) {
		var cb = cbs[i];
		if (cb.type == "checkbox") cb.checked = true;
	}
}

function selectNone() {
	var right = document.getElementById("right");
	var cbs = right.getElementsByTagName("INPUT");
	for (var i=0; i<cbs.length; i++) {
		var cb = cbs[i];
		if (cb.type == "checkbox") cb.checked = false;
	}
}

function toggleSelect() {
	var cb = document.getElementById("tableHeaderCB");
	if (cb) {
		if (cb.checked) selectAll();
		else selectNone();
	}
}

function displayCN() {
	var tbody = document.getElementById("tableBody");
	var cbs = tbody.getElementsByTagName("INPUT");
	var urls = ""
	for (var i=0; i<cbs.length; i++) {
		var cb = cbs[i];
		if ((cb.type == "checkbox") && cb.checked) {
			var td = cb.parentNode.nextSibling;
			var a = td.getElementsByTagName("A")[0];
			var url = a.getAttribute("href");
			if (urls != "") urls += "|";
			urls += url;
		}
	}
	if (urls != "") window.open("/casenav?suppressHome=yes&urls="+urls, "shared");
}

//************************************************
//Approval Queue
//************************************************
function approvalQueue() { alert("approvalQueue"); }

//************************************************
//Conferences
//************************************************
function loadConferences() {
	confTreeManager =
		new TreeManager(
			"confs",
			"/confs/tree",
			"/mirc/images/plus.gif",
			"/mirc/images/minus.gif");
	confTreeManager.load();
	confTreeManager.display();
	confTreeManager.expandAll();
}

function showConferenceContents(eveent) {
	var source = getSource(getEvent(event));
	var currentNode = source.treenode;
	deselectAll();
	currentNode.showPath();

	var req = new AJAX();
	req.GET("/confs/getAgenda", "nodeID="+currentNode.nodeID+"&"+req.timeStamp(), null);
	if (req.success()) {
		var pane = document.getElementById("right");
		while (pane.firstChild) pane.removeChild(pane.firstChild);

		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;

		var items = root.getElementsByTagName("item");
		if (items.length > 0) {
			pane.appendChild( makeLinks(false) );
			var tbody = setConferenceTable(pane);
			for (var i=0; i<items.length; i++) {
				var item = items[i];
				appendAgendaItem(tbody, item);
			}
			//pane.appendChild( makeLinks(false) );
			selectAll();
		}
		else right.appendChild(document.createTextNode("The conference is empty."));
	}
	resize();
}

function setConferenceTable(pane) {
	pane.appendChild( document.createElement("BR") );
	var table = document.createElement("TABLE");
	pane.appendChild(table);
	pane.appendChild( document.createElement("BR") );

	var thead = document.createElement("THEAD");
	table.appendChild(thead);
	var tr = document.createElement("TR");
	thead.appendChild(tr);
	appendTH(tr, "");
	appendTH(tr, "Title");
	appendTH(tr, "Author");
	thead.appendChild(tr);
	var tbody = document.createElement("TBODY");
	tbody.id = "tableBody";
	table.appendChild(tbody);
	return tbody;
}

function appendAgendaItem(tbody, item) {
	var tr = document.createElement("TR");

	appendTDCB(tr);

	var td = document.createElement("TD");
	var a = document.createElement("A");
	a.href = item.getAttribute("url");
	a.target = "shared";
	a.className = "TitleLink";
	a.appendChild( document.createTextNode(item.getAttribute("title") ) );
	td.appendChild(a);
	tr.appendChild(td);

	var td = document.createElement("TD");
	td.appendChild( document.createTextNode(item.getAttribute("subtitle") ) );
	tr.appendChild(td);

	tbody.appendChild(tr);
}

//************************************************
//File Cabinets
//************************************************
function loadFileCabinets() {
	fileTreeManager =
		new TreeManager(
			"cabs",
			"/files/tree",
			"/mirc/images/plus.gif",
			"/mirc/images/minus.gif");
	fileTreeManager.load();
	fileTreeManager.display();
	fileTreeManager.expandAll();
}

//Handlers for tree selection
//
function showFileDirContents(event) {
	var source = getSource(getEvent(event));
	var currentNode = source.treenode;
	deselectAll();
	var currentPath = currentNode.getPath();
	fileTreeManager.closePaths();
	currentNode.showPath();
	//menuBar.setText(currentPath);
	var req = new AJAX();
	req.GET("/files/mirc/"+currentPath, req.timeStamp(), null);
	if (req.success()) {
		var right = document.getElementById("right");
		while (right.firstChild) right.removeChild(right.firstChild);
		//nFiles = 0;
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		while (child) {
			if ((child.nodeType == 1) && (child.tagName == "file")) {
				var img = document.createElement("IMG");
				img.className = "desel";
				//img.onclick = cabinetFileClicked;
				img.ondblclick = cabinetFileDblClicked;
				//img.onmousedown = startImageDrag;
				img.setAttribute("src", "/files/"+child.getAttribute("iconURL"));
				img.setAttribute("title", child.getAttribute("title"));
				img.xml = child;
				right.appendChild(img);
				//nFiles++;
			}
			child = child.nextSibling;
		}
		//lastClicked = -1;
		//nodeType = "MircFolder";
		//nSelected = 0;
	}
	else alert("The attempt to get the directory contents failed.");
	//setEnables();
}

function cabinetFileDblClicked(theEvent) {
	var theEvent = getEvent(theEvent)
	stopEvent(theEvent);
	var source = getSource(theEvent);
	var currentClicked = getClicked(source);
	if (currentClicked == -1) return;
	var fileURL = "/files/" + source.xml.getAttribute("fileURL");
	deselectAll();
	source.className = "sel";
	lastClicked = currentClicked;
	//setEnables();

	if (theEvent.altKey) {
		var filename = source.getAttribute("title");
		if ((filename.toLowerCase().lastIndexOf(".dcm") == filename.length - 4) ||
					(filename.replace(/[\.\d]/g,"").length == 0)) {
			fileURL += "?list";
		}
	}
	window.open(fileURL, "_blank");
}

function getClicked(file) {
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

//************************************************
//Submit Service
//************************************************
function submitService(ssid) {
	deselectAll();
	var right = document.getElementById("right");
	while (right.firstChild) right.removeChild(right.firstChild);
	var iframe = document.createElement("IFRAME");
	iframe.src = "/submit/"+ssid+"?ui=integrated";
	right.appendChild(iframe);
}

//************************************************
//Libraries classes
//************************************************
function LocalLibrary(id, title, authenb, subenb, zipenb ) {
	this.id = id;
	this.title = title;
	this.authenb = authenb;
	this.subenb = subenb;
	this.zipenb = zipenb;
}
LocalLibrary.prototype.toString = function() {
	var s = "id      = "+this.id+"\n" +
			"title   = "+this.title+"\n" +
			"authenb = "+this.authenb+"\n" +
			"subenb  = "+this.subenb+"\n" +
			"zipenb  = "+this.zipenb+"\n";
	return s;
}

function Library(enb, addr, svrname, local) {
	this.enabled = (enb=='yes');
	this.address = addr;
	this.name = svrname;
	this.isLocal = (local=='yes');
}

//************************************************
//Cookie functions
//************************************************
function setState() {
	var cookies = getCookieObject();
	//var clib = cookies["selectedlib"];
	//if (clib != null) selectedLocalLibrary = parseInt(cserv);
	setSelectFromCookie("serverselect", cookies);
	setSelectFromCookie("maxresults", cookies);
	setSelectFromCookie("display", cookies);
	setSelectFromCookie("bgcolor", cookies);
	setCheckboxFromCookie("unknown", cookies);
}

function setCookies() {
	setSelectCookie("serverselect");
	setSelectCookie("maxresults");
	setSelectCookie("display");
	setSelectCookie("bgcolor");
	setCheckboxCookie("unknown");
}

function setTextCookie(id) {
	var el = document.getElementById(id);
	if (el != null) {
		var text = el.value;
		setCookie(id, text);
	}
}

function setSelectCookie(id) {
	var el = document.getElementById(id);
	if (el != null) setCookie(id, getOptions(el));
}

function getOptions(el) {
	var text = "";
	var opts = el.getElementsByTagName("OPTION");
	for (var i=0; i<opts.length; i++) {
		if (opts[i].selected) {
			if (text != "") text += ":";
			text += i;
		}
	}
	return text;
}

function setCheckboxCookie(id) {
	var el = document.getElementById(id);
	if (el != null) setCookie(id, el.checked);
}

function setTextFromCookie(id, cookies) {
	var ctext = cookies[id];
	var el = document.getElementById(id);
	if ( (el != null) && (ctext != null) ) {
		el.value = ctext;
	}
}

function setSelectFromCookie(id, cookies) {
	var ctext = cookies[id];
	if (ctext == null) return;
	var ints = ctext.split(":");
	for (var i=0; i<ints.length; i++) {
		ints[i] = parseInt(ints[i]);
	}
	var el = document.getElementById(id);
	if (el == null) return;
	var opts = el.getElementsByTagName("OPTION");
	for (var i=0; i<opts.length; i++) {
		opts[i].selected = false;
	}
	for (var i=0; i<ints.length; i++) {
		var k = ints[i];
		if ((k >= 0) && (k < opts.length)) {
			opts[k].selected = true;
		}
	}
}

function setCheckboxFromCookie(id, cookies) {
	var ctext = cookies[id];
	var el = document.getElementById(id);
	if ( (el != null) && (ctext != null) ) {
		el.checked = (ctext == "true");
	}
}

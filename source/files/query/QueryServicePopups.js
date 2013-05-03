function showServersPopup(requery) {
	var id = "serversPopupID";
	var title = "Select Libraries";
	var closebox = "/icons/closebox.gif";
	var pop = document.getElementById(id);
	if (pop) {
		var ssWH = setServerSelectWH(pop);
		showPopup(id, ssWH.w + 30, ssWH.h + 80, title, closebox);
	}
	else {
		var div = getServerSelectDiv(requery);
		var ssWH = setServerSelectWH(div);
		showDialog(id, ssWH.w + 30, ssWH.h + 80, title, closebox, null, div, null, null);
	}
}

function createServersPopup() {
	var id = "serversPopupID";
	var title = "Select Libraries";
	var closebox = "/icons/closebox.gif";
	var div = getServerSelectDiv();
	var ssWH = setServerSelectWH(div);
	showDialog(id, ssWH.w + 30, ssWH.h + 80, title, closebox, null, div, null, null, true);
}

function getServerSelectDiv() {
	var div = document.createElement("DIV");
	div.className = "content";
	var sel = document.createElement("SELECT");
	sel.id = "serverselect";
	sel.multiple = "true";
	appendServers(sel);
	div.appendChild(sel);
	div.appendChild(makeServerButton("Select All", selectAllServers));
	div.appendChild(makeServerButton("Select Local", selectLocalServers));
	div.appendChild(makeServerButton("OK", serverSelectOK));
	return div;
}

function serverSelectOK() {
	hidePopups();
	if (collectionQuery) {
		firstResult = 1;
		collectionQuery();
	}
}

function appendServers(sel) {
	for (var i=0; i<allServers.length; i++) {
		var opt = document.createElement("OPTION");
		//if (allServers[i].isLocal) opt.selected = true;
		opt.value = i;
		opt.appendChild(document.createTextNode(allServers[i].name));
		sel.appendChild(opt);
	}
}

function makeServerButton(name, fn) {
	var b = document.createElement("INPUT");
	b.type = "button";
	b.className = "stdbutton";
	b.style.width = 100;
	b.value = name;
	b.onclick = fn;
	return b;
}

function setServerSelectWH(div) {
	var select = div.getElementsByTagName("SELECT")[0];
	var options = select.getElementsByTagName("OPTION");
	var h = 15 * options.length + 40;
	if (h > 500) h = 500;
	var w = 440;
	select.style.height = h + "px";
	select.style.width = w + "px";
	var wh = new Object();
	wh.w = w;
	wh.h = h;
	return wh;
}

function selectAllServers() {
	var ss = document.getElementById("serverselect");
	var options = ss.getElementsByTagName("OPTION");
	for (var i=0; i<options.length; i++) {
		options[i].selected = true;
	}
}

function selectLocalServers() {
	var ss = document.getElementById("serverselect");
	var options = ss.getElementsByTagName("OPTION");
	for (var i=0; i<options.length; i++) {
		options[i].selected = allServers[ options[i].value ].isLocal;
	}
}

function showAboutPopup() {
	var id = "aboutPopupID";
	var pop = document.getElementById(id);
	if (pop) pop.parentNode.removeChild(pop);

	var w = 600;
	var h = 600;

	var div = document.createElement("DIV");
	div.className = "content";
	var h1 = document.createElement("H1");
	h1.appendChild(document.createTextNode("RSNA Teaching File System"));
	h1.style.fontSize = "24pt";
	div.appendChild(h1);
	div.appendChild(document.createTextNode("\u00A0"));
	var p = document.createElement("P");
	p.appendChild(document.createTextNode("Version "+version));
	p.appendChild(document.createElement("BR"));
	p.appendChild(document.createTextNode(date));

	var deltaH = 0;
	if (user.hasRole("admin") && (version < rsnaVersion)) {
		p.appendChild(document.createElement("BR"));
		p.appendChild(document.createTextNode("["));
		var anchor = document.createElement("A");
		anchor.setAttribute("href", "http://mirc.rsna.org/download");
		anchor.setAttribute("target", "download");
		anchor.appendChild(document.createTextNode("TFS version "+rsnaVersion+" is now available"));
		p.appendChild(anchor);
		p.appendChild(document.createTextNode("]"));
		deltaH = 16;
	}

	div.appendChild(p);

	div.appendChild(document.createTextNode("\u00A0"));
	p = document.createElement("P");
	var iframe = document.createElement("IFRAME");
	iframe.style.width = w - 30;
	iframe.style.height = h - 171 - deltaH;
	iframe.src = "/query/credits.html";
	p.appendChild(iframe);
	div.appendChild(p);

	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "About RSNA TFS", closebox, null, div, null, null);
}

function showSearchingPopup() {
	var id = "searchingPopupID";
	var pop = document.getElementById(id);
	if (pop) pop.parentNode.removeChild(pop);

	var div = document.createElement("DIV");
	div.className = "content";
	var w = 200;
	var h = 90;
	var x = document.createElement("H3");
	x.appendChild(document.createTextNode("Searching..."));
	x.style.fontSize = "14pt";
	div.appendChild(x);
	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "Search in progress", closebox, null, div, null, null);
}

function showHelpPopup() {
	var id = "helpPopupID";
	var pop = document.getElementById(id);
	if (pop) pop.parentNode.removeChild(pop);

	var div = document.createElement("DIV");
	div.className = "content";
	var w = 400;
	var h = 400;
	var iframe = document.createElement("IFRAME");
	iframe.style.width = w - 30;
	iframe.style.height = h - 55;
	iframe.src = "/query/help.html";
	div.appendChild(iframe);
	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "RSNA TFS Help", closebox, null, div, null, null);
}

function showDisclaimerPopup() {
	if (disclaimerURL != "") {
		var id = "disclaimerPopupID";
		var pop = document.getElementById(id);
		if (pop) pop.parentNode.removeChild(pop);

		var div = document.createElement("DIV");
		div.className = "content";
		var w = 650;
		var h = 400;
		var iframe = document.createElement("IFRAME");
		iframe.style.width = w - 30;
		iframe.style.height = h - 55;
		iframe.src = disclaimerURL;
		div.appendChild(iframe);
		var closebox = "/icons/closebox.gif";
		showDialog(id, w, h, null, closebox, null, div, null, null);
	}
}

function showSessionPopup() {
	var cooks = getCookieObject();

	var admin = getCookie("ADMIN", cooks);
	if ((admin == "") && user.isLoggedIn && user.hasRole("admin")) {
		setSessionCookie("ADMIN", "session");
		if (checkActivityReport()) {
			showActivityReportPopup();
			return;
		}
		if ((sitename == "My Teaching Files") || (email == "")) {
			showSitePopup();
			return;
		}
	}

	var mirc = getCookie("MIRC", cooks);
	if (mirc == "") {
		setSessionCookie("MIRC", "session");
		if (sessionPopup == "help") {
			showHelpPopup();
		}
		else if (sessionPopup == "notes") {
			showDisclaimerPopup();
		}
		else if (sessionPopup == "login") {
			if (!user.isLoggedIn) loginLogout();
		}
	}
}

function checkActivityReport() {
	var req = new AJAX();
	req.GET("/activity/check", req.timeStamp(), null);
	if (req.success()) {
		return (req.responseText() == "old");
	}
	return false
}

function showActivityReportPopup() {
	var id = "activityPopupID";
	var pop = document.getElementById(id);
	if (pop) pop.parentNode.removeChild(pop);

	var w = 375;
	var h = 375;

	var div = document.createElement("DIV");
	div.className = "content";
	var h1 = document.createElement("H1");
	h1.appendChild(document.createTextNode("Activity Report"));
	h1.style.fontSize = "24pt";
	div.appendChild(h1);
	div.appendChild(document.createTextNode("\u00A0"));
	var p = document.createElement("P");
	p.style.textAlign = "left";
	p.appendChild(document.createTextNode(
		"This site has not sent an activity " +
		"summary report to the RSNA for more " +
		"than one week. Click the button below " +
		"to view the detailed activity report on your " +
		"your site."));
	div.appendChild(p);

	div.appendChild(document.createElement("BR"));

	p = document.createElement("P");
	var button = document.createElement("INPUT");
	button.className = "stdbutton";
	button.style.width = "200px";
	button.type = "button";
	button.value = "View Activity Report";
	button.onclick = loadActivityReport;
	p.appendChild(button);
	div.appendChild(p);

	div.appendChild(document.createElement("BR"));

	p = document.createElement("P");
	p.style.textAlign = "left";
	p.appendChild(document.createTextNode(
		"Click the button below to send a " +
		"summary of the detailed activity " +
		"report to the RSNA site."));
	div.appendChild(p);

	div.appendChild(document.createElement("BR"));

	p = document.createElement("P");
	button = document.createElement("INPUT");
	button.className = "stdbutton";
	button.style.width = "200px";
	button.type = "button";
	button.value = "Send Summary Report";
	button.onclick = sendSummaryReport;
	p.appendChild(button);
	div.appendChild(p);

	div.appendChild(document.createElement("BR"));

	var p = document.createElement("P");
	p.style.textAlign = "left";
	p.appendChild(document.createTextNode(
		"You can enable the automatic sending\n" +
		"of summary reports on the "));
	var a = document.createElement("A");
	a.href = "/qsadmin";
	a.appendChild(document.createTextNode("Query Service Admin"));
	p.appendChild(a);
	p.appendChild(document.createTextNode(" page."));
	div.appendChild(p);

	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "Activity Report", closebox, null, div, null, null);
}

function loadActivityReport() {
	hidePopups();
	window.open("/activity", "shared");
}

function sendSummaryReport() {
	hidePopups();
	var req = new AJAX();
	//get the summary report from the local server
	req.GET("/activity", "format=xml&type=summary&"+req.timeStamp(), null);
	if (req.success()) {
		//send the report to the RSNA site
		var report = req.responseText();
		showSubmissionPopup("http://mirc.rsna.org/activity/submit?report="+encodeURIComponent(report)+"&"+req.timeStamp());
		//update the lastReportTime on the local server
		var updatereq = new AJAX();
		updatereq.GET("/activity/update", req.timeStamp(), null);
	}
	else alert("Unable to obtain the summary report from the local server.");
}

function showSitePopup() {
	var id = "sitePopupID";
	var pop = document.getElementById(id);
	if (pop) pop.parentNode.removeChild(pop);

	var w = 375;
	var h = 255;

	var div = document.createElement("DIV");
	div.className = "content";
	var h1 = document.createElement("H1");
	h1.appendChild(document.createTextNode("Update Site Info"));
	h1.style.fontSize = "24pt";
	div.appendChild(h1);
	div.appendChild(document.createTextNode("\u00A0"));
	var p = document.createElement("P");
	p.style.textAlign = "left";
	p.appendChild(document.createTextNode(
		"The RSNA requests that all sites choose site " +
		"names that include the name of the institution " +
		"and that sites also provide an email address. " +
		"Click the button below to go to the Query Service " +
		"Admin page to make any desired changes. "));
	div.appendChild(p);

	div.appendChild(document.createElement("BR"));

	p = document.createElement("P");
	var button = document.createElement("INPUT");
	button.className = "stdbutton";
	button.style.width = "200px";
	button.type = "button";
	button.value = "Query Service Admin Page";
	button.onclick = gotoQSAdmin;
	p.appendChild(button);
	div.appendChild(p);

	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "Update Site Info", closebox, null, div, null, null);
}

function gotoQSAdmin() {
	hidePopups();
	window.open("/qsadmin", "_self");
}

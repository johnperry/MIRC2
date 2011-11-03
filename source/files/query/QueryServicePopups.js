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
	div.appendChild(p);
	div.appendChild(document.createTextNode("\u00A0"));
	p = document.createElement("P");
	p.appendChild(document.createTextNode(date));
	div.appendChild(p);

	div.appendChild(document.createTextNode("\u00A0"));
	p = document.createElement("P");
	var iframe = document.createElement("IFRAME");
	iframe.style.width = w - 30;
	iframe.style.height = h - 190;
	iframe.src = "/query/credits.html";
	p.appendChild(iframe);
	div.appendChild(p);

	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "About RSNA TFS", closebox, null, div, null, null);
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
	var cook = getCookie("MIRC", cooks);
	if ((cook == null) || (cook == "")) {
		setSessionCookie("MIRC", "session");
		if (sessionPopup == "help") {
			showHelpPopup();
		}
		else if (sessionPopup == "notes") {
			showDisclaimerPopup();
		}
	}
}

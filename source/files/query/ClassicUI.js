var current_page;
var current_tab;
var user;
var collectionQuery = null;//not used in the classic UI

function loaded() {
	user = new User();
	setLocalLibrariesMenu();
	setMenuEnables();
	menuBar.display();
	createLoginDiv();
	createServersPopup();
	setState();
	current_page = document.getElementById('div1');
	current_tab = document.getElementById('page1tab');
	selectTab(current_tab);
	var freeText = document.getElementById("freetext");
	freeText.focus();
	if (!document.all) {
		var titleDiv = document.getElementById("pagetitle");
		titleDiv.style.color = "blue";
	}
	showNews();
	showSessionPopup();
}
window.onload = loaded;

function createLoginDiv() {
	var sib = document.getElementById("pagetitle");
	if (user.isLoggedIn) {
		//make the username div
		var div = document.createElement("DIV");
		div.id = "login";
		div.className = "login";
		div.appendChild(document.createTextNode(user.name));
		//make the logout div
		var lbdiv = document.createElement("DIV");
		lbdiv.className = "logout";
		lbdiv.onclick = mircLogout;
		lbdiv.appendChild(document.createTextNode("(logout)"));
		//put them all together
		var logmain = document.createElement("DIV");
		logmain.className = "logmain";
		logmain.appendChild(div);
		logmain.appendChild(document.createElement("BR"));
		logmain.appendChild(document.createElement("BR"));
		logmain.appendChild(lbdiv);
		sib.parentNode.insertBefore(logmain, sib);
		menuBar.setText("You are logged in as...");
	}
	else {
		var div = document.createElement("DIV");
		div.id = "login";
		div.className = "login";
		var b = document.createElement("INPUT");
		b.className = "login";
		b.type = "button";
		b.value = "Login";
		b.onclick = mircLogin;
		div.appendChild(b);
		sib.parentNode.insertBefore(div, sib);
		menuBar.setText("Login to Access All Features");
	}
}

function mircLogin() {
	if (user.loginURL == "") showLoginPopup('/query');
	else window.open(user.loginURL, "_self");
}

function mircLogout() {
	if (user.logoutURL == "") logout('/query');
	else window.open(user.logoutURL, "_self");
}

function setLocalLibrariesMenu() {
	var items = new Array();
	for (var i=0; i<localLibraries.length; i++) {
		var item = new Item( localLibraries[i].title, selectLocalLibrary );
		item.libIndex = i;
		items[items.length] = item;
	}
	localLibrariesItems = items;
	authorItems[1] = new Menu("Select Local Library", items);
	menuBar.setPointers();
}

function setState() {
	var cookies = getCookieObject();
	var clib = cookies["selectedlib"];
	if (clib != null) selectedLocalLibrary = parseInt(cserv);

	var session = (getCookie("MIRC", cookies) != "");
	if (session) setSelectFromCookie("serverselect", cookies);
	else {
		var svrsel = document.getElementById("serverselect");
		var opts = svrsel.getElementsByTagName("OPTION");
		var k = 0;
		for (var i=0; i<allServers.length; i++) {
			if (allServers[i].enabled) {
				if (allServers[i].deflib) opts[k].selected = true;
				k++;
			}
		}
	}

	setSelectFromCookie("maxresults", cookies);
	setSelectFromCookie("display", cookies);
	setSelectFromCookie("bgcolor", cookies);
	setCheckboxFromCookie("unknown", cookies);
	setCheckboxFromCookie("showimages", cookies);
	setCheckboxFromCookie("casenavigator", cookies);
	setCheckboxFromCookie("randomize", cookies);
	setCheckboxFromCookie("icons", cookies);
}

function showNews() {
	var newsDiv = document.getElementById("news");
	if (newsDiv != null) {
		var goButton = document.getElementById("go");
		var goPos = findObject(goButton);
		newsDiv.style.top = goPos.y;
		newsDiv.style.left = 0;
		newsDiv.style.zIndex = 1;
		newsDiv.style.visibility = "visible";
		newsDiv.style.display = "block";
	}
}

function setBreedList() {
	var ptSpecies = document.getElementById("pt-species");
	var ptBreed = document.getElementById("pt-breed");
	ptBreed.options.length = 0;
	var choice = ptSpecies.selectedIndex;
	var breedlist = breeds[choice];
	ptBreed.options[0] = new Option("","");
	for (var i=0; i<breedlist.length; i++) {
		 ptBreed.options[i+1] = new Option(breedlist[i], breedlist[i]);
	}
}

function bclick(next_page_Id, theEvent) {
	var clicked_tab = getSource(theEvent);
	var next_page = document.getElementById(next_page_Id);
	if (current_page != next_page) {
		current_page.style.visibility="hidden";
		current_page.style.display="none";
		current_page = next_page;
		current_page.style.visibility="visible";
		current_page.style.display="block";
		deselectTab(current_tab);
		current_tab = clicked_tab;
		selectTab(current_tab);
		showNews();
	}
}

function selectTab(tab) {
	tab.style.backgroundColor = "#2977b9";
	tab.style.color = 'white';
}

function deselectTab(tab) {
	tab.style.backgroundColor = 'white';
	tab.style.color = "#2977b9";
}

function clearQueryFields() {
	var form = document.getElementById("queryform");
	var inputs = form.getElementsByTagName("INPUT");
	for (var i=0; i<inputs.length; i++) {
		if (inputs[i].type == "text") inputs[i].value = "";
		else if ((inputs[i].type == "checkbox") &&
				 (inputs[i].name != "unknown") &&
				 (inputs[i].name != "casenavigator") &&
				 (inputs[i].name != "randomize") &&
				 (inputs[i].name != "icons")) inputs[i].checked = false;
	}
	var selects = form.getElementsByTagName("SELECT");
	for (var i=0; i<selects.length; i++) {
		if ((selects[i].name != "serverselect") &&
			(selects[i].name != "maxresults") &&
			(selects[i].name != "display") &&
			(selects[i].name != "bgcolor")) selects[i].selectedIndex = 0;
	}
}

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

function Library(enb, def, addr, svrname, local) {
	this.enabled = (enb=='yes');
	this.deflib = (def=='yes');
	this.address = addr;
	this.name = svrname;
	this.isLocal = (local=='yes');
}

function setServerIDs() {
	var server = document.getElementById("server");
	var sel = document.getElementById("serverselect");
	if (sel == null) { alert("serverselect is null"); return; }
	var svrs = getOptions(sel);
	server.value = svrs;
}

function setCookies() {
	setSelectCookie("serverselect");
	setSelectCookie("maxresults");
	setSelectCookie("display");
	setSelectCookie("bgcolor");
	setCheckboxCookie("unknown");
	setCheckboxCookie("showimages");
	setCheckboxCookie("casenavigator");
	setCheckboxCookie("randomize");
	setCheckboxCookie("icons");
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
	setCookie(id, el.checked);
}

function setTextFromCookie(id, cookies) {
	var ctext = cookies[id];
	if (ctext == null) return;
	document.getElementById(id).value = ctext;
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
	if (ctext == null) return;
	document.getElementById(id).checked = (ctext == "true");
}

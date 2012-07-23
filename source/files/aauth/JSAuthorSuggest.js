function PrefsDB() {
	this.prefs = new Array();
}

PrefsDB.prototype.load = function() {
	this.prefs = new Array();
	var req = new AJAX();
	req.GET("/prefs/xml/allusers", req.timeStamp(), null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml.firstChild;
		var users = root.getElementsByTagName("User");
		for (var i=0; i<users.length; i++) {
			this.prefs.push(new Pref(users[i]));
		}
	}
}

PrefsDB.prototype.getMatches = function(s, maxMatches) {
	s = s.toLowerCase();
	var count = 0;
	var matches = new Array();
	for (var i=0; i<this.prefs.length; i++) {
		if (this.prefs[i].matches(s)) {
			matches.push(this.prefs[i]);
			count++;
		}
		if(count >= maxMatches) break;
	}
	return matches;
}

function Pref(user) {
	this.username = user.getAttribute("username");
	this.personname = user.getAttribute("name");
	this.personnameLC = this.personname.toLowerCase();
	this.affiliation = user.getAttribute("affiliation");
	this.contact = user.getAttribute("contact");
}

Pref.prototype.matches = function(s) {
	return (this.personnameLC.indexOf(s) != -1);
}

var prefsDB = null;
var lastAuthorTextField = null;

function startAuthorSuggest(event) {
	var evt = getEvent(event);
	var src = getSource(evt);

	if (src === lastAuthorTextField) return;
	lastAuthorTextField = src;

	var div = document.getElementById("suggest");
	if (div == null) {
		div = document.createElement("DIV");
		div.className = "AuthorSuggest";
		div.style.visibility = "hidden";
		document.body.appendChild(div);
	}

	if (prefsDB == null) {
		prefsDB = new PrefsDB();
		prefsDB.load();
	}

	new AuthorSuggest(prefsDB, src, div, 10);
}

function AuthorSuggest(db, oText, oDiv, maxSize) {
	this.db = db;
	this.oText = oText;
	this.oDiv = oDiv;
	this.maxSize = maxSize;
	this.cur = -1;

	oText.onkeyup = this.keyUp;
	oText.onkeydown = this.keyDown;
	oText.suggest = this;
	oText.onblur = this.hideSuggest;
}

AuthorSuggest.prototype.setSelection = function(pref) {
	var parentDiv = this.oText.parentNode;
	var texts = parentDiv.getElementsByTagName("INPUT");
	if (texts.length >= 3) {
		texts[0].value = pref.personname;
		texts[1].value = pref.affiliation;
		texts[2].value = pref.contact;

		//set the username as the owner
		var ownerField = document.getElementById("DocumentOwner");
		var owners = trim(ownerField.value);
		if (owners == "") owners = pref.username;
		else if (owners.indexOf(pref.username) == -1) {
			owners += ","+pref.username;
		}
		ownerField.value = owners;
		texts[1].focus(); //focus workaround for Chrome
	}
}

AuthorSuggest.prototype.hideSuggest = function() {
	this.suggest.oDiv.style.visibility = "hidden";
}

AuthorSuggest.prototype.keyDown = function(oEvent) {
	oEvent = window.event || oEvent;
	var keyCode = oEvent.keyCode;
	switch (keyCode) {
		case 38: //up arrow
			this.suggest.moveUp();
			break;
		case 40: //down arrow
			this.suggest.moveDown();
			break;
		case 13: //return key
			var cur = this.suggest.cur;
			var div = this.suggest.oDiv;
			var kids = div.childNodes;
			if ( (cur >= 0) && (kids.length > 0) && (cur < kids.length) ) {
				this.suggest.setSelection(kids[cur].pref);
			}
	}
}

AuthorSuggest.prototype.moveDown = function() {
	if ((this.oDiv.childNodes.length > 0) && (this.cur < (this.oDiv.childNodes.length - 1))) {
		this.cur++;
		for (var i=0;i<this.oDiv.childNodes.length;i++) {
			if (i == this.cur) {
				this.oDiv.childNodes[i].className="AuthorSuggestMouseOver";
				this.oText.value=this.oDiv.childNodes[i].innerHTML;
			}
			else {
				this.oDiv.childNodes[i].className="";
			}
		}
	}
}

AuthorSuggest.prototype.moveUp = function() {
	if((this.oDiv.childNodes.length > 0) && (this.cur > 0)) {
		this.cur--;
		for(var i=0; i<this.oDiv.childNodes.length; i++) {
			if (i == this.cur) {
				this.oDiv.childNodes[i].className="AuthorSuggestMouseOver";
				this.oText.value = this.oDiv.childNodes[i].innerHTML;
			}
			else {
				this.oDiv.childNodes[i].className="";
			}
		}
	}
}

AuthorSuggest.prototype.keyUp = function(oEvent) {
	oEvent = oEvent || window.event;
	var keyCode = oEvent.keyCode;
	if ((keyCode == 8) || (keyCode == 46)) {
		this.suggest.onTextChange(false);
	}
	else if (keyCode < 32 || (keyCode >= 33 && keyCode <= 46) || (keyCode >= 112 && keyCode <= 123)) {
        //ignore
    }
	else {
		this.suggest.onTextChange(true);
	}
}

AuthorSuggest.prototype.positionSuggest = function() {
	var pos = findObject(this.oText);
	this.oDiv.style.top = (pos.y + pos.h) + "px";
	this.oDiv.style.left = pos.x + "px";
	this.oDiv.style.width = pos.w;
}

AuthorSuggest.prototype.onTextChange = function() {
	var txt = this.oText.value;
	var oThis = this;
	this.cur = -1;

	if (txt.length > 0) {
		while(this.oDiv.hasChildNodes()) this.oDiv.removeChild(this.oDiv.firstChild);

		var matches = this.db.getMatches(txt, this.maxSize);
		if (!matches.length) { this.hideSuggest; return; }
		this.positionSuggest();

		for(i in matches) {
			var oNew = document.createElement('div');
			this.oDiv.appendChild(oNew);
			oNew.onmouseover =
			oNew.onmouseout =
			oNew.onmousedown = function(oEvent) {
				oEvent = window.event || oEvent;
				oSrcDiv = oEvent.target || oEvent.srcElement;

				if(oEvent.type == "mousedown") {
					oThis.oText.value = this.innerHTML;
					//handle this as a selection.
					oThis.setSelection(this.pref);
				}
				else if(oEvent.type == "mouseover") {
					this.className = "AuthorSuggestMouseOver";
				}
				else if(oEvent.type == "mouseout") {
					this.className = "";
				}
				else {
					this.oText.focus();
				}
			};
			oNew.innerHTML = matches[i].personname;
			oNew.pref = matches[i];
		}
		this.oDiv.style.visibility = "visible";
	}
	else {
		this.oDiv.innerHTML = "";
		this.oDiv.style.visibility = "hidden";
	}
}

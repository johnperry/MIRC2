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
}

PrefsDB.prototype.getMatches = function(s, maxMatches) {
	var count = 0;
	var matches = new Array();
	for (var i=0; i<this.prefs.length; i++) {
		if (this.prefs[i].matches(s)) {
			matches.push(this.prefs[i]);
			count++;
		}
		if(count >= maxSize) break;
	}
	return matches;
}

function Pref(user) {
	this.username = user.getAttribute("username");
	this.personname = user.getAttribute("personname");
	this.personnameLC = this.personname.toLowerCase();
	this.affiliation = user.getAttribute("affiliation");
	this.contact = user.getAttribute("contact");
}

Pref.prototype.matches = function(s) {
	return this.personnameLC.indexOf(s);
}

function AuthorSuggest(db, oText, oDiv, maxSize) {
	this.db = db;
	this.oText = oText;
	this.oDiv = oDiv;
	this.maxSize = maxSize;
	this.cur = -1;

	oText.onkeyup = this.keyUp;
	oText.onkeydown = this.keyDown;
	oText.autoComplete = this;
	oText.onblur = this.hideSuggest;
}

AuthorSuggest.prototype.hideSuggest = function() {
	this.AuthorSuggest.oDiv.style.visibility="hidden";
}

AuthorSuggest.prototype.keyDown = function(oEvent) {
	oEvent = window.event || oEvent;
	var keyCode = oEvent.keyCode;
	switch(keyCode) {
		case 38: //up arrow
			this.AuthorSuggest.moveUp();
			break;
		case 40: //down arrow
			this.AuthorSuggest.moveDown();
			break;
		case 13: //return key
			window.focus();
			break;
	}
}

AuthorSuggest.prototype.moveDown = function() {
	if ((this.oDiv.childNodes.length > 0) && (this.cur < (this.oDiv.childNodes.length - 1))) {
		this.cur++;
		for (var i=0;i<this.oDiv.childNodes.length;i++) {
			if (i == this.cur) {
				this.oDiv.childNodes[i].className="over";
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
				this.oDiv.childNodes[i].className="over";
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
	var keyCode=oEvent.keyCode;
	if ((keyCode == 8) || (keyCode == 46)) {
		this.autoComplete.onTextChange(false);
	}
	else if (keyCode < 32 || (keyCode >= 33 && keyCode <= 46) || (keyCode >= 112 && keyCode <= 123)) {
        //ignore
    }
	else {
		this.autoComplete.onTextChange(true);
	}
}

AuthorSuggest.prototype.positionSuggest = function() {
	var oNode = this.oText;
	var x = 0;
	var y = oNode.offsetHeight;

	while (oNode.offsetParent && oNode.offsetParent.tagName.toUpperCase() != 'BODY') {
		x += oNode.offsetLeft;
		y += oNode.offsetTop;
		oNode = oNode.offsetParent;
	}

	x += oNode.offsetLeft;
	y += oNode.offsetTop;

	this.oDiv.style.top = y + "px";
	this.oDiv.style.left = x + "px";
}

AuthorSuggest.prototype.onTextChange = function() {
	var txt = this.oText.value;
	var oThis = this;
	this.cur = -1;

	if(txt.length>0) {
		while(this.oDiv.hasChildNodes()) this.oDiv.removeChild(this.oDiv.firstChild);

		var matches = this.db.getMatches(txt, this.maxSize);
		if (!matches.length) { this.hideSuggest ;return }
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
				}
				else if(oEvent.type == "mouseover") {
					this.className = "over";
				}
				else if(oEvent.type == "mouseout") {
					this.className = "";
				}
				else {
					this.oText.focus();
				}
			};
			oNew.innerHTML = matches[i].personname;
		}

		this.oDiv.style.visibility = "visible";
	}
	else {
		this.oDiv.innerHTML = "";
		this.oDiv.style.visibility = "hidden";
	}
}

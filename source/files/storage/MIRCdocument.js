//This section provides multi-browser support for certain
//commonly required functions.

//Get the height of the window.
function getHeight() {
	return (IE) ?
		document.body.clientHeight : window.innerHeight - 10;
}

//Get the width of the window.
function getWidth() {
	return (IE) ?
		document.body.clientWidth : window.innerWidth;
}

//Get the displayed width of an object
function getObjectWidth(obj) {
	return obj.offsetWidth;
}

//Get the displayed height of an object
function getObjectHeight(obj) {
	return obj.offsetHeight;
}

//Open a url
function openURL(url,target) {
	window.open(url,target);
}
//end of the multi-browser functions

function copy(event) {
	if (IE) {
		var source = getSource(getEvent(event));
		var textarea = document.createElement("TEXTAREA");
		textarea.innerText = source.innerText;
		var copy = textarea.createTextRange();
		copy.execCommand("Copy");
	}
}

window.onresize = window_resize;
window.onload = mirc_onload;
document.onkeydown = keyDown;
document.onkeyup = keyUp;

var horizontalSplit = null;
var verticalSplit = null;

function mirc_onload() {
	setBackground();
	if ((display == "mstf") || (display == "mirctf") || (display == "tab")) {
		initTabs();
		if (imageSection) loadImage(1);
		setIFrameSrc(currentPage);
		var pos = findObject(document.body);
		var leftWidth = pos.w / 4;
		if (display == 'tab')  leftWidth = 90;
		horizontalSplit = new HorizontalSplit("leftside", "divider", "rightside", true,
												leftWidth, true,
												1, 1, splitHandler);
		if (document.getElementById("sldiv")) {
			var leftside = document.getElementById("leftside");
			var h = parseInt(leftside.style.height);
			verticalSplit = new VerticalSplit("uldiv", "sldiv", "lldiv", 2*h/3, 1, 1);
		}
	}
	setWheelDriver();
	window.focus();
	if (imageSection) IMAGES.load();
}

function splitHandler() {
	setSize();
	displayImage();
}

function setBackground() {
	if (background == "light") {
		document.body.style.color = "black";
		document.body.style.background = "white";
	}
	else {
		document.body.style.color = "white";
		document.body.style.background = "black";
		document.body.setAttribute("link","white");
	}
}

//save images button handler
function saveImages(fileurl) {
	openURL(fileurl, "_self");
}

//export button zip extension handler
function exportZipFile(url,target, myEvent) {
	if (getEvent(myEvent).altKey) url += "&ext=mrz";
	openURL(url,target);
}

var deleteURL = "";
function deleteDocument(url) {
	deleteURL = url;
	var div = document.getElementById("ediv");
	if (div) div.parentNode.removeChild(div);
	showTextDialog("ediv", 400, 215, "Are you sure?", "/icons/closebox.gif", "Delete?",
		"Are you sure you want to delete this document from the server?",
		deleteDocumentHandler, hidePopups);
}

function deleteDocumentHandler() {
	hidePopups();
	var req = new AJAX();
	req.GET(deleteURL, req.timeStamp(), null);
	if (req.success()) {
		alert("The document was deleted.");
		history.go(-2);
	}
	else alert("The attempt to delete the files failed.");
}

//quiz answer event handler (toggle response)
function toggleResponse(divid) {
	var x = document.getElementById(divid);
	if (x.style.display == 'block') {
		x.style.display = 'none';
		x.style.visibility = 'hidden';
		if (!document.all) window.resizeBy(0,1);
	} else {
		x.style.display = 'block';
		x.style.visibility = 'visible';
		if (!document.all) window.resizeBy(0,-1);
	}
}

//jump button event handler for text-captions
function jumpButton(inc, myEvent) {
	var source = getSource(getEvent(myEvent));
	var buttons = source.parentElement.getElementsByTagName("input");
	var i;
	for (i=0; i<buttons.length; i++) {
		if (buttons[i] === source) {
			var b = i + inc;
			if ((b >= 0) && (b < buttons.length)) buttons[b].scrollIntoView(false);
			return;
		}
	}
}

//show button event handler for text-captions
function showButton(myEvent) {
	var source = getSource(getEvent(myEvent));
	var captionDiv = getPreviousNamedSibling(source,"div");
	if (captionDiv.className == "hide") {
		captionDiv.className="show";
    	if (inputType == "image")
			source.src = "/mirc/buttons/hidecaption.png";
		else if (inputType == "button")
			source.value = "Hide Caption";
	} else {
		captionDiv.className="hide";
    	if (inputType == "image")
			source.src = "/mirc/buttons/showcaption.png";
		else if (inputType == "button")
			source.value = "Show Caption";
	}
	source.scrollIntoView(false);
}

function getPreviousNamedSibling(start,name) {
	var prev = start.previousSibling;
	while ((prev != null) && (prev.tagName.toLowerCase() != name))
		prev = prev.previousSibling;
	return prev;
}

//initialize the tabs
function initTabs() {
	currentPage = document.getElementById('tab'+firstTab);
	currentButton = document.getElementById('b'+firstTab);
	if (currentPage == null) {
		currentPage = document.getElementById('tab1');
		currentButton = document.getElementById('b1');
	}
	currentPage.className = "show";
	currentButton.className = "s";
	if (showEmptyTabs != "yes") {
		var n = 2;
		var done = false;
		while (!done) {
			var b = document.getElementById("b"+n);
			if (b != null) {
				if (n != firstTab) {
					var d = document.getElementById("tab"+n);
					if (!isImageSection(d) && !containsIFrame(d)) {
						//get the text
						var text = getText(d);
						var trimmedText = trim(text);
						//count the nodes
						var count = 0;
						for (var q=0; q<d.childNodes.length; q++) {
							if (d.childNodes[q].nodeType == 1) count++;
						}
						if ((trimmedText == "") || (count < 2)) {
							b.style.display = 'none';
							b.style.visibility = 'hidden';
						}
					}
				}
				n += 1;
			}
			else done = true;
		}
	}
}

function isImageSection(d) {
	for (var q=0; q<d.childNodes.length; q++) {
		var c = d.childNodes[q];
		if (c.nodeType == 1) {
			if (c.id == "rightside") return true;
		}
	}
	return false;
}

function containsIFrame(d) {
	var iframes = d.getElementsByTagName("IFRAME");
	return (iframes.length > 0);
}

function getText(d) {
	if (d.nodeType == 3) return d.nodeValue;
	if (d.nodeType == 1) {
		var tn = d.tagName.toLowerCase();
		if (tn == "img") return "x";
		if ((tn != "h1") && (tn != "h2") && (tn != "h3")) {
			var text = "";
			for (var q=0; q<d.childNodes.length; q++) {
				text += getText(d.childNodes[q]);
			}
			return text;
		}
	}
	return "";
}

//tab button event handler
var currentPage;
var currentButton;
function bclick(b,np) {
	currentButton.className = "u";
	currentButton = document.getElementById(b);
	currentButton.className = "s";
	var nextPage = document.getElementById(np);
	if (currentPage != nextPage) {
		currentPage.className="hide";
		currentPage = nextPage;
		currentPage.className="show";
		setIFrameSrc(currentPage);
		if (display == "tab") {
			var rightside = document.getElementById("rightside");
			if (rightside != null) {
				var imagetab = rightside.parentNode.id;
				var maindiv = document.getElementById("maindiv");
				if (np == imagetab) {
					maindiv.style.overflow = "hidden";
					horizontalSplit.positionSlider();
				}
				else
					maindiv.style.overflow = "auto";
			}
		}
	}
}

//set the src attribute of all the iframe children in an element.
//(this is just to avoid the error message from IE when an
//iframe is loaded in a non-visible tab and the iframe tries
//to grab the focus.)
function setIFrameSrc(el) {
	var iframes = el.getElementsByTagName("IFRAME");
	for (var i=0; i<iframes.length; i++) {
		var fr = iframes[i];
		var src = fr.getAttribute("src");
		var longdesc = fr.getAttribute("longdesc");
		if ((longdesc) && (longdesc != "")) {
			fr.src = longdesc;
			fr.setAttribute("longdesc", "");
		}
	}
}

function window_resize() {
	if (horizontalSplit) horizontalSplit.positionSlider();
}

//set the sizes for the components of the document
var rightsideWidth = 700;
var leftsideWidth = 256;

function setSize() {
	if ((display == "mstf") || (display == "mirctf") || (display == "tab")) {
		var bodyPos = findObject(document.body);
		var maindiv = document.getElementById('maindiv');
		var maindivPos = findObject(maindiv);
		var maindivHeight = bodyPos.h - maindivPos.y;
		if (maindivHeight < 100) maindivHeight = 100;
		maindiv.style.height = maindivHeight;

		if (imageSection) {
			var leftside = document.getElementById('leftside');
			var uldiv = document.getElementById('uldiv');
			var sldiv = document.getElementById("sldiv");
			var lldiv = document.getElementById('lldiv');
			var rightside = document.getElementById('rightside');
			var rbuttons = document.getElementById('rbuttons');
			var captions = document.getElementById('captions');
			var rimage = document.getElementById('rimage');

			var leftsidePos = findObject(leftside);
			if (leftsidePos.w) {
				if (uldiv) uldiv.style.width = leftsidePos.w;
				if (sldiv) sldiv.style.width = leftsidePos.w;
				if (lldiv) lldiv.style.width = leftsidePos.w;

				if ((uldiv != null) && (lldiv != null)) {
					if (verticalSplit) verticalSplit.positionSlider();
					/*
					var uldivHeight = leftsidePos.h * 2/3;
					uldiv.style.height = uldivHeight;
					lldiv.style.height = leftsidePos.h - uldivHeight;
					*/
				}
				else if ((uldiv == null) && (lldiv != null)) {
					lldiv.style.height = leftsidePos.h - 1;
				}
				else if ((uldiv != null) && (lldiv == null)) {
					uldiv.style.height = leftsidePos.h - 1;
				}

				var rightsidePos = findObject(rightside);
				rbuttons.style.width = rightsidePos.w;
				captions.style.width = rightsidePos.w;
				rimage.style.width = rightsidePos.w - 1;

				var rbuttonsPos = findObject(rbuttons);
				var captionsPos = findObject(captions);
				var rimageTop = rightsidePos.y + rbuttonsPos.h + captionsPos.h;
				var rimageHeight = maindivHeight - rbuttonsPos.h - captionsPos.h - 1;
				rimage.style.height = rimageHeight - 6;
			}
		}
	}
}

//display images in image-sections
//
function loadImage(image) {
	var index = image - 1;
	dehighlightToken();
	IMAGES.setCurrentIMAGESET(index);
	highlightToken();
	displayImage();
}

function loadAnnotation(image) {
	var index = image - 1;
	dehighlightToken();
	IMAGES.setCurrentIMAGESET(index);
	highlightToken();
	if (displayImage(image)) displayAnnotation();
}

//display the next image, if possible
function nextImage() {
	dehighlightToken();
	IMAGES.nextIMAGESET();
	highlightToken();
	displayImage();
}

//display the previous image, if possible
function prevImage() {
	dehighlightToken();
	IMAGES.prevIMAGESET();
	highlightToken();
	displayImage();
}

//dehighlight the current thumbnail
function dehighlightToken() {
	if (IMAGES.hasCurrentIMAGESET()) {
		var llc = document.getElementById("lldiv");
		if (llc) {
			var tokens = llc.getElementsByTagName("INPUT");
			tokens[IMAGES.currentIndex].className = "tokenbuttonDESEL";
		}
	}
}

function highlightToken() {
	if (IMAGES.hasCurrentIMAGESET()) {
		var llc = document.getElementById("lldiv");
		if (llc) {
			var tokens = llc.getElementsByTagName("INPUT");
			tokens[IMAGES.currentIndex].className = "tokenbuttonSEL";
		}
	}
}

function displayImage() {
	if (IMAGES.hasCurrentIMAGESET()) {
		var place = document.getElementById('rimagecenter');
		while (place.firstChild != null) place.removeChild(place.firstChild);
		var imageSet = IMAGES.currentIMAGESET;

		setCaptions(imageSet);
		setSize();

		var image = imageSet.pImage;
		if (IMAGES.autozoom && imageSet.osImage) image = imageSet.osImage;
		var img = makeElement(image);
		place.appendChild(img);

		var imagenumber = document.getElementById('imagenumber');
		while (imagenumber.firstChild != null) imagenumber.removeChild(imagenumber.firstChild);
		imagenumber.appendChild(document.createTextNode("Image: " + (IMAGES.currentIndex+1)));


		if (imageSet.hasAnnotation()) enableButton('annbtn','inline');
		else disableButton('annbtn','none');

		if (imageSet.osImage) enableButton('orgbtn','inline');
		else disableButton('orgbtn','none');

		if (imageSet.ofImage) enableButton('dcmbtn','inline');
		else disableButton('dcmbtn','none');

		if (!IMAGES.firstIsCurrent()) enableButton('previmg','inline');
		else disableButton('previmg','inline');

		if (!IMAGES.lastIsCurrent()) enableButton('nextimg','inline');
		else disableButton('nextimg','inline');

		imageSet.annotationDisplayed = false;

		return true;
	}
	return false;
}

function setCaptions(imageSet) {
	var capdiv = document.getElementById('captions');
	while (capdiv.firstChild != null) capdiv.removeChild(capdiv.firstChild);

	if (!imageSet.hasCaption()) {
		capdiv.style.display = "none";
	}
	else {
		capdiv.style.display = "block";
		if (imageSet.hasACaption()) {
			setCaption(capdiv, imageSet.aCaption, false);
		}
		if (imageSet.hasCCaption()) {
			if (imageSet.cFlag) setCaption(capdiv, imageSet.cCaption, false);
			else setCaption(capdiv, 'more...' ,true);
		}
	}
}

function setCaption(parent, caption, clickable) {
	var p = document.createElement("P");
	p.style.marginTop = 0;
	var t = document.createTextNode(caption);
	if (clickable) {
		p.onclick = showClickableCaption;
		var u = document.createElement("U");
		u.appendChild(t);
		p.appendChild(u);
	}
	else p.appendChild(t);
	parent.appendChild(p);
}

function showClickableCaption() {
	if (IMAGES.hasCurrentIMAGESET()) {
		var imageSet = IMAGES.currentIMAGESET;
		if (imageSet.hasCCaption()) {
			var capdiv = document.getElementById('captions');
			var pList = capdiv.getElementsByTagName("P");
			capdiv.removeChild(pList[pList.length-1]);
			setCaption(capdiv, imageSet.cCaption, false);
			imageSet.cFlag = true;
		}
	}
}

function disableButton(id, display) {
	var b = document.getElementById(id)
	b.disabled = true;
	b.style.backgroundColor = 'gray';
	b.style.fontWeight = 'bold';
	b.style.visibility = 'hidden';
	b.style.display = display;
}

function enableButton(id, display) {
	var b = document.getElementById(id)
	b.disabled = false;
	b.style.backgroundColor = 'dodgerblue';
	b.style.color = 'white';
	b.style.fontWeight = 'bold';
	b.style.visibility = 'visible';
	b.style.display = display;
}

function displayAnnotation() {
	if (IMAGES.hasCurrentIMAGESET()) {
		var imageSet = IMAGES.currentIMAGESET;
		if (imageSet.annotationDisplayed) displayImage();
		else {
			//Check the aImage and sImage values to determine
			//whether this is an SVG annotation or an image
			//with burned-in annotations. Give precedence to
			//the burned-in image because it loads faster and
			//doesn't require the SVG viewer.
			var place = document.getElementById('rimagecenter');
			if (imageSet.hasAnnotation()) {
				if (imageSet.aImage && (imageSet.aImage.src != "")) {
					//Image
					while (place.firstChild != null) place.removeChild(place.firstChild);
					place.appendChild(makeElement(imageSet.aImage));
					imageSet.annotationIsSVG = false;
					imageSet.annotationDisplayed = true;
				}
				else if (imageSet.sImage && (imageSet.sImage.src != "")) {
					//SVG
					//Get the size of the image currently displayed.
					var img = place.getElementsByTagName("IMG")[0];
					if (img == null) return;
					var width = getObjectWidth(img);
					var height = getObjectHeight(img);

					//Embed the SVG file
					var svgEmbedID = "svgEmbedID";
					var embed = document.createElement("EMBED");
					embed.setAttribute("id",svgEmbedID);
					embed.setAttribute("src", imageSet.sImage.src);
					embed.setAttribute("width",width);
					embed.setAttribute("height",height);
					embed.setAttribute("type","image/svg+xml");
					while (place.firstChild != null) place.removeChild(place.firstChild);
					place.appendChild(embed);

					//Set up the root element.
					//Get the href for the base image.
					var href = imageSet.pImage.src;
					configureSVGRoot(svgEmbedID, href);
					imageSet.annotationIsSVG = true;
					imageSet.annotationDisplayed = true;
				}
			}
		}
	}
}

function configureSVGRoot(id, href) {
	try {
		var embed = document.getElementById(id);
		var width = embed.getAttribute("width");
		var height = embed.getAttribute("height");
		var svgdoc = embed.getSVGDocument();

		var root = svgdoc.rootElement;
		root.setAttribute("width", width);
		root.setAttribute("height", height);
		var viewBox = "0 0 " + width + " " + height;
		root.setAttribute("viewBox",viewBox);

		var imageElements = root.getElementsByTagName("image");
		if (imageElements.length == 0) {
			var image = svgdoc.createElementNS("http://www.w3.org/2000/svg", "image");
			image.setAttributeNS("http://www.w3.org/1999/xlink","href", href);
			image.setAttribute("x", 0);
			image.setAttribute("y", 0);
			image.setAttribute("width", width);
			image.setAttribute("height", height);
			if (root.firstChild == null) root.appendChild(image);
			else root.insertBefore(image,root.firstChild);
		}
	}
	catch (ex) {
		setTimeout("configureSVGRoot(\""+id+"\",\""+href+"\");",10);
	}
}

function makeElement(image) {
	var elem = document.createElement("IMG");
	elem.src = image.src;
	if (IMAGES.autozoom) {
		var w = image.w;
		var h = image.h;
		if (w && h) {
			var rimage = document.getElementById("rimage");
			var rimagePos = findObject(rimage);
			var zh = rimagePos.h / h;
			var zw = rimagePos.w / w;
			var z = ((zh > zw) ? zw : zh);
			elem.width = z * w;
			elem.height = z * h;
		}
	}
	return elem;
}

function fetchOriginal() {
	if (IMAGES.hasCurrentIMAGESET()) {
		window.open(IMAGES.currentIMAGESET.osImage.src, "_blank");
	}
}

function fetchModality(myEvent) {
	if (IMAGES.hasCurrentIMAGESET()) {
		var imageSet = IMAGES.currentIMAGESET;
		myEvent = getEvent(myEvent)
		var imagePath = dirPath + "/" + imageSet.ofImage.src;
		if (myEvent.altKey)
			//alt key generates a DICOM dataset dump
			openURL(imagePath+"?dicom","_blank");
		else if (myEvent.ctrlKey)
			//ctrl key downloads the file
			openURL(imagePath,"_self");
		else if (myEvent.shiftKey) {
			//shift key redisplays the current image
			//this removes the DICOM viewer applet, if loaded
			displayImage();
		}
		else {
			displayImage();
			/* //Disable the DICOM viewer for now
			//no modifiers: request the DICOM viewer applet
			var place = document.getElementById('rimagecenter');
			while (place.firstChild != null) place.removeChild(place.firstChild);
			var pos = findObject(document.getElementById('rimage'));
			var rwidth = "" + pos.w;
			var rheight = "" + pos.h;
			var iframe = document.createElement("IFRAME");
			iframe.setAttribute("id", "viewerApplet");
			iframe.setAttribute("name", "viewerApplet");
			iframe.setAttribute("src", imagePath+"?viewer");
			iframe.setAttribute("width", rwidth);
			iframe.setAttribute("height", rheight);
			//now load the iframe
			place.appendChild(iframe);
			*/
		}
	}
}

function addParam(applet, name, value) {
	var param = document.createElement("PARAM");
	param.setAttribute(name,value);
	applet.appendChild(param);
}

//handle a link to a metadata file.
function fetchFile(url, myEvent) {
	if (!getEvent(myEvent).altKey) openURL(url,"_blank");
	else openURL(url+"?dicom", "_blank");
}

//set a case of the day
function setCaseOfTheDay() {
	var qs = "title=" + encodeURIComponent(codTitle)
			+ "&link=" + encodeURIComponent(docPath)
			+ "&image=" + encodeURIComponent(dirPath + "/" +codImage)
			+ "&timestamp=" + new Date().getTime();
	var req = new AJAX();
	req.POST("/qsadmin/setcod", qs, codResult);

	function codResult() {
		if (req.success()) {
			alert("The document was entered as the Case of the Day.");
		}
	}
}

var lastAcceptedWheelTime = 0;
function setWheelDriver() {
	if ((display == "mstf") || (display == "mirctf") || (display == "tab")) {
		var rimage = document.getElementById("rimage");
		if (rimage) {
			if (rimage.addEventListener) {
				//Mozilla
				rimage.addEventListener('DOMMouseScroll', wheel, false);
				rimage.addEventListener("mousewheel", wheel, false);
			}
			else {
				// IE/Opera
				rimage.attachEvent("onmousewheel", wheel);
			}
			//set the handler for the home, end, and arrow keys
			document.onkeydown = keyDown;
			document.onkeyup = keyUp;
		}
	}
}

function wheel(event){
	if (!event) event = window.event;
	var delta = event.detail ? -event.detail : event.wheelDelta/40;
	if (delta < 0) delta = -1;
	if (delta > 0) delta = +1;
	var date = new Date();
	var currentTime = date.getTime();
	//alert("currentTime = "+currentTime+"\nlastAcceptedWheelTime = "+lastAcceptedWheelTime);
	if ((currentTime - lastAcceptedWheelTime) > 75) {
		if (!event.ctrlKey) loadImage(IMAGES.currentIndex + 1 - delta);
		else loadAnnotation(IMAGES.currentIndex + 1 - delta);
		lastAcceptedWheelTime = currentTime;
	}
	if (event.preventDefault) event.preventDefault();
	event.returnValue = false;
}

function keyDown(event) {
	if (!event) event = window.event;
	var nextImage = IMAGES.currentIndex + 1;
	var kc = event.keyCode;
	if (kc == 36) nextImage = 1; //HOME
	else if (kc == 35) nextImage = IMAGES.length(); //END
	else if (kc == 38) nextImage--; //UP ARROW
	else if (kc == 40) nextImage++; //DOWN ARROW

	else if ((kc == 109) || (kc == 189)) { //MINUS
		IMAGES.autozoom = false;
		displayImage();
		return;
	}

	else if ((kc == 107) || (kc == 187)) { //PLUS or EQUAL
		IMAGES.autozoom = true;
		displayImage();
		return;
	}

	else if (kc == 37) { //LEFT ARROW
		if (horizontalSplit) horizontalSplit.moveSlider(-10, displayImage);
		return;
	}
	else if (kc == 39) { //RIGHT ARROW
		if (horizontalSplit) horizontalSplit.moveSlider(+10, displayImage);
		return;
	}
	else if (kc == 33) { //PAGE UP
		if (horizontalSplit) horizontalSplit.moveSliderTo(1, displayImage);
		return;
	}
	else if (kc == 34) { //PAGE DOWN
		var pos = findObject(document.body);
		if (horizontalSplit) horizontalSplit.moveSliderTo(pos.w - imagePaneWidth - 7, displayImage);
		return;
	}

	if (kc != 17) {
		if (!event.ctrlKey) loadImage(nextImage);
		else loadAnnotation(nextImage);
	}
	else {	//annotations on current image
		if (!IMAGES.currentIMAGESET.annotationDisplayed) {
			displayAnnotation();
		}
	}
}
function keyUp(event) {
	if (!event) event = window.event;
	if (event.keyCode == 17) {
		if (IMAGES.currentIMAGESET.annotationDisplayed) displayImage();
	}
}

function exportToMyRsnaFiles(url) {
	url += "&myrsna";
	var req = new AJAX();
	req.GET(url, null, myRsnaResult);

	function myRsnaResult() {
		if (req.success()) {
			alert(req.responseText);
		}
	}
}

function sortImages(url) {
	openURL(url, "_self");
}

function addToConference(url, title, alturl, alttitle, subtitle) {
	var dy = IE ? 0 : 26;
	var dx = 0;
	var div = document.getElementById("treediv");
	if (div == null) {
		var div = document.createElement("DIV");
		div.className = "filetree";
		div.id = "confstree";
		div.appendChild(document.createElement("DIV"));

		showDialog("treediv", 600+dx, 400+dy, "Select Conference", "/icons/closebox.gif", "Select a Conference", div, null, null);

		treeManager =
			new TreeManager(
				"confstree",
				"/confs/tree",
				"/icons/plus.gif",
				"/icons/minus.gif");
		treeManager.load();
		treeManager.display();
		treeManager.expandAll();
	}
	else showPopup("treediv", 600+dx, 400+dy, "Select Cabinet", "/icons/closebox.gif");
}

function showConferenceContents(event) {
	var unkTitle = altTitle;
	if (unkTitle == "") {
		unkTitle = "Unknown";
		if (category != "") unkTitle += " - " + category;
	}
	var source = getSource(getEvent(event));
	currentNode = source.treenode;
	var req = new AJAX();
	var qs = "nodeID="+currentNode.nodeID+
				"&url="+encodeURIComponent(docPath)+
				"&title="+encodeURIComponent(title)+
					"&alturl="+encodeURIComponent(docPath + "?unknown=yes")+
					"&alttitle="+encodeURIComponent(unkTitle)+
						"&subtitle="+encodeURIComponent(author)+
							"&"+req.timeStamp();

	req.GET("/confs/appendAgendaItem", qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		if ((root != null) && (root.tagName == "ok")) {
			alert("The MIRCdocument was added to the conference.");
			hidePopups();
			return;
		}
	}
	alert("Unable to add the MIRCdocument to the conference.");
}

//export to MIRC site handler
function exportTo(url) {
	var div = document.getElementById("exportdiv");
	if (div == null) {
		var div = document.createElement("DIV");
		div.id = "seldest";
		div.appendChild(document.createElement("DIV"));
		var p = document.createElement("P");
		p.className = "center";
		var select = document.createElement("SELECT");
		select.id = "destinationID";
		select.className = "destSelect";
		select.servletURL = url;
		p.appendChild(select);
		div.appendChild(p);
		for (var i=0; i<destinations.length; i++) {
			var option = document.createElement("OPTION");
			option.setAttribute("value", destinations[i].url);
			option.appendChild( document.createTextNode(destinations[i].name) );
			select.appendChild(option);
		}
		showDialog("exportdiv", 400, 200, "Select Destination", "/icons/closebox.gif", "Select a Destination", div, exportHandler, hidePopups);
	}
	else showPopup("exportdiv", 400, 200, "Select Destination", "/icons/closebox.gif");
}

function exportHandler() {
	var req = new AJAX();
	var sel = document.getElementById("destinationID");
	var url = sel.servletURL;
	var value = sel.value;
	url += "&dest=" + encodeURIComponent(value) + "&"+req.timeStamp();
	hidePopups();
	req.GET(url, null, null);
	if (req.success()) {
		alert(req.responseText());
	}
}


var addfileCount = 0;

function showAddPopup() {
	var div = document.getElementById("adddiv");
	if (div != null) div.parentNode.removeChild(div);
	addfileCount = 0;

	div = document.createElement("DIV");
	div.id = "add";
	div.className = "add";
	div.appendChild(document.createElement("DIV"));

	var form = document.createElement("FORM");
	form.id = "addFormID";
	form.action = addurl;
	form.method = "POST";
	form.acceptCharset = "UTF-8";
	form.encoding = "multipart/form-data";
	div.appendChild(form);

	var p = document.createElement("P");
	p.className = "center";
	p.style.color = "black";
	form.appendChild(p);
	var anon = document.createElement("INPUT");
	anon.type = "checkbox";
	anon.name = "anonymize";
	anon.checked = true;
	p.appendChild(anon);
	p.appendChild(document.createTextNode("Anonymize DICOM Objects"));
	p.appendChild( document.createElement("BR") );

	p = document.createElement("P");
	p.className = "center";
	form.appendChild(p);
	var fileinput = document.createElement("INPUT");
	fileinput.className = "file";
	fileinput.onchange = captureFile;
	fileinput.setAttribute( "name", "selectedfile0");
	fileinput.id = "selectedfile0";
	fileinput.type = "file";
	p.appendChild(fileinput);
	p.appendChild( document.createElement("BR") );

	showDialog("adddiv", 600, 300, "Add Images", "/icons/closebox.gif", "Add Images", div, addHandler, hidePopups);
}

function captureFile() {
	var selectedfileElement = document.getElementById("selectedfile" + addfileCount);
	addfileCount++;

	var newInput = selectedfileElement.cloneNode(true);
	newInput.setAttribute( "name", "selectedfile" + addfileCount);
	newInput.id = "selectedfile" + addfileCount;
	newInput.onchange = captureFile;
	newInput.value = "";

	var br = document.createElement("BR");

	selectedfileElement.disabled = true;
	var parent = selectedfileElement.parentNode;
	parent.appendChild(newInput);
	parent.appendChild(br);
	newInput.scrollIntoView(false);
}

function addHandler() {
	//Enable all the non-blank selectedfile elements
	for (var i=0; i<addfileCount; i++) {
		var sf = document.getElementById("selectedfile"+i);
		if (sf != null) {
			var t = sf.value;
			t = t.replace(/^\s+/g,"")	//trim the text
			t = t.replace(/\s+$/g,"");
			if (t != "") sf.disabled = false;
		}
	}
	var form = document.getElementById("addFormID");
	form.target = "_self";
	form.submit();
	hidePopups();
}

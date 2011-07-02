function SVGIsInstalled() {
	if (IE) {
		//this needs to be translated from VB
		//return IsObject(CreateObject("Adobe.SVGCtl"));
		return true;
	}
	else if ((navigator.mimeTypes != null) &&
			(navigator.mimeTypes.length > 0) &&
			(navigator.mimeTypes["image/svg+xml"] != null)) return true;
	return false;
}

function loadSVGEditor(sourceObject,svgSrc,imageSrc) {

	// press the line button by default
	pressButton("svgLine");

	//Save the sourceObject in the main editor so
	//we can return the results of the svg editing
	//session.
	editorSourceObject = sourceObject;

	//Make sure the svgDiv is empty.
	var div = document.getElementById("svgDiv");
	while (div.firstChild != null) div.removeChild(div.firstChild);

	//Swap around the divs to hide the main
	//editor and display the SVG editor.
	swapEditorDivs();

	//Display the image so we can get its size
	var img = document.createElement("IMG");
	img.setAttribute("src", imageSrc);
	div.appendChild(img);

	//Wait for the image to load and then use
	//its size to set the size of the svg editor.
	loadSVG(svgSrc,img);
}

var tempImage;
var tempSvgSrc;
function reloadSVG() {
	loadSVG(tempSvgSrc,tempImage);
}

function loadSVG(svgSrc,image) {
	//Wait for the image to load
	if (!image.complete) {
		tempSvgSrc = svgSrc;
		tempImage = image;
		setTimeout("reloadSVG();",10);
		return;
	}

	//Get the parameters of the image to be annotated
	var width = getObjectWidth(image);
	var height = getObjectHeight(image);

	//The image is loaded and we have its size. Now remove it.
	image.parentNode.removeChild(image);

	//Initialize the variables.
	svgInitialize();

	//Embed an SVG file. There doesn't seem to be a way to
	//construct this programmatically, so this has to be a file
	//in the root of the servlet.
	embedSVG(embedID,svgSrc,width,height);

	//Construct the root element in the SVG document.
	createSVGRoot(embedID,image.src);
}

function swapEditorDivs() {
	//Swap around the editor divs.
	var mainEditorDiv = document.getElementById("mainEditorDiv");
	var svgEditorDiv = document.getElementById("svgEditorDiv");
	var vis = svgEditorDiv.style.visibility;
	if (vis != "visible") {
		mainEditorDiv.style.visibility = "hidden";
		mainEditorDiv.style.display = "none";
		svgEditorDiv.style.visibility = "visible";
		svgEditorDiv.style.display = "block";
	}
	else {
		svgEditorDiv.style.visibility = "hidden";
		svgEditorDiv.style.display = "none";
		mainEditorDiv.style.visibility = "visible";
		mainEditorDiv.style.display = "block";
	}
}

function embedSVG(id,svgSrc,width,height) {
	var svg = document.createElement("EMBED");
	svg.setAttribute("id",id);
	svg.setAttribute("src",svgSrc);
	svg.setAttribute("width",width);
	svg.setAttribute("height",height);
	svg.setAttribute("type","image/svg+xml");
	var div = document.getElementById("svgDiv");
	if (div != null) {
		while (div.firstChild != null) div.removeChild(div.firstChild);
		div.appendChild(svg);
	}
}

function createSVGRoot(id,href) {
	try {
		var embed = document.getElementById(id);
		var width = embed.getAttribute("width");
		var height = embed.getAttribute("height");
		var svgdoc = embed.getSVGDocument();

		var root = svgdoc.rootElement;

		root.setAttribute("width",width);
		root.setAttribute("height",height);

		//test to see if it's IE
		if( document.all ) {
			root.setAttribute("onmousedown","svgMousedown(evt);");
			root.setAttribute("onmousemove","svgMousemove(evt);");
			root.setAttribute("onmouseup","svgMouseup(evt);");
			root.setAttribute("onkeypress","svgKeypressed(evt);");
		} else {
			svgdoc.addEventListener("mousemove", svgMousemove, true);
			svgdoc.addEventListener("mousedown", svgMousedown, true);
			svgdoc.addEventListener("mouseup", svgMouseup, true);
			svgdoc.addEventListener("keypress", svgKeypressed, true);
		}

		var viewBox = "0 0 " + width + " " + height;
		root.setAttribute("viewBox",viewBox);

		var imageElements = root.getElementsByTagName("image");
		if (imageElements.length == 0) {
			var image = svgdoc.createElementNS("http://www.w3.org/2000/svg","image");
			image.setAttribute("id","img");
			image.setAttributeNS("http://www.w3.org/1999/xlink","href",href);
			image.setAttribute("x",0);
			image.setAttribute("y",0);
			image.setAttribute("width",width);
			image.setAttribute("height",height);
			if (root.firstChild == null) root.appendChild(image);
			else root.insertBefore(image,root.firstChild);
		}
	}
	catch (ex) { setTimeout("createSVGRoot(\""+id+"\",\""+href+"\");",10); }
}

function svgInitialize() {
	svgCurrentElement = null;
	svgCurrentMode = "";
	svgCurrentSelectDot = null;
	svgX = 0;
	svgY = 0;
	svgLastX = 0;
	svgLastY = 0;
	svgType = "line";
	stroke = "red";
	strokeWidth = 3;
	font = "serif";
	fontSize = 20;
	fontStyle = "normal";
	fontWeight = "normal";
	arrowSize = 20;
}

function getSVGDocument() {
	var embed = document.getElementById(embedID);
	return embed.getSVGDocument();
}

function getSVGRoot() {
	return getSVGDocument().rootElement;
}


function svgSave(event) {
	if (editorSourceObject == null) return;
	svgDeselectAll(getSVGRoot());
	editorSourceObject.svgText = getSVGText();
	saveClicked();
	swapEditorDivs();
}

//-----------------------------------------------------------------------
// A few debugging routines
function showSize(img) {
	if (img != null) {
		alert("width = "+img.offsetWidth+"\n"+"height = "+img.offsetHeight);
	}
	else alert(id + " is null");
}

function showTree() {
	alert(getTree(getSVGRoot(),""));
}

function showProps() {
	var svgdoc = getSVGDocument();
	var text = "";
	for (name in svgdoc) text += name + "\n";
	alert("svgDocument properties\n\n" + text);
}

function getTree(node,margin) {
	var text = "";
	if (node.nodeType == 1) {
		text += margin + node.tagName + "\n";
		var child = node.firstChild;
		while (child != null) {
			text += getTree(child, margin + "    ");
			child = child.nextSibling;
		}
	}
	return text;
}

function showCurrentElementProps() {
	showElementProps(svgCurrentElement);
}

function showElementProps(element) {
	if (element == null) return;
	var text = "";
	var name;
	for (name in element) {
		if (name == "id")
			text += name + " = " + element.getAttribute(name) + "\n";
		else
			text += name + "\n";
	}
	alert(element.tagName + " properties\n\n" + text);
}
//-----------------------------------------------------------------------

var embedID = "svgEmbed";
var editorSourceObject = null;
var svgCurrentElement = null;
var svgCurrentMode = "";
var svgCurrentSelectDot = null;
var svgX = 0;
var svgY = 0;
var svgLastX = 0;
var svgLastY = 0;
var svgType = "line";
var stroke = "red";
var strokeWidth = 3;
var font = "serif";
var fontSize = 20;
var fontStyle = "normal";
var fontWeight = "normal";
var arrowSize = 20;

// images array of buttons that are popped out
var svgToolButtonArray = new Array();
svgToolButtonArray['svgArrow'] = "svg-buttons/arrow.gif";
svgToolButtonArray['svgSelect'] = "svg-buttons/select.gif";
svgToolButtonArray['svgLine'] = "svg-buttons/line.gif";
svgToolButtonArray['svgCircle'] = "svg-buttons/circle.gif";
svgToolButtonArray['svgText'] = "svg-buttons/text.gif";

// images array of buttons that are pressed in
var svgToolButtonPressedArray = new Array();
svgToolButtonPressedArray['svgArrow'] = "svg-buttons/arrow-pressed.gif";
svgToolButtonPressedArray['svgSelect'] = "svg-buttons/select-pressed.gif";
svgToolButtonPressedArray['svgLine'] = "svg-buttons/line-pressed.gif";
svgToolButtonPressedArray['svgCircle'] = "svg-buttons/circle-pressed.gif";
svgToolButtonPressedArray['svgText'] = "svg-buttons/text-pressed.gif";

// this function will change the graphic of the button who's ID you've passed in to pressed
// and the rest of the buttons to popped out
function pressButton(buttonID) {
	// loop thru all buttons
	for( var i in svgToolButtonArray ) {
		// if the button in the loop matches the buttonID we passed in, press it
		if( buttonID == i ) {
			document.getElementById(i).setAttribute("src", changeImageSource(svgToolButtonPressedArray[i], document.getElementById(i).getAttribute("src")));
		// otherwise, pop it out
		} else {
			document.getElementById(i).setAttribute("src", changeImageSource(svgToolButtonArray[i], document.getElementById(i).getAttribute("src")));		
		}
	}
}

// this is a little screwy, we can't replace the entire source, because the file path differs from 
// site to site depending on the name of the users storage service, so we'll have to leave that part in 
// tact
function changeImageSource (newImage, oldImage) {
	var newSource = "";
	// gotta love javascript....IE urls have an http://server thrown in front of them, so we'll have 
	// to strip that stuff off, but only in IE.
	if( oldImage.indexOf("//") > 0 ) {
		oldImage = oldImage.substring(oldImage.indexOf("//")+2, oldImage.length);
	}
	// old image starts with a leading slash, we don't need to get that
	oldImage = oldImage.substring(oldImage.indexOf("/")+1, oldImage.length);

	newSource ="/" + oldImage.substring(0, oldImage.indexOf("/")+1) + newImage;
	return newSource;
}


function setType(theType) {
	switch (theType) {
		case "select":
			pressButton("svgSelect");
		break;
		case "text":
			pressButton("svgText");
		break;
		case "circle":
			pressButton("svgCircle");
		break;
		case "line":
			pressButton("svgLine");
		break;
		case "arrow":
			pressButton("svgArrow");
		break;
	}
	svgType = theType;
	svgFixText();
	svgDeselectAll(getSVGRoot());
	svgCurrentElement = null;
}

function setStroke(theStroke) {
	stroke = theStroke;
	if (svgCurrentElement != null) {
		svgCurrentElement.setAttribute("stroke",stroke);
		var fill = svgCurrentElement.getAttribute("fill");
		if ((fill != null) && (fill.toLowerCase() != "none"))
			svgCurrentElement.setAttribute("fill",stroke);
	}
}

function setStrokeWidth(inc) {
	strokeWidth += inc;
	strokeWidth = Math.min(strokeWidth,7);
	strokeWidth = Math.max(strokeWidth,1);
	if ((svgCurrentElement != null) && !isText(svgCurrentElement))
		svgCurrentElement.setAttribute("stroke-width",strokeWidth);
}

function setArrowSize(inc) {
	arrowSize += inc * 10;
	arrowSize = Math.min(arrowSize,40);
	arrowSize = Math.max(arrowSize,10);
	if (isArrow(svgCurrentElement))
		drawArrow(svgCurrentElement);
}

function setFont() {
	var selector = document.getElementById("svgFontSelector");
	var options = selector.getElementsByTagName("OPTION");
	font = options[selector.selectedIndex].value;
	if (isText(svgCurrentElement)) {
		svgCurrentElement.setAttribute("font-family",font);
	}
}

function setFontSize(inc) {
	fontSize += inc;
	fontSize = Math.min(fontSize,96);
	fontSize = Math.max(fontSize,12);
	if (isText(svgCurrentElement))
		svgCurrentElement.setAttribute("font-size",fontSize);
}

function setFontStyle() {
	fontStyle = (fontStyle == "normal") ? "italic" : "normal";
	if (isText(svgCurrentElement))
		svgCurrentElement.setAttribute("font-style",fontStyle);
}

function setFontWeight() {
	fontWeight = (fontWeight == "normal") ? "bold" : "normal";
	if (isText(svgCurrentElement))
		svgCurrentElement.setAttribute("font-weight",fontWeight);
}

function isText(w) {
	if (w == null) return false;
	var svgType = w.getAttribute("type");
	return (svgType == "text");
}

function isArrow(w) {
	if (w == null) return false;
	var svgType = w.getAttribute("type");
	return (svgType == "arrow");
}

function setXY(event) {
	svgX = event.clientX;
	svgY = event.clientY;
}

function setLastXY() {
	svgLastX = svgX;
	svgLastY = svgY;
}

function svgAppend(w) {
	getSVGRoot().appendChild(w);
}

function svgBringToFront(w) {
	var svg = getSVGRoot();
	svg.removeChild(w);
	svg.appendChild(w);
}

function svgDeselectAll(node) {
	var count = 0;
	if (node.nodeType == 1 || node.nodeType == "1") {
		var svgType = node.getAttribute("type");
		//if (svgType == null) return;
		if (svgType == "select") {
			node.parentNode.removeChild(node);
		}
		else if (svgType == "text") {
			deselectText(node);
		}
		else {
			var child = node.firstChild;
			while (child != null) {
				var nextChild = child.nextSibling;
				svgDeselectAll(child);
				child = nextChild;
			}
		}
	}
}

function svgKeypressed(evt) {
	if (svgCurrentElement != null) {
		var svgType = svgCurrentElement.getAttribute("type");
		if (svgType == "text") {
			var child = svgCurrentElement.firstChild;
			if (child != null) {
				var text = child.nodeValue;
				if ((text.length > 0) && (text.charAt(text.length-1) == "|"))
					text = text.substring(0,text.length-1);
				var code = evt.charCode;
				if (code < 32) {
					if ((code == 8) && (text.length > 0)) {
						text = text.substring(0,text.length-1) + "|";
					}
					else return;
				}
				else {
					text += String.fromCharCode(code) + "|";
				}
				svgCurrentElement.removeChild(child);
				var newText = getSVGDocument().createTextNode(text);
				svgCurrentElement.appendChild(newText);
			}
		}
		else if (evt.charCode == 8) svgCut();
	}
}

function svgCut() {
	if (svgCurrentElement != null) {
		var svg = getSVGRoot();
		svg.removeChild(svgCurrentElement);
		svgCurrentElement = null;
		svgDeselectAll(getSVGRoot());
	}
}

function svgFixText() {
	if ((svgCurrentElement != null) && (svgCurrentElement.getAttribute("type") == "text")) {
		if (isEmptyText(svgCurrentElement)) svgCut();
		else deselectText(svgCurrentElement);
	}
}

function svgMousedown(evt) {
	if (evt.ctrlKey || evt.altKey || evt.metaKey) return;
	var target = evt.target;
	var svgDocument = getSVGDocument();
	var svg = svgDocument.rootElement;
	var img = svgDocument.getElementById("img");
	svgX = evt.clientX;
	svgY = evt.clientY;
	setLastXY();
	svgDeselectAll(getSVGRoot());
	svgFixText();
	if ((target === svg) || (target === img)) {
		svgCurrentElement = newElement();
		svgCurrentMode = "new";
	} else if (target.getAttribute("type") == "select") {
		svgCurrentMode = "resize";
		svgCurrentSelectDot = target.getAttribute("dotID");
	} else {
		svgCurrentElement = target;
		svgBringToFront(target);
		svgCurrentMode = "drag";
	}
}

function svgMousemove(evt) {
	if (evt.ctrlKey || evt.altKey || evt.metaKey) return;
//	setXY(evt);
	svgX = evt.clientX;
	svgY = evt.clientY;

	if (svgCurrentElement != null) {
		switch (svgCurrentMode) {
			case "new":
				extendElement(svgCurrentElement); break;
			case "drag":
				dragElement(svgCurrentElement); break;
			case "resize":
				resizeElement(svgCurrentElement); break;
		}
	}
}

function svgMouseup(evt) {
	svgCurrentMode = "";
	if (svgCurrentElement != null) {
		if (isEmpty(svgCurrentElement)) svgCut();
		else selectElement();
	}
}

function newElement() {
	switch (svgType) {
		case "line": return newLine(svgX,svgY,stroke,strokeWidth);
		case "circle": return newCircle(svgX,svgY,0,stroke,strokeWidth);
		case "arrow": return newArrow(svgX,svgY,stroke,strokeWidth);
		case "text": return newText(svgX,svgY,stroke,strokeWidth);
		default: return null;
	}
	return null;
}

function extendElement() {
	var svgType = svgCurrentElement.getAttribute("type");
	switch (svgType) {
		case "line": return extendLine(svgCurrentElement);
		case "circle": return extendCircle(svgCurrentElement);
		case "arrow": return extendArrow(svgCurrentElement);
		case "text": return extendText(svgCurrentElement);
	}
}

function dragElement() {
	var svgType = svgCurrentElement.getAttribute("type");
	switch (svgType) {
		case "line": return dragLine(svgCurrentElement);
		case "circle": return dragCircle(svgCurrentElement);
		case "arrow": return dragArrow(svgCurrentElement);
		case "text": return dragText(svgCurrentElement);
	}
}

function resizeElement() {
	var svgType = svgCurrentElement.getAttribute("type");
	switch (svgType) {
		case "line": return resizeLine(svgCurrentElement);
		case "circle": return resizeCircle(svgCurrentElement);
		case "arrow": return resizeArrow(svgCurrentElement);
	}
	return false;
}

function selectElement() {
	var svgType = svgCurrentElement.getAttribute("type");
	switch (svgType) {
		case "line": return selectLine(svgCurrentElement);
		case "circle": return selectCircle(svgCurrentElement);
		case "arrow": return selectArrow(svgCurrentElement);
		case "text": return selectText(svgCurrentElement);
	}
}

function isEmpty() {
	var svgType = svgCurrentElement.getAttribute("type");
	switch (svgType) {
		case "line": return isEmptyLine(svgCurrentElement);
		case "circle": return isEmptyCircle(svgCurrentElement);
		case "arrow": return isEmptyArrow(svgCurrentElement);
	}
	return false;
}

function newLine(svgX,svgY,stroke,strokeWidth) {
	var svgDocument = getSVGDocument();
	var w = svgDocument.createElementNS("http://www.w3.org/2000/svg", "line");
	w.setAttribute("type","line");
	w.setAttribute("x1",svgX);
	w.setAttribute("y1",svgY);
	w.setAttribute("x2",svgX);
	w.setAttribute("y2",svgY);
	w.setAttribute("stroke",stroke);
	w.setAttribute("stroke-width",strokeWidth);
	svgAppend(w);
	return w;
}

function extendLine(w) {
	w.setAttribute("x2",svgX);
	w.setAttribute("y2",svgY);
}

function resizeLine(w) {
	switch (svgCurrentSelectDot) {
		case "p1":
			w.setAttribute("x1",svgX);
			w.setAttribute("y1",svgY);
			break;
		case "p2":
			w.setAttribute("x2",svgX);
			w.setAttribute("y2",svgY);
			break;
	}
}

function dragLine(w) {
	var dx = svgX - svgLastX;
	var dy = svgY - svgLastY;
	var x1 = Number(w.getAttribute("x1")) + dx;
	var y1 = Number(w.getAttribute("y1")) + dy;
	var x2 = Number(w.getAttribute("x2")) + dx;
	var y2 = Number(w.getAttribute("y2")) + dy;
	w.setAttribute("x1",x1);
	w.setAttribute("y1",y1);
	w.setAttribute("x2",x2);
	w.setAttribute("y2",y2);
	setLastXY();
}

function selectLine(w) {
	var x1 = Number(w.getAttribute("x1"));
	var y1 = Number(w.getAttribute("y1"));
	selectDot(x1,y1,"p1");
	var x2 = Number(w.getAttribute("x2"));
	var y2 = Number(w.getAttribute("y2"));
	selectDot(x2,y2,"p2");
}

function isEmptyLine(w) {
	var x1 = Number(w.getAttribute("x1"));
	var y1 = Number(w.getAttribute("y1"));
	var x2 = Number(w.getAttribute("x2"));
	var y2 = Number(w.getAttribute("y2"));
	return (x1 == x2) && (y1 == y2);
}

function selectDot(svgX,svgY,dotID) {
	var svgDocument = getSVGDocument();
	var w = svgDocument.createElementNS("http://www.w3.org/2000/svg", "circle");
	w.setAttribute("type","select");
	w.setAttribute("cx",svgX);
	w.setAttribute("cy",svgY);
	w.setAttribute("r",2.5);
	w.setAttribute("stroke","yellow");
	w.setAttribute("stroke-width",1);
	w.setAttribute("fill","yellow");
	w.setAttribute("dotID",dotID);
	svgAppend(w);
}

function newCircle(cx,cy,r,stroke,strokeWidth) {
	var svgDocument = getSVGDocument();
	var w = svgDocument.createElementNS("http://www.w3.org/2000/svg", "circle");
	w.setAttribute("type","circle");
	w.setAttribute("cx",cx);
	w.setAttribute("cy",cy);
	w.setAttribute("r",r);
	w.setAttribute("stroke",stroke);
	w.setAttribute("stroke-width",strokeWidth);
	w.setAttribute("fill","none");
	svgAppend(w);
	return w;
}

function extendCircle(w) {
	var r = Math.sqrt(Math.pow(svgX-svgLastX,2) + Math.pow(svgY-svgLastY,2));
	w.setAttribute("r",r);
}

function resizeCircle(w) {
	var cx = Number(w.getAttribute("cx"));
	var cy = Number(w.getAttribute("cy"));
	var r = Math.sqrt(Math.pow(svgX-cx,2) + Math.pow(svgY-cy,2));
	w.setAttribute("r",r);
}

function dragCircle(w) {
	var cx = Number(w.getAttribute("cx"));
	var cy = Number(w.getAttribute("cy"));
	cx = cx + svgX - svgLastX;
	cy = cy + svgY - svgLastY;
	w.setAttribute("cx",cx);
	w.setAttribute("cy",cy);
	setLastXY();
}

function selectCircle(w) {
	var cx = Number(w.getAttribute("cx"));
	var cy = Number(w.getAttribute("cy"));
	var r = Number(w.getAttribute("r"));
	selectDot(cx+r,cy,"right");
	selectDot(cx-r,cy,"left");
	selectDot(cx,cy+r,"top");
	selectDot(cx,cy-r,"bottom");
}

function isEmptyCircle(w) {
	var r = Number(w.getAttribute("r"));
	return (r == 0);
}

function newArrow(svgX,svgY,stroke,strokeWidth) {
	var svgDocument = getSVGDocument();
	var w = svgDocument.createElementNS("http://www.w3.org/2000/svg", "polygon");
	w.setAttribute("type","arrow");
	var points = getPoints(svgX,svgY,svgX,svgY);
	w.setAttribute("points",points);
	w.setAttribute("stroke",stroke);
	w.setAttribute("stroke-width",strokeWidth);
	w.setAttribute("fill",stroke);
	w.setAttribute("x1",svgX);
	w.setAttribute("y1",svgY);
	w.setAttribute("x2",svgX);
	w.setAttribute("y2",svgY);
	svgAppend(w);
	return w;
}

function extendArrow(w) {
	var x1 = Number(w.getAttribute("x1"));
	var y1 = Number(w.getAttribute("y1"));
	var points = getPoints(x1,y1,svgX,svgY);
	w.setAttribute("points",points);
	w.setAttribute("x2",svgX);
	w.setAttribute("y2",svgY);
	setLastXY();
}

function resizeArrow(w) {
	resizeLine(w);
	drawArrow(w);
}

function drawArrow(w) {
	var x1 = Number(w.getAttribute("x1"));
	var y1 = Number(w.getAttribute("y1"));
	var x2 = Number(w.getAttribute("x2"));
	var y2 = Number(w.getAttribute("y2"));
	var points = getPoints(x1,y1,x2,y2);
	w.setAttribute("points",points);
}

function dragArrow(w) {
	var dx = svgX - svgLastX;
	var dy = svgY - svgLastY;
	var x1 = Number(w.getAttribute("x1")) + dx;
	var y1 = Number(w.getAttribute("y1")) + dy;
	var x2 = Number(w.getAttribute("x2")) + dx;
	var y2 = Number(w.getAttribute("y2")) + dy;
	w.setAttribute("x1",x1);
	w.setAttribute("y1",y1);
	w.setAttribute("x2",x2);
	w.setAttribute("y2",y2);
	var points = getPoints(x1,y1,x2,y2);
	w.setAttribute("points",points);
	setLastXY();
}

function getPoints(x1,y1,x2,y2) {
	var points = "";
	if ((x1 != x2) || (y1 != y2)) {
		var arrowWidth = 3 * arrowSize / 10;
		var ux = x1 - x2;
		var uy = y1 - y2;
		var len = Math.sqrt(ux*ux + uy*uy);
		ux = ux/len;
		uy = uy/len;
		vx = arrowWidth * uy;
		vy = -arrowWidth * ux;
		xp = x2 + arrowSize * ux;
		yp = y2 + arrowSize * uy;
		points += addPoint(x1,y1);
		points += addPoint(xp,yp);
		points += addPoint(xp + vx, yp + vy);
		points += addPoint(x2,y2);
		points += addPoint(xp - vx, yp - vy);
		points += addPoint(xp,yp);
		points += addPoint(x1,y1);
	}
	return points;
}

function addPoint(svgX,svgY) {
	return " " + svgX + " " + svgY;
}

function selectArrow(w) {
	selectLine(w);
}

function isEmptyArrow(w) {
	return isEmptyLine(w);
}

function newText(svgX,svgY,stroke,strokeWidth) {
	var svgDocument = getSVGDocument();
	var w = svgDocument.createElementNS("http://www.w3.org/2000/svg", "text");
	w.setAttribute("type","text");
	w.setAttribute("font-family",font);
	w.setAttribute("font-size",fontSize);
	w.setAttribute("font-weight",fontWeight);
	w.setAttribute("font-style",fontStyle);
	w.setAttribute("stroke",stroke);
	w.setAttribute("stroke-width",1);
	w.setAttribute("fill",stroke);
	w.setAttribute("x",svgX);
	w.setAttribute("y",svgY);
	var t = svgDocument.createTextNode("|");
	w.appendChild(t);
	svgAppend(w);
	return w;
}

function extendText(w) {
}

function dragText(w) {
	var dx = svgX - svgLastX;
	var dy = svgY - svgLastY;
	var xx = Number(w.getAttribute("x")) + dx;
	var yy = Number(w.getAttribute("y")) + dy;
	w.setAttribute("x",xx);
	w.setAttribute("y",yy);
	setLastXY();
}

function selectText(w) {
	var child = w.firstChild;
	if (child != null) {
		var text = child.nodeValue;
		if ((text.length > 0) && (text.charAt(text.length-1) == "|")) return;
		text += "|";
		w.removeChild(child);
		var newText = getSVGDocument().createTextNode(text);
		w.appendChild(newText);
	}
}

function deselectText(w) {
	var child = w.firstChild;
	if (child != null) {
		var text = child.nodeValue;
		if ((text.length > 0) && (text.charAt(text.length-1) == "|")) {
			text = text.substring(0,text.length-1);
			w.removeChild(child);
			var newText = getSVGDocument().createTextNode(text);
			w.appendChild(newText);
		}
	}
}

function isEmptyText(w) {
	var child = w.firstChild;
	if (child == null) return true;
	var text = child.nodeValue;
	if (text.length == 0) return true;
	if (text == "|") return true;
	return false;
}

//-------------------------------------------------------------
// Get the text of the entire SVG object
function getSVGText() {
	var text = svgGetNodeText(getSVGRoot());
	return text;
}

function svgGetNodeText(node) {
	switch (node.nodeType) {
		case 1: //ELEMENT_NODE:
			//Handle the image element separately.
			if (node.nodeName == "image") {
				var imageText = "<image id=\"img\"";
				var href = node.getAttributeNS("http://www.w3.org/1999/xlink","href");
				//Force href to be a local reference
				var k = href.lastIndexOf("/") + 1;
				href = href.substring(k);
				imageText += " xlink:href=\"" + href + "\"";
				imageText += " x=\"0\" y=\"0\"";
				imageText += " width=\"" + node.getAttribute("width") + "\"";
				imageText += " height=\"" + node.getAttribute("height") + "\"/>";
				return imageText;
			}

			var text = "<" + node.nodeName;
			//Suppress the attributes of the svg element.

			if (node.nodeName == "svg") {
				//Add the svg attributes that we need.
				text += " xmlns=\"http://www.w3.org/2000/svg\" ";
				text += "xmlns:xlink=\"http://www.w3.org/1999/xlink\" ";
				text += "viewBox=\"" + node.getAttribute("viewBox") + "\" "
				text += "height=\"" + node.getAttribute("height") + "\" "
				text += "width=\"" + node.getAttribute("width") + "\""
			}  else {
				var attrs = node.attributes;
				for (var i=0; i<attrs.length; i++) {
					var attr = attrs.item(i);
					text += svgGetNodeText(attr);
				}
			}
			var child = node.firstChild;
			if (child == null) text += "/>";
			else {
				text += ">";
				while (child != null) {
					text += svgGetNodeText(child);
					child = child.nextSibling;
				}
				text += "</" + node.nodeName + ">";
			}
			return text;
		case 2: //ATTRIBUTE_NODE:
			return " " + node.nodeName + "=\"" + node.nodeValue + "\"";
		case 3: //TEXT_NODE:
			if (node.nodeValue.replace( /\s+/g, "" ) == "") return "";
			return node.nodeValue;
	}
	return "";
}

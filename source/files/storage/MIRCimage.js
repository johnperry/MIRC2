//Object encapsulating an array of IMAGESETs for display in an image-section
function IMAGESECTION() {
	this.currentIMAGESET = null;
	this.currentIndex = -1;
	this.autozoom = true;
	this.IMAGESETs = new Array();
}

IMAGESECTION.prototype.addIMAGESET = function(set) {
	this.IMAGESETs[this.IMAGESETs.length] = set;
	if (this.currentIndex == -1) {
		this.currentIndex = 0;
		this.currentIMAGESET = set;
	}
}

IMAGESECTION.prototype.nextIMAGESET = function() {
	if (this.currentIndex + 1 < this.IMAGESETs.length) {
		this.currentIndex++;
		this.currentIMAGESET = this.IMAGESETs[this.currentIndex];
	}
	return this.currentIMAGESET;
}

IMAGESECTION.prototype.prevIMAGESET = function() {
	if (this.currentIndex > 0) {
		this.currentIndex--;
		this.currentIMAGESET = this.IMAGESETs[this.currentIndex];
	}
	return this.currentIMAGESET;
}

IMAGESECTION.prototype.setCurrentIMAGESET = function(index) {
	if ((index >= 0) && (index < this.IMAGESETs.length)) {
		this.currentIndex = index;
		this.currentIMAGESET = this.IMAGESETs[this.currentIndex];
	}
	return this.currentIMAGESET;
}

IMAGESECTION.prototype.firstIMAGESET = function() {
	return this.setCurrentIMAGESET(0);
}

IMAGESECTION.prototype.lastIMAGESET = function() {
	return this.setCurrentIMAGESET(this.IMAGESETs.length - 1);
}

IMAGESECTION.prototype.getIMAGESET = function(index) {
	if ((index >= 0) && (index < this.IMAGESETs.length)) {
		return this.IMAGESETs[index];
	}
	return null;
}

IMAGESECTION.prototype.length = function() {
	return this.IMAGESETs.length;
}

IMAGESECTION.prototype.hasCurrentIMAGESET = function() {
	return (this.currentIndex != -1);
}

IMAGESECTION.prototype.firstIsCurrent = function() {
	return (this.currentIndex == 0);
}

IMAGESECTION.prototype.lastIsCurrent = function() {
	return (this.currentIndex == (this.IMAGESETs.length - 1));
}

IMAGESECTION.prototype.toString = function() {
	var t = "IMAGESECTION:\n";
	t += "    currentIMAGESET is "+(this.currentIMAGESET ? "not " : "")+"null\n";
	t += "    currentIndex = "+this.currentIndex+"\n";
	t += "    autozoom = "+this.autozoom+"\n";
	for (var i=0; i<this.IMAGESETs.length; i++) {
		t += this.IMAGESETs[i].toString(i);
	}
	return t;
}

IMAGESECTION.prototype.load = function() {
	var req = new AJAX();
	var nextToLoad = 0;
	var thisObject = this;
	var pause = 10;

	getImage();

	function getImage() {
		var pImage = null;
		while ((nextToLoad < thisObject.IMAGESETs.length)
					&& !(pImage = thisObject.IMAGESETs[nextToLoad].pImage)) nextToLoad++;
		if (pImage) {
			nextToLoad += 1;
			req.GET(pImage.src, null, getNextImage);
			setStatusLine("P[" + (nextToLoad-1) + "]: " + pImage.src);
		}
		else {
			nextToLoad = 0;
			getOSImage();
		}
	}
	function getNextImage() {
		if (req.success()) {
			window.setTimeout(getImage, pause);
		}
	}
	function getOSImage() {
		var osImage = null;
		while ((nextToLoad < thisObject.IMAGESETs.length)
					&& !(osImage = thisObject.IMAGESETs[nextToLoad].osImage)) nextToLoad++;
		if (osImage) {
			nextToLoad += 1;
			req.GET(osImage.src, null, getNextOSImage);
			setStatusLine("OS[" + (nextToLoad-1) + "]: " + osImage.src);
		}
		else {
			window.setTimeout(clearStatusLine, 5000);
		}
	}
	function getNextOSImage() {
		if (req.success()) {
			window.setTimeout(getOSImage, pause);
		}
	}
	function clearStatusLine() {
		setStatusLine(" ");
	}
}

//Object describing a set of related images
//pImage: the base image (to fit in right pane)
//sImage: the SVG annotation XML file
//aImage: the displayable (JPEG) annotated image
//osImage: the displayable image of the original size
//ofImage: the original format image
//aCaption: the text of a caption to be displayed with the image (a = always)
//cCaption: the text of a caption to be displayed after clicking on a link under the aCaption
//cFlag: true if the clickable caption has been shown
//
function IMAGESET() {
	this.pImage = null;
	this.sImage = null;
	this.aImage = null;
	this.osImage = null;
	this.ofImage = null;
	this.aCaption = "";
	this.cCaption = "";
	this.cFlag = false;
	this.annotationDisplayed = false;
	this.annotationIsSVG = false;
}

IMAGESET.prototype.addIMAGE = function(type, src, w, h) {
	if (src && (src != "")) {
		var w = (w ? w : 0);
		var h = (h ? h : 0);
		this[type] = new IMAGE(src, w, h);
	}
}

IMAGESET.prototype.addCAPTION = function(type, caption) {
	if (caption) {
		this[type] = caption;
	}
}

IMAGESET.prototype.hasAnnotation = function() {
	return (this.sImage || this.aImage);
}

IMAGESET.prototype.hasCaption = function() {
	return (this.hasACaption() || this.hasCCaption());
}

IMAGESET.prototype.hasACaption = function() {
	return (this.aCaption != "");
}

IMAGESET.prototype.hasCCaption = function() {
	return (this.cCaption != "");
}

IMAGESET.prototype.toString = function(n) {
	var margin = "     ";
	var s = "IMAGESET" + ((n!=null) ? "["+n+"]" : "") + ":\n";
	if (this.pImage != null) s += margin + "pImage: " + this.pImage.toString();
	if (this.sImage != null) s += margin + "sImage: " + this.sImage.toString();
	if (this.aImage != null) s += margin + "aImage: " + this.aImage.toString();
	if (this.osImage != null) s += margin + "osImage: " + this.osImage.toString();
	if (this.ofImage != null) s += margin + "ofImage: " + this.osImage.toString();
	if (this.aCaption != "") s += margin + "aCaption: ["+this.aCaption+"]\n";
	if (this.cCaption != "") s += margin + "cCaption: ["+this.cCaption+"]\n";
	s += margin + "cFlag: ["+this.cFlag+"]\n";
	s += margin + "-----------\n";
	return s;
}

//Object describing a single image in an IMAGESET
//src: the URL of the image
//w: the width of the image (in the file)
//h: the height of the image (in the file)
function IMAGE(src, w, h) {
	this.src = src;
	this.w = w;
	this.h = h;
}

IMAGE.prototype.toString = function() {
	return "["+this.src+"; "+this.w+"; "+this.h+"]\n";
}

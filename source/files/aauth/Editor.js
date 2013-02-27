//Get the height of the window.
function getHeight() {
	return document.body.clientHeight - 2;
}

//Get the width of the window.
function getWidth() {
	return document.body.clientWidth - 8;
}

//************** end of the multi-browser functions **************

//This code sets the height of the editor and palette
//spans so that they get their own scrollbars. Even though the
//getHeight and getWidth functions attempt to get the height and
//width of the window in the browser's coordinate system, there is
//no guarantee that it works beyond IE.

function setSize() {
	var editor = document.getElementById("editor");
	var palette = document.getElementById("palette");
	var cabinetdiv = document.getElementById('cabinetdiv');
	var filesdiv = document.getElementById("filesdiv");
	var captiondiv = document.getElementById("captionEditorDiv");
	if (captiondiv != null) captiondiv.style.height = getHeight();

	var height = getHeight() - getOffsetTop(editor);
	editor.style.height = height;
	cabinetdiv.style.height = 25;
	palette.style.height = height;
	filesdiv.style.height = height - 25;

	var paletteWidth = 117;
	var editorWidth = getWidth() - paletteWidth;
	if (editorWidth < 25) editorWidth = 25;

	editor.style.width = editorWidth;
	palette.style.width = paletteWidth;
}
window.onresize = setSize;

//************** end of the height-setting code **************

//These variables set the border-colors of selected and unselected objects.
//Note: these colors must correspond to the values in the editor-styles.cs file.
var unselColor = "#eeeeee";
var selColor = "red";

//These variables define the state of the editor.
var currentSection = -1;			//The index of the section that is being edited
var lastFileClicked = -1;			//The last object clicked in the palette.
var lastSectionImageClicked = -1;	//The last image clicked in the image-section.

var lastSave = "";
var closeboxURL = "/mirc/images/closebox.gif";

//When the document is loaded, set the size and show the active section.
function loaded() {
	setSize();
	defaultStatus = "";

	var form = document.getElementById("author-service-form");
	var activetab = Number(form.activetab.value);
	if (activetab >= 0) showSection(activetab);
	else showSection(1);

	//setShowHideIcon();
	//setHideableElements(document.body);
	showAll(); //override the show/hide parameter
	setForDisplayMode();
	setFirstTabOptions();
	setToolEnables();

	//Set up to catch the window closing.
	window.onbeforeunload = confirmUnload;
	lastSave = getDocumentText();
}
window.onload = loaded;

//Handle the clicking of a button in the tab div.
function tabClicked(myEvent) {
	var source = getSource(getEvent(myEvent));
	var tabs = document.getElementById("tabs").getElementsByTagName("INPUT");
	var clicked;
	//Find the index of the clicked section button.
	for (clicked=0;
			clicked<tabs.length && tabs.item(clicked)!==source;
				clicked++) ;  //empty loop on purpose
	//and show the corresponding section.
	if (clicked < tabs.length) showSection(clicked);
	else alert("The section finder failed.");
}

//Display a section
function showSection(sectionNumber) {
	var tabs, section;
	tabs = document.getElementById("tabs").getElementsByTagName("INPUT");
	//Hide the current section and deselect its tab
	section = getSection(currentSection);
	if (section != null) {
		section.style.visibility = "hidden";
		section.style.display = "none";
		deselectTab(tabs.item(currentSection));
	}
	//Show the new section and select its tab
	currentSection = sectionNumber;
	section = getSection(currentSection);
	if (section != null) {
		section.style.visibility = "visible";
		section.style.display = "block";
		selectTab(tabs.item(currentSection));
	}
	setToolEnables();
}

//Find a numbered section in the editor. This method
//examines the siblings sequentially to allow for
//skipping intervening text nodes between the section divs.
function getSection(sectionNumber) {
	if (sectionNumber < 0) return null;
	var count = 0;
	var child = document.getElementById("editor").firstChild;
	while ((child != null) && (count <= sectionNumber)) {
		if ((child.nodeName.toLowerCase() == "div") && (child.getAttribute("type") != null)) {
			//This must be a section
			if (count == sectionNumber) return child;
			else count++;
		}
		child = child.nextSibling;
	}
	return null;
}

//Find the current section element
function getCurrentSection() {
	return getSection(currentSection);
}

//Find a numbered tab.
function getTab(sectionNumber) {
	if (sectionNumber < 0) return null;
	var tabs = document.getElementById("tabs").getElementsByTagName("INPUT");
	return tabs.item(sectionNumber);
}

//Find the current tab element
function getCurrentTab() {
	return getTab(currentSection);
}

//Functions to select and deselect a tab
function selectTab(item) {
	item.className = "selectedTab";
}
function deselectTab(item) {
	item.className = "deselectedTab";
}

//Show/hide the image-section depending on the display mode
function setForDisplayMode() {
	var modeSelector = document.getElementById("display");
	var mode = modeSelector.value;
	var show = (mode.toLowerCase() != "page") && (mode != "");
	var visibility = show ? "visible" : "hidden";
	var display = show ? "inline" : "none";
	var tabs = document.getElementById("tabs").getElementsByTagName("INPUT");
	for (var i=0; i<tabs.length; i++) {
		var section = getSection(i);
		var type = section.getAttribute("type");
		if ((type != null) && (type == "image-section")) {
			tabs[i].style.visibility = visibility;
			tabs[i].style.display = display;
		}
	}
}

//Set up the first-tab select element based on the titles of the sections
function setFirstTabOptions() {
	var modeSelector = document.getElementById("display");
	var mode = modeSelector.value;
	var page = (mode.toLowerCase() == "page");
	var select = document.getElementById("first-tab");
	//If there are no options, use the value of firstTabAttribute
	//as the starting point; otherwise, use the current value of
	//the element.
	var options = select.getElementsByTagName("OPTION");
	var value = (options.length == 0) ? firstTabAttribute : select.value;
	if (value == "") value = 2;
	else value = Number(value);
	//Get rid of the old options
	for (var i=options.length-1; i>=0; i--) select.removeChild(options[i]);
	//Make new ones based on the headings of the sections.
	var type;
	var option;
	var tabNumber = 1;
	var section = document.getElementById("editor").firstChild;
	while (section != null) {
		if ((section.nodeName.toLowerCase() == "div") && ((type=section.getAttribute("type")) != null)) {
			//This must really be a section
			option = document.createElement("OPTION");
			option.setAttribute("value",tabNumber.toString());
			switch (type) {
			case "document-description":
				option.appendChild(document.createTextNode("Document"));
				select.appendChild(option);
				if (value == tabNumber) option.setAttribute("selected","true");
				tabNumber++;
				break;
			case "document-section":
			case "image-section":
			case "references":
				if ((type != "image-section") || !page) {
					var h1s = section.getElementsByTagName("H1");
					option.appendChild(document.createTextNode(getTextValue(h1s[0])));
					select.appendChild(option);
					if (value == tabNumber) option.setAttribute("selected","true");
					tabNumber++;
					break;
				}
			}
		}
		section = section.nextSibling;
	}
	//This is a kludge to keep the element hidden
	var current = currentSection;
	showSection(0);
	showSection(current);
}

//Make a section tab track the heading attribute.
function headingChanged(myEvent) {
	if (currentSection != -1) {
		var source = getSource(getEvent(myEvent));
		var heading = source.value;
		//Don't allow blank headings
		if (heading.length == 0) {
			heading = "Heading";
			source.value = heading;
		}
		//Now set the text in the tab
		var tabs = document.getElementById("tabs").getElementsByTagName("INPUT");
		var input = tabs.item(currentSection);
		input.value = heading;
		//Now find the h1 element in this div so we can change its value.
		//Walk the tree backwards until we get to the div.
		var parent = source;
		while ((parent != null) && (parent.tagName.toLowerCase() != "div")) {
			parent = parent.parentNode;
		}
		//If we found a div, get the h1 element
		if (parent != null) {
			var h1 = parent.getElementsByTagName("H1");
			if (h1.length > 0) {
				var child = h1.item(0).firstChild;
				if (child.nodeType == 3) child.nodeValue = heading;
			}
		}
		//And reset the first tab options
		setFirstTabOptions();
	}
}

//Record the index of the last file clicked in the palette. Handle
//crtl-clicks and shift-clicks. Set the border-colors appropriately.
function fileClicked(myEvent) {
	var source = getSource(getEvent(myEvent));
	var files = source.parentNode.getElementsByTagName("IMG");
	lastFileClicked = handleSelection(source,files,lastFileClicked, myEvent);
	setToolEnables();
}

//Handle a double click of a file in the palette.
function fileDblClicked(theEvent) {
	var theEvent = getEvent(theEvent)
	stopEvent(theEvent);
	var source = getSource(theEvent);
	var url = source.xml.getAttribute("fileURL");
	deselectAll();
	lastFileClicked = -1;
	var path = "/files/"+url;
	window.open(path,"_blank");
	setToolEnables();
}

//Set the tool enables on all the tools
function setToolEnables() {
	if (currentSection == -1) return;
	var section = getSection(currentSection);
	var type = section.getAttribute("type");
	switch (type) {
		case "document-description":
		case "title":
		case "authors":
		case "abstract":
		case "keywords":
		case "permissions":
		case "references":
			setAllSectionTools(false);
			setAllItemTools(false);
			break;
		case "index-elements":
			setAllSectionTools(false);
			setAllItemTools(false);
			setToolEnable("insertpatient-button",true);
			setToolEnable("removeobject-button",isItemSelected(section));
			break;
		case "phi":
			setAllSectionTools(false);
			setAllItemTools(false);
			setToolEnable("removeobject-button",isItemSelected(section));
			break;
		case "document-section":
			var itemIsSelected = isItemSelected(section);
			setToolEnable("removesection-button",true);
			setToolEnable("promotesection-button",!isFirstSection());
			setToolEnable("demotesection-button",!isLastSection());
			setToolEnable("insertparagraph-button",true);
			setToolEnable("insertcaption-button",true);
			setToolEnable("insertimage-button",isPaletteImageSelected());
			setToolEnable("insertiframe-button",true);
			setToolEnable("insertpatient-button",true);
			setToolEnable("insertquiz-button",true);
			setToolEnable("insertscoredquestion-button",true);
			setToolEnable("insertcommentblock-button",true);
			setToolEnable("removeobject-button",itemIsSelected);
			setToolEnable("promoteobject-button",itemIsSelected);
			setToolEnable("demoteobject-button",itemIsSelected);
			break;
		case "image-section":
			var imageIsSelected = isItemSelected(section);
			setToolEnable("removesection-button",true);
			setToolEnable("promotesection-button",!isFirstSection());
			setToolEnable("demotesection-button",!isLastSection());
			setToolEnable("insertparagraph-button",false);
			setToolEnable("insertcaption-button",false);
			setToolEnable("insertimage-button",isPaletteImageSelected());
			setToolEnable("insertpatient-button",false);
			setToolEnable("insertquiz-button",false);
			setToolEnable("insertscoredquestion-button",false);
			setToolEnable("insertcommentblock-button",false);
			setToolEnable("removeobject-button",imageIsSelected);
			setToolEnable("promoteobject-button",imageIsSelected);
			setToolEnable("demoteobject-button",imageIsSelected);
			break;
	}
}

function isPaletteImageSelected() {
	var palette = document.getElementById("palette");
	return isItemSelected(palette);
}

function isItemSelected(item) {
	if (item.nodeType != 1) return false;
	if (isSelected(item)) return true;
	var child = item.firstChild;
	while (child != null) {
		if (isItemSelected(child)) return true;
		child = child.nextSibling;
	}
	return false;
}

function isFirstSection() {
	var section = getSection(currentSection - 1);
	var type = section.getAttribute("type");
	if ((type == "keywords") || (type == "abstract") ||
		(type == "authors") || (type == "document-description")) return true;
	return false;
}

function isLastSection() {
	var section = getSection(currentSection + 1);
	var type = section.getAttribute("type");
	if (type == "references") return true;
	return false;
}

//Set enables on all section tools (except insert section)
function setAllSectionTools(enable) {
	setToolEnable("removesection-button",enable);
	setToolEnable("promotesection-button",enable);
	setToolEnable("demotesection-button",enable);
}

//Set enables on all object (item) tools
function setAllItemTools(enable) {
	setToolEnable("insertparagraph-button",enable);
	setToolEnable("insertcaption-button",enable);
	setToolEnable("insertimage-button",enable);
	setToolEnable("insertiframe-button",enable);
	setToolEnable("insertpatient-button",enable);
	setToolEnable("insertquiz-button",enable);
	setToolEnable("insertscoredquestion-button",enable);
	setToolEnable("insertcommentblock-button",enable);
	setToolEnable("removeobject-button",enable);
	setToolEnable("promoteobject-button",enable);
	setToolEnable("demoteobject-button",enable);
}

//Set the enable/disable for a tool button
function setToolEnable(id,enable) {
	var tool = document.getElementById(id);
	if (tool == null) return;
	var src = tool.getAttribute("src");

	//get the extension of the src attribute
	var k = src.lastIndexOf(".");
	var ext = src.substring(k+1);

	var isDisabled = (src.indexOf("-x.") != -1);
	if (!enable && isDisabled) return;
	if (enable && !isDisabled) return;
	var len = src.length;
	if (enable)
		src = src.substring(0, k-2) + "." + ext;
	else
		src = src.substring(0, k) + "-x." + ext;
	tool.setAttribute("src",src);
}

//Handle the Cabinet button in the palette.
var openpath = "";
var treeManager;
var currentNode;
var currentPath;
function cabinetClicked() {
	var dy = IE ? 0 : 26;
	var dx = 0;
	var div = document.getElementById("treediv");
	if (div == null) {
		var div = document.createElement("DIV");
		div.className = "filetree";
		div.id = "filetree";
		div.appendChild(document.createElement("DIV"));

		showDialog("treediv", 600+dx, 400+dy, "Select Cabinet", closeboxURL, "Select a File Cabinet", div, null, null);

		treeManager =
			new TreeManager(
				"filetree",
				"/files/tree",
				"/icons/plus.gif",
				"/icons/minus.gif");
		treeManager.load("conferences=no");
		treeManager.display();
		treeManager.expandAll();
	}
	else showPopup("treediv", 600+dx, 400+dy, "Select Cabinet", closeboxURL);
}

function showFileDirContents(event) {
	var source = getSource(getEvent(event));
	currentNode = source.treenode;
	showCurrentFileDirContents();
}

function showCurrentFileDirContents() {
	currentPath = currentNode.getPath();
	treeManager.closePaths();
	currentNode.showPath();
	var req = new AJAX();
	req.GET("/files/mirc/"+currentPath, req.timeStamp(), null);
	if (req.success()) {
		var filesdiv = document.getElementById("filesdiv");
		while (filesdiv.firstChild) filesdiv.removeChild(filesdiv.firstChild);
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		while (child) {
			if ((child.nodeType == 1) && (child.tagName == "file")) {
				var img = document.createElement("IMG");
				img.className = "fileImg desel";
				img.onclick = fileClicked;
				img.ondblclick = fileDblClicked;
				img.setAttribute("src", "/files/"+child.getAttribute("iconURL"));
				img.setAttribute("title", child.getAttribute("title"));
				img.setAttribute("source", "["+currentPath+"]"+child.getAttribute("title"));
				img.xml = child;
				filesdiv.appendChild(img);
			}
			child = child.nextSibling;
		}
		lastFileClicked = -1;
	}
	else alert("The attempt to get the directory contents failed.");
	setToolEnables();
}

function showMyRsnaDirContents(event) {
	var source = getSource(getEvent(event));
	currentNode = source.treenode;
	currentPath = currentNode.getPath();
	treeManager.closePaths();
	currentNode.showPath();
	var req = new AJAX();
	req.GET("/file/service/myrsna/folder/"+currentNode.nodeID, req.timeStamp(), null);
	if (req.success()) {
		var filesdiv = document.getElementById("filesdiv");
		while (filesdiv.firstChild) filesdiv.removeChild(filesdiv.firstChild);
		var xml = req.responseXML();
		var root = xml ? xml.firstChild : null;
		var child = root ? root.firstChild : null;
		while (child) {
			if ((child.nodeType == 1) && (child.tagName == "file")) {
				var img = document.createElement("IMG");
				img.className = "fileImg desel";
				img.onclick = fileClicked;
				img.ondblclick = fileDblClicked;
				img.setAttribute("src", child.getAttribute("iconURL"));
				img.setAttribute("title", child.getAttribute("title"));
				img.setAttribute("source", "[myRSNA|"+child.getAttribute("title")+"]"+child.getAttribute("id"));
				img.xml = child;
				filesdiv.appendChild(img);
			}
			child = child.nextSibling;
		}
		lastClicked = -1;
	}
	else alert("The attempt to get the myRSNA folder contents failed.");
	setToolEnables();
}

//Record the index of the last image clicked in the image-section. Handle
//ctrl-clicks and shift-clicks. Set the border-colors appropriately.
//Note: because there is only one lastSectionImageClicked var,
//there can be only one image-section per document.
//This probably is not a problem, but it is something to ponder.
function sectionImageClicked(myEvent) {
	var source = getSource(getEvent(myEvent));
	var files = source.parentNode.getElementsByTagName("IMG");
	lastSectionImageClicked = handleSelection(source,files,lastSectionImageClicked, myEvent);
	setToolEnables();
}

//Handle a double click of an image.
//Swap the editor divs around to show the selected editor.
function sectionImageDblClicked(myEvent) {
	var source = getSource(getEvent(myEvent));
	if (!imageHasBeenSaved(source)) {
		alert("This image has been inserted since\n" +
			  "the document was last saved.\n\n" +
			  "The document must be saved before\n" +
			  "this image can be annotated or captioned.\n\n" +
			  "Please click the Save button and\n" +
			  "then double-click this image again.");
		return;
	}
	var evt = getEvent(myEvent);
	if (evt.ctrlKey || evt.shiftKey) { //allow shiftKey for Macs
		//Open the image for creating captions
		loadCaptionEditor(source);
	}
	else if (evt.altKey) {
		//Open the image for window width and level
		loadWWWLEditor(source);
	}
	else {
		//Open the image for annotation
		runSVGEditor(source);
	}
}

//Set the annotation editor div and handle the button click event.
function startSVGEditor() {
	var div = document.getElementById("imginfo");
	if (div) {
		runSVGEditor(div.source);
	}
}
function runSVGEditor(source) {
	removeImgInfo();
	var imageSrc = dirpath + source.getAttribute("base-image");
	var svgSrc;
	//If this is a new annotation, start with the svg file in
	//the root of the servlet. If this is an existing
	//annotation, start with the one identified by the
	//annotation attribute of the img element.
	var annSrc = source.getAttribute("ansvgsrc");
	if ((annSrc == null) || (annSrc == ""))
		svgSrc = "/aauth/svg-editor.svg"
	else
		svgSrc = dirpath + annSrc;
	loadSVGEditor(source,svgSrc,imageSrc);
}

function imageHasBeenSaved(source) {
	var basesrc = source.getAttribute("base-image");
	if ((basesrc == null) || (basesrc == "")) {
		return false;
	}
	return true;
}

//Handle image selection in either the palette or the image-section.
//Handle ctrl-clicks and shift-clicks. Set the border colors appropriately.
function handleSelection(item,list,lastClick,myEvent) {
	var last = lastClick;
	var evt = getEvent(myEvent);
	var source = getSource(evt);
	//Handle alt-clicks
	if (evt.altKey) {
		var filename = item.getAttribute("title");
		if ((filename.toLowerCase().lastIndexOf(".dcm") == filename.length - 4) ||
			(filename.replace(/[\.\d]/g,"").length == 0)) {
			var path = "/files/"+source.xml.getAttribute("fileURL")+"?list";
			window.open(path,"_blank");
			deselectAll();
			last = -1;
		}
	}
	//Handle ctrl-clicks
	else if (evt.ctrlKey) {
		if (isSelected(item)) deselect(item);
		else select(item);
	}
	//Handle shift-clicks
	else if (evt.shiftKey && (lastClick != -1)) {
		var sel = false;
		var clicked;
		for (var i=0; i<list.length; i++) {
			clicked = (list.item(i) === item);
			if (clicked) last = i;
			if (!sel) {
				if ((i == lastClick) || clicked) sel = true;
				if (sel) select(list.item(i));
				else deselect(list.item(i));
				if ((i == lastClick) && clicked) sel = false;
			}
			else {
				if (sel) select(list.item(i));
				else deselect(list.item(i));
				if ((i == lastClick) || clicked) sel = false;
			}
		}
	}
	//Handle single clicks
	else {
		for (var i=0; i<list.length; i++) {
			if (list.item(i) === item) {
				select(list.item(i));
				last = i;
			}
			else deselect(list.item(i));
		}
	}
	return last;
}

//Functions to determine whether an item is selected,
//and to select or deselect an item,
function isSelected(item) {
	if (item.style.borderColor.indexOf(selColor) != -1) return true;
	return false;
}
function select(item) {
	item.style.borderColor = selColor;
}
function deselect(item) {
	item.style.borderColor = unselColor;
}
function deselectAll() {
	var filesdiv = document.getElementById("filesdiv");
	var imgs = filesdiv.getElementsByTagName("IMG");
	for (var i=0; i<imgs.length; i++) deselect(imgs[i]);
	lastClick = -1;
}

//Swap two nodes without using the
//Microsoft-specific swapNode method.
function swap(first,second) {
	if (second.nextSibling === first) {
		//Protect against the case where the nodes
		//are out of order and next to each other.
		swap(second,first);
		return;
	}
	var parent = first.parentNode;
	var nextSib = second.nextSibling;
	parent.insertBefore(second,first);
	if (nextSib != null) parent.insertBefore(first,nextSib);
	else parent.appendChild(first);
}

//Clone a node and append it to a parent.
function append(node,parent,setClicks) {
	var clone = node.cloneNode(true);
	if (setClicks) {
		clone.onclick = sectionImageClicked;
		clone.ondblclick = sectionImageDblClicked;
	}
	parent.appendChild(clone);
}

//Move an image to the left in the image-section.
function imageMoveLeft(section) {
	var divs = section.getElementsByTagName("DIV");
	var div = divs.item(0);
	var ps = div.getElementsByTagName("P");
	if (ps.length == 0) return;
	var parent = ps.item(0);
	var images = parent.getElementsByTagName("IMG");
	if (images.length == 0) return;
	var prev = null;
	var curr;
	for (i=0; i<images.length; i++) {
		curr = images.item(i);
		if (prev == null) prev = curr;
		if (isSelected(curr) && !isSelected(prev)) swap (prev,curr);
		else prev = curr;
	}
}

//Move an image to the right in the image-section.
function imageMoveRight(section) {
	var divs = section.getElementsByTagName("DIV");
	var div = divs.item(0);
	var ps = div.getElementsByTagName("P");
	if (ps.length == 0) return;
	var parent = ps.item(0);
	var images = parent.getElementsByTagName("IMG");
	if (images.length == 0) return;
	var prev = null;
	var curr;
	for (var i=images.length-1; i>=0; i--) {
		curr = images.item(i);
		if (prev == null) prev = curr;
		if (isSelected(curr) && !isSelected(prev)) swap (curr,prev);
		else prev = curr;
	}
}

//Insert all the selected images from the palette
//into the image-section. Note that this function
//clones and appends any non-img nodes after the last
//img node in order to maintain evenness of spacing,
//in case the page was created with extra blank text
//nodes between the img elements.
function imageInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var div = divs.item(0);
	var ps = div.getElementsByTagName("P");
	if (ps.length == 0) return;
	var parent = ps.item(0);

	//Find the last img element
	var nodes = parent.childNodes;
	var first;
	var last = nodes.length;
	for (first=last-1;
		first>=0 && nodes.item(first).nodeName.toLowerCase() != "img";
		first--) ; //empty loop on purpose
	//Now point to the next node after the last img element
	first++;

	//Now insert the selected img elements from the palette
	var files = document.getElementById("palette").getElementsByTagName("IMG");
	for (var i=0; i<files.length; i++) {
		var img = files.item(i);
		if (isSelected(img) && isImage(img)) {
			append(img,parent,true);
			//Copy in any intervening nodes to maintain spacing
			for (var j=first; j<last; j++)
				append(nodes.item(j),parent,false);
		}
	}
	setToolEnables();
}

//Remove an image from the image-section.
//Note: this function also removes any intervening
//non-img nodes after a deleted img in order to
//maintain spacing.
function imageRemove(section) {
	var divs = section.getElementsByTagName("DIV");
	var div = divs.item(0);
	var ps = div.getElementsByTagName("P");
	if (ps.length == 0) {
		return;
	}
	var parent = ps[0];
	var images = parent.getElementsByTagName("IMG");

	if (images.length == 0) return;
	var nodes = parent.childNodes;
	var lastImg = nodes.length;

	for (var i=nodes.length-1; i>=0; i--) {
		var curr = nodes.item(i);

		if (curr.nodeName.toLowerCase() == "img") {
			if (isSelected(curr)) {
				for (var j=lastImg-1; j>=i; j--)
					parent.removeChild(nodes.item(j));
			}
			lastImg = i;
		}
	}
	setToolEnables();
}

//************************* Toolbar functions *****************************

//Show/hide
function showhideClicked(myEvent) {
	var source = getSource(getEvent(myEvent));
	show = !show;
	setHideableElements(document.body);
	setShowHideIcon();
}

function showAll() {
	show = true;
	setHideableElements(document.body);
	setShowHideIcon();
}

var showTitle = "Show hidden elements";
var hideTitle = "Hide technical elements";
function setShowHideIcon() {
	var toolbar = document.getElementById("toolbar");
	var imgs = toolbar.getElementsByTagName("IMG");
	var context = imgs[0].getAttribute("src");
	context = context.substring(0,context.lastIndexOf("/")+1);
	if (show) {
		imgs[0].setAttribute("src",context+"hide.gif");
		imgs[0].setAttribute("title",hideTitle);
		status = hideTitle;
	}
	else {
		imgs[0].setAttribute("src",context+"show.gif");
		imgs[0].setAttribute("title",showTitle);
		status = showTitle;
	}
}

function setHideableElements(element) {
	if (element.getAttribute("hideable") != null) {
		var visibility = show ? "visible" : "hidden";
		var display = show ? "block" : "none";
		if ((element.tagName.toLowerCase() == "input") && (display == "block"))
			display = "inline";
		element.style.visibility = visibility;
		element.style.display = display;
	}
	var child = element.firstChild;
	while (child != null) {
		if (child.nodeType == 1) setHideableElements(child);
		child = child.nextSibling;
	}
}

//Save the document.
function saveClicked() {
	if (!checkUnsupportedElements()) return;

	//Require that there be a non-blank title
	if (!hasTitle()) {
		//Switch to the Title section
		var n = getTitleSectionNumber();
		if (n >= 0) showSection(n);
		alert("The document must have a title\nbefore it can be saved.");
		return;
	}

	var form = document.getElementById("author-service-form");
	lastSave = getDocumentText();
	form.target = "_self";
	form.doctext.value = lastSave;
	form.preview.value = "false";
	form.activetab.value = currentSection;
	window.onbeforeunload = "";
	form.submit();
}

function getTitleSectionNumber() {
	var child = document.getElementById("editor").firstChild;
	var n = 0;
	while (child != null) {
		if (child.nodeName.toLowerCase() == "div") {
			if (child.getAttribute("type") == 'title') return n;
			else n++;
		}
		child = child.nextSibling;
	}
	return -1;
}

function getTitleSection() {
	var child = document.getElementById("editor").firstChild;
	while (child != null) {
		if ((child.nodeName.toLowerCase() == "div")
			&& (child.getAttribute("type") == 'title')) {
				return child;
		}
		child = child.nextSibling;
	}
	return null;
}

function hasTitle() {
	var titleSection = getTitleSection();
	if (titleSection) {
		var list = titleSection.getElementsByTagName("TEXTAREA");
		var title = filter(normalize(trim(list[0].value)));
		return (title != "");
	}
	return false;
}

function hasTitle() {
	var child = document.getElementById("editor").firstChild;
	while (child != null) {
		if ((child.nodeName.toLowerCase() == "div")
			&& (child.getAttribute("type") == 'title')) {

			var list = child.getElementsByTagName("TEXTAREA");
			var title = filter(normalize(trim(list[0].value)));
			return (title != "");
		}
		child = child.nextSibling;
	}
	return false;
}

//Check that the document has been saved before closing.
function confirmUnload() {
	var currentText = getDocumentText();
	if (currentText == lastSave)
		return;
	else
		return "The document has changed since it was last saved."
}

function previewClicked() {
	if (!checkUnsupportedElements()) return;
	var form = document.getElementById("author-service-form");
	lastSave = getDocumentText();
	var doctext = document.getElementById("doctext");
	doctext.value = lastSave;
	form.target = "preview";
	var preview = document.getElementById("preview");
	preview.value = "true";
	form.submit();
}

function checkUnsupportedElements() {
	if (!insertElementsPresent) return true;
	var text = "This document contains elements that are\n" +
				"not supported by the Author Service editor.\n\n";

	if (insertElementsPresent)
		text += "One or more \"insert\" elements are present. These\n" +
				"elements are used by the DICOM Service to insert new\n" +
				"images into the document. Unless you are certain that\n" +
				"all the images for this study document have been\n" +
				"received, you should not continue.\n\n";

	text += "These elements will be removed from the\n" +
			"document. If you wish to continue, click OK;\n" +
			"otherwise, click Cancel.";


	return confirm(text);
}

//************************* Document text generation code *****************************
//Get the text of the document
function getDocumentText() {
	var text = "";
	var child = document.getElementById("editor").firstChild;
	while (child != null) {
		if ((child.nodeName.toLowerCase() == "div") && (child.getAttribute("type") != null)) {
			//This must be a section
			text += getSectionText(child);
		}
		child = child.nextSibling;
	}
	if (dsPHI != 0) text += "\n<insert-dataset phi=\"yes\"/>\n";
	if (dsNoPHI != 0) text += "\n<insert-dataset phi=\"no\"/>\n";
	text += "</MIRCdocument>";
	return text;
}

//Get the text of a section based on the type
function getSectionText(section) {
	switch (section.getAttribute("type")) {
		case "document-description": return getDocumentDescriptionText(section);
		case "title": return getTitleText(section);
		case "authors": return getAuthorsText(section);
		case "abstract": return getAbstractText(section);
		case "keywords": return getKeywordsText(section);
		case "document-section": return getDocumentSectionText(section);
		case "image-section": return getImageSectionText(section);
		case "references": return getReferencesText(section);
		case "index-elements": return getIndexElementsText(section);
		case "phi": return getPHIText(section);
		case "permissions": return getPermissionsText(section);
	}
	return "";
}

function getDocumentDescriptionText(section) {
	var iList = section.getElementsByTagName("INPUT");
	var sList = section.getElementsByTagName("SELECT");
	var tList = section.getElementsByTagName("TEXTAREA");
	var text = "<MIRCdocument";
	if (iList[0].checked) {
		var display = trim(sList[0].value);
		if (display != "") text += " display=\"" + display + "\"";
		var bg = trim(sList[1].value);
		if (bg != "") text += " background=\"" + bg + "\"";
		var tab = trim(sList[2].value);
		if (tab != "") text += " first-tab=\"" + tab + "\"";
		text += " as-mode=\""+ show + "\"";
	}
	else {
		var docref = trim(iList[2].value);
		if (docref != "") text += " docref=\"" + docref + "\"";
	}
	if (iList[3].checked) {
		text += " pubreq=\"yes\"";
	}
	if (draftpath != "") {
		text += " draftpath=\"" + draftpath + "\"";
	}
	else if (draft == "yes") {
		text += " draftpath=\"" + docpath + "\"";
	}

	text += ">\n";
	var doctype = trim(sList[3].value);
	if (doctype != "") text += "<document-type>" + doctype + "</document-type>\n";
	var category = trim(sList[4].value);
	if (category != "") text += "<category>" + category + "</category>\n";
	var level = trim(sList[5].value);
	if (level != "") text += "  <level>" + level + "</level>\n";
	var language = trim(sList[6].value);
	if (language != "") text += "<language>" + language + "</language>\n";
	var copyright = filter(normalize(trim(tList[0].value)));
	if (copyright != "") text += "<rights>" + copyright + "</rights>\n";
	var pubdate = normalize(trim(getTextValue(document.getElementById("pubdate"))));
	if (pubdate != "") text += "<publication-date>" + pubdate + "</publication-date>\n";
	var creator = filter(normalize(trim(getTextValue(document.getElementById("creator")))));
	if (creator != "") text += "<creator>" + creator + "</creator>\n";
	return text;
}

function getTitleText(section) {
	var text = "";
	var list = section.getElementsByTagName("TEXTAREA");
	var title = filter(normalize(trim(list[0].value)));
	if (title != "")
		text += "<title>" + title + "</title>\n";
	else
		text += "<title>Untitled</title>\n";
	var alttitle = filter(normalize(trim(list[1].value)));
	if (alttitle != "") text += "<alternative-title>" + alttitle + "</alternative-title>\n";
	return text;
}

function getAuthorsText(section) {
	var text = "";
	var list = section.getElementsByTagName("P");
	//skip the first and the last
	for (var i=1; i<list.length - 1; i++) {
		text += getAuthorText(list[i]);
	}
	return text;
}

function getAuthorText(author) {
	var text = "";
	var list = author.getElementsByTagName("INPUT");
	if (list.length == 3) {
		var name = escapeChars(normalize(trim(list[0].value)));
		var affiliation = escapeChars(normalize(trim(list[1].value)));
		var contact = escapeChars(normalize(trim(list[2].value)));
		if (name != "") {
			text += "<author>\n";
			text += "  <name>"+name+"</name>\n";
			if (affiliation != "")
				text += "  <affiliation>"+affiliation+"</affiliation>\n";
			if (contact != "")
				text += "  <contact>"+contact+"</contact>\n";
			text += "</author>\n";
		}
	}
	return text;
}

function getAbstractText(section) {
	var text = "";
	var list = section.getElementsByTagName("TEXTAREA");
	if (normalize(trim(list[0].value)).length != 0) {
		var abs = normalize(trim(makeParagraphs(list[0].value)));
		text = "<abstract>" + abs + "</abstract>\n";
	}
	if (normalize(trim(list[1].value)).length != 0) {
		var altabs = normalize(trim(makeParagraphs(list[1].value)));
		text += "<alternative-abstract>" + altabs + "</alternative-abstract>\n";
	}
	return text;
}

function getKeywordsText(section) {
	var list = section.getElementsByTagName("TEXTAREA");
	var kw = escapeChars(normalize(trim(list[0].value)));
	if (kw != "") return "<keywords>" + kw + "</keywords>\n";
	return "";
}

function getDocumentSectionText(section) {
	var iList = section.getElementsByTagName("INPUT");
	var sList = section.getElementsByTagName("SELECT");
	var dList = section.getElementsByTagName("DIV");
	var pList = dList[0].getElementsByTagName("P");
	var text = "<section";
	var heading = escapeChars(normalize(trim(iList[0].value)));
	var after = escapeChars(normalize(trim(iList[1].value)));
	var visible = normalize(trim(sList[0].value));
	var width = normalize(trim(iList[2].value));
	var minwidth = normalize(trim(iList[3].value));
	text += " heading=\""+heading+"\" visible=\""+visible+"\"";
	if (after != "") text += " after=\""+after+"\"";
	if ((width != "") && (width > 32)) text += " image-width=\""+width+"\"";
	if (minwidth != "") text += " min-width=\""+minwidth+"\"";
	text += ">\n";
	var inCenter = false;
	var itemType;
	var centerType;
	for (var i=0; i<pList.length; i++) {
		itemType = pList[i].getAttribute("item-type");
		centerType = (itemType == "image") || (itemType == "text-caption") || (itemType == "iframe");
		if (!inCenter && centerType) {
			inCenter = true;
			text += "<center>\n";
		}
		else if (inCenter && !centerType) {
			inCenter = false;
			text += "</center>\n";
		}
		text += getItemText(pList[i]);
	}
	if (inCenter) text += "</center>\n";
	text += "</section>\n";
	return text;
}

var cbDisambiguator = 0;

function getItemText(item) {
	var text = "";

	var inputList = item.getElementsByTagName("INPUT");
	var tableList = item.getElementsByTagName("TABLE");
	var textareaList = item.getElementsByTagName("TEXTAREA");
	var selectList = item.getElementsByTagName("SELECT");
	var imgList = item.getElementsByTagName("IMG");

	switch (item.getAttribute("item-type")) {
	case "p":
		return makeParagraphs(textareaList[0].value) + "\n";

	case "image":
		return makeImage(imgList[0]);

	case "text-caption":
		var text = "<text-caption";
		if (inputList[0].checked) text += " display=\"always\"";
		if (inputList[1].checked) text += " show-button=\"yes\"";
		if (inputList[2].checked) text += " jump-buttons=\"yes\"";
		text += ">" + filter(normalize(trim(textareaList[0].value))) + "</text-caption>\n";
		return text;

	case "patient":
		return getPatientText(tableList[0]);

	case "iframe":
		var ifrsrc = escapeChars(trim(inputList[0].value));
		var ifrwidth = trim(inputList[1].value);
		var ifrheight = trim(inputList[2].value);
		var ifrscrolling = selectList[0].value;
		if (ifrsrc == "") return "";
		ifrwidth = (ifrwidth == "") ? "512" : ifrwidth;
		ifrheight = (ifrheight == "") ? "480" : ifrheight;
		var text = "<iframe";
		text += " src=\""+ifrsrc+"\"";
		text += " width=\""+ifrwidth+"\"";
		text += " height=\""+ifrheight+"\"";
		text += " scrolling=\""+ifrscrolling+"\"";
		text += " frameborder=\"0\"";
		text += "></iframe>\n";
		return text;

	case "quiz":
		var text = "<quiz>\n";
		var tatext;
		var className;
		//get the context, if it's there
		var i = 0;
		if (textareaList[0].className == "context") {
			tatext = getTAText(textareaList[0]);
			if (tatext != "")  text += "<quiz-context>"+ tatext +"</quiz-context>\n";
			i++;
		}
		//get the questions
		while ((i=getNext(textareaList,i,"question")) < textareaList.length) {
			tatext = getTAText(textareaList[i]);
			//only process questions with text
			if (tatext != "") {
				text += "<question>\n";
				text += "<question-body>"+tatext+"</question-body>\n";
				text += getAnswers(textareaList,i+1);
				text += "</question>\n";
			}
			i++;
		}
		return text + "</quiz>\n";

	case "scoredquestion":
		var text = "<ScoredQuestion id=\""+item.id+"\">\n";
		if (textareaList.length) {
			text += getTAText(textareaList[0]) + "\n";
		}
		return text + "</ScoredQuestion>\n";

	case "commentblock":
		var title = imgList[0].title;
		if (title == "New Comment Block") {
			title = "CB-" + (new Date()).getTime() + "-" + (cbDisambiguator++);
		}
		return "<threadblock id=\""+title+"\"/>";
	}

	return "";
}

function getAnswers(list,index) {
	var text = "";
	//This function is called with index pointing to the current question.
	//Find the next question.
	var nextQuestion = getNext(list,index+1,"question");
	var i = index;
	while ((i=getNext(list,i,"answer")) < nextQuestion) {
		var tatext = getTAText(list[i]);
		if (tatext != "") {
			text += "<answer>\n";
			text += "<answer-body>"+tatext+"</answer-body>\n";
			if (((i+1) < list.length) && (list[i+1].className == "response")) {
				tatext = getTAText(list[i+1]);
				text += "<response>"+tatext+"</response>\n";
			}
			text += "</answer>\n";
		}
		i++;
	}
	return text;
}

function getNext(list,index,name) {
	for (var i=index; i<list.length; i++) {
		if (list[i].className == name) return i;
	}
	return list.length;
}

function getTAText(textarea) {
	return filter(normalize(trim(makeLists(textarea.value))));
}

function getImageSectionText(section) {
	var inputList = section.getElementsByTagName("INPUT");
	var divList = section.getElementsByTagName("DIV");
	var imgList = divList[0].getElementsByTagName("IMG");
	var text = "<image-section";
	var heading = normalize(trim(inputList[0].value));
	var width = normalize(trim(inputList[1].value));
	var minwidth = normalize(trim(inputList[2].value));
	if (heading != "") text += " heading=\""+heading+"\"";
	if (width != "") text += " image-pane-width=\""+width+"\"";
	if (minwidth != "") text += " min-width=\""+minwidth+"\"";
	if (inputList[3].checked) text += " icons=\"no\"";
	text += ">\n";
	for (var i=0; i<imgList.length; i++) {
		text += makeImage(imgList[i]);
	}
	if (inputList[4].checked) {
		text += "<insert-megasave width=\""+width+"\"";
		if (minwidth != "") text += " min-width=\""+minwidth+"\"";
		text += "/>\n";
	}
	text += "</image-section>\n";
	return text;
}

function makeImage(img) {
	var text = "";
	var ref = img.getAttribute("ref");
	if ((ref != null) && (ref != "")) text = "  <image href=\""+ref+"\">\n";
	var source = img.getAttribute("base-image");
	if (source == null) source = img.getAttribute("source");
	if (source == null) source = img.getAttribute("src");
	text += "  <image src=\""+source+"\""
			+ imageattr(img,"imagewidth","width")
			+ imageattr(img,"imageheight","height")
			+">\n";
	text += altimage(img,"icon");
	text += altimage(img,"annotation");
	text += altimage(img,"original-dimensions");
	text += altimage(img,"original-format");
	text += imagechild(img,"format");
	text += imagechild(img,"compression");
	text += imagechild(img,"modality");
	text += imagecaption(img,"alwayscaption","always");
	text += imagecaption(img,"clickcaption","click");
	text += orderby(img);
	text += "  </image>\n";
	if ((ref != null) && (ref != "")) text += "  </image>\n";
	return text;
}

function imageattr(img,name,attr) {
	var x = img.getAttribute(name);
	if (x == null) return "";
	return " "+attr+"=\""+x+"\"";
}

function imagechild(img,name) {
	var x = img.getAttribute(name);
	if (x == null) return "";
	if (x == "") return "";
	return "    <"+name+">" + x + "</"+name+">\n";
}

function imagecaption(img,name,attr) {
	var x = img.getAttribute(name);
	if (x == null) return "";
	if (x == "") return "";
	x = x.replace(/\\/g,"");
	x = escapeChars(normalize(trim(x.replace( /\"/g, "\\\""))));
	return "    <image-caption display=\""+attr+"\">" + x + "</image-caption>\n";
}

function orderby(img) {
	var study = img.getAttribute("orderby-study");
	var series = img.getAttribute("orderby-series");
	var acquisition = img.getAttribute("orderby-acquisition");
	var instance = img.getAttribute("orderby-instance");
	var date = img.getAttribute("orderby-date");
	if ((study!=null) && (series!=null) && (acquisition!=null) && (instance!=null)) {
		var x = "<order-by";
		x += " study=\"" + study + "\"";
		x += " series=\"" + series + "\"";
		x += " acquisition=\"" + acquisition + "\"";
		x += " instance=\"" + instance + "\"";
		x += " date=\"" + date + "\"";
		x += "/>";
		return x;
	}
	else return "";
}

function altimage(img,role) {
	if (role == "annotation") {
		if (img.svgText != null) {
			return "    <alternative-image role=\"annotation\" type=\"svg\" src=\"\">\n" +
						img.svgText +
				   "    </alternative-image>\n";
		}
		else {
			var an = "";
			var ansvgsrc = img.getAttribute("ansvgsrc");
			if ((ansvgsrc != null) && (ansvgsrc != "")) {
				an += "    <alternative-image role=\"annotation\" type=\"svg\" src=\""+ansvgsrc+"\"/>\n";
			}
			var animagesrc = img.getAttribute("animagesrc");
			if ((animagesrc != null) && (animagesrc != "")) {
				an += "    <alternative-image role=\"annotation\" type=\"image\" src=\""+animagesrc+"\"/>\n";
			}
			return an;
		}
	}
	//It's not an annotation; just process it normally.
	var x = img.getAttribute(role);
	if (x == null) return "";
	if (x == "") return "";
	return "    <alternative-image role=\""+role+"\" src=\""+x+"\"/>\n";
}

function getReferencesText(section) {
	var text = "";
	var list = section.getElementsByTagName("TEXTAREA");
	var found = false;
	for (var i=0; i<list.length; i++) {
		var ref = filter(normalize(trim(list[i].value)));
		if (ref != "") {
			if (!found) text += "<references>\n";
			text += "  <reference>"+ref+"</reference>\n";
			found = true;
		}
	}
	if (found) text += "</references>\n";
	return text;
}

function getPermissionsText(section) {
	var text = "<authorization>\n";
	var list = section.getElementsByTagName("INPUT");
	if (list.length == 13) {
		var xowner = normalize(trim(list[0].value));
		var xread = getPermission(list,1);
		var xupdate = getPermission(list,5);
		var xexport = getPermission(list,9);
		text += "  <owner>"+xowner+"</owner>\n";
		text += "  <read>"+xread+"</read>\n";
		text += "  <update>"+xupdate+"</update>\n";
		text += "  <export>"+xexport+"</export>\n";
	}
	text += "</authorization>\n";
	return text;
}

function getPermission(list,index) {
	if (list[index].checked) return "*";
	if (list[index+1].checked) return "";
	return normalize(trim(list[index+3].value));
}

function getPHIText(section) {
	var text = "";
	var dList = section.getElementsByTagName("DIV");
	if (dList.length > 0) {
		var itemType;
		var pList = dList[0].getElementsByTagName("P");
		var tList;
		for (var i=0; i<pList.length; i++) {
			itemType = pList[i].getAttribute("item-type");
			if ((itemType != null) && (itemType == "phi-study")) {
				tList = pList[i].getElementsByTagName("TABLE");
				if (tList.length > 0) text += getPHIStudyText(tList[0]);
			}
		}
	}
	if (text != "") text = "<phi>\n" + text + "</phi>\n";
	return text;
}

function getPHIStudyText(table) {
	var iList = table.getElementsByTagName("INPUT");
	var studyUID = escapeChars(normalize(trim(iList[0].value)));
	var ptID = escapeChars(normalize(trim(iList[1].value)));
	var ptName = escapeChars(normalize(trim(iList[2].value)));
	var text = "";
	if (studyUID != "") text += "  <si-uid>" + studyUID + "</si-uid>\n";
	if (ptID != "") text += "  <pt-id>" + ptID + "</pt-id>\n";
	if (ptName != "") text += "  <pt-name>" + ptName + "</pt-name>\n";
	if (text != "") text = " <study>\n" + text + " </study>\n";
	return text;
}

function getIndexElementsText(section) {
	var text = "";
	var list = section.getElementsByTagName("TEXTAREA");
	var history = escapeChars(normalize(trim(list[0].value)));
	if (history != "") text += "<history>" + history + "</history>\n";
	var findings = escapeChars(normalize(trim(list[1].value)));
	if (findings != "") text += "<findings>" + findings + "</findings>\n";
	var dx = escapeChars(normalize(trim(list[2].value)));
	if (dx != "") text += "<diagnosis>" + dx + "</diagnosis>\n";
	var ddx = escapeChars(normalize(trim(list[3].value)));
	if (ddx != "") text += "<differential-diagnosis>" + ddx + "</differential-diagnosis>\n";
	var discussion = escapeChars(normalize(trim(list[4].value)));
	if (discussion != "") text += "<discussion>" + discussion + "</discussion>\n";
	var anatomy = escapeChars(normalize(trim(list[5].value)));
	if (anatomy != "") text += "<anatomy>" + anatomy + "</anatomy>\n";
	var pathology = escapeChars(normalize(trim(list[6].value)));
	if (pathology != "") text += "<pathology>" + pathology + "</pathology>\n";
	var organ = escapeChars(normalize(trim(list[7].value)));
	if (organ != "") text += "<organ-system>" + organ + "</organ-system>\n";
	var modality = escapeChars(normalize(trim(list[8].value)));
	if (modality != "") text += "<modality>" + modality + "</modality>\n";
	var code = escapeChars(normalize(trim(list[9].value)));
	if (code != "") text += "<code>" + code + "</code>\n";

	var dList = section.getElementsByTagName("DIV");
	if (dList.length > 0) {
		var itemType;
		var pList = dList[0].getElementsByTagName("P");
		var tList;
		for (var i=0; i<pList.length; i++) {
			itemType = pList[i].getAttribute("item-type");
			if ((itemType != null) && (itemType == "patient")) {
				tList = pList[i].getElementsByTagName("TABLE");
				if (tList.length > 0) text += getPatientText(tList[0]);
			}
		}
	}
	return text;
}

function getPatientText(table) {
	var text = "";
	//figure out what mode this patient is
	var tdList = table.getElementsByTagName("TD");
	var temp = getTextValue(tdList[10]);
	var rad = (temp.indexOf("Race:") != -1);
	var iList = table.getElementsByTagName("INPUT");
	var sList = table.getElementsByTagName("SELECT");
	var x0 = escapeChars(normalize(trim(iList[0].value)));
	var x1 = escapeChars(normalize(trim(iList[1].value)));
	var x2 = escapeChars(normalize(trim(iList[2].value)));
	var x3 = escapeChars(normalize(trim(iList[3].value)));
	var x4 = escapeChars(normalize(trim(iList[4].value)));
	var x5 = escapeChars(normalize(trim(iList[5].value)));
	var x6 = escapeChars(normalize(trim(iList[6].value)));
	var x7 = escapeChars(normalize(trim(sList[0].value)));
	var x8 = (rad ?
				escapeChars(normalize(trim(iList[7].value))) :
				escapeChars(normalize(trim(sList[1].value))));
	var x9 = (rad ? "" : escapeChars(normalize(trim(sList[2].value))));
	var suppress = iList[iList.length-1].checked;
	if (trim(x0 + x1 + x2 + x3 + x4 + x5 + x6 + x7 + x8 + x9) != "") {
		if (suppress) text += "<patient visible=\"phi-restricted\">\n";
		else text += "<patient>\n";
		if (x0 != "") text += "  <pt-name>"+x0+"</pt-name>\n";
		if (x1 != "") text += "  <pt-id>"+x1+"</pt-id>\n";
		if (x2 != "") text += "  <pt-mrn>"+x2+"</pt-mrn>\n";

		if (trim(x3 +x4 + x5 + x6) != "") {
			text += "  <pt-age>\n";
			if (x3 != "") text += "    <years>"+x3+"</years>\n";
			if (x4 != "") text += "    <months>"+x4+"</months>\n";
			if (x5 != "") text += "    <weeks>"+x5+"</weeks>\n";
			if (x6 != "") text += "    <days>"+x6+"</days>\n";
			text += "  </pt-age>\n";
		}

		if (x7 != "") text += "  <pt-sex>"+x7+"</pt-sex>\n";
		if (rad) {
			if (x8 != "") text += "  <pt-race>"+x8+"</pt-race>\n";
		}
		else {
			if (x8 != "") text += "  <pt-species>"+x8+"</pt-species>\n";
			if (x9 != "") text += "  <pt-breed>"+x9+"</pt-breed>\n";
		}
		text += "</patient>\n";
	}
	return text;
}

function trim(text) {
	if (text == null) return "";
	text = text.replace( /^\s+/g, "" );// strip leading
	return text.replace( /\s+$/g, "" );// strip trailing
}

function normalize(text) {
	return text.replace( /\s+/g, " ");
}

//Create paragraph and list elements from a content string
function makeParagraphs(text) {
	var t = text;
	t = t.replace(/^\s+/g,"");					//trim the text
	t = t.replace(/\s+$/g,"");
	t = t.replace(/\r/g,"");					//flush the returns

	t = "<p>\n" + t + "\n</p>";					//wrap the text
	t = t.replace(/\n(\s*\n)+/g,"\n</p><p>");	//split into paragraphs

	t = makeLists(t);

	t = t.replace(/\n(\s*\n)+/g,"\n");			//flush multiple newlines

	t = t.replace(/<p>\s*<\/p>/g,"");			//flush empty paragraphs

	//force at least one paragraph
	//if (t.replace(/\s*/g,"").length == 0) t = "<p></p>\n";

	t = filter(t);								//try to fix any escapable characters
	return t;
}

//Handle list elements for quizzes
function makeLists(text) {
	var t = text;
	t = "\n" + t;
	t = t.replace(/((\n#[^\n]*)+)/g,"\n<ol>$&\n</ol>");	//insert the <ol> elements
	t = t.replace(/((\n\-[^\n]*)+)/g,"\n<ul>$&\n</ul>");//insert the <ul> elements
	t = t.replace(/^[#\-]([^\n]*)/gm,"<li>$1</li>");	//insert the <li> elements
	if (t.charAt(0) == "\n") t = t.substring(1);
	return t;
}

function getTextValue(node) {
	if (node == null) return "";
	if (node.nodeType == 3) return node.nodeValue;
	if (node.nodeType == 1) {
		var text = "";
		var child = node.firstChild;
		while (child != null) {
			text += getTextValue(child);
			child = child.nextSibling;
		}
		return text;
	}
	return "";
}
//******************** End of the text generation code ****************************

function sectionInsertClicked() {
	var section, h1, text, p, table, tbody, tr, td, input, select, option, div;

	//Create a new document section element
	section = document.createElement("DIV");
	section.className = "sectionPage";
	section.setAttribute("type","document-section");

	h1 = document.createElement("H1");
	text = document.createTextNode("Section Heading");
	h1.appendChild(text);
	section.appendChild(h1);

	p = document.createElement("P");
	p.className = "p1";
	text = document.createTextNode(
		"Use the Items toolbar icons to insert, remove, and rearrange content items.");
	p.appendChild(text);
	section.appendChild(p);

	p = document.createElement("P");
	p.className = "p4";
	p.setAttribute("hideable","true");

	table = document.createElement("TABLE");
	table.setAttribute("BORDER","1");
	table.className = "techtable";
	tbody = document.createElement("TBODY");

	tr = document.createElement("TR");
	td = document.createElement("TD");
	text = document.createTextNode("Heading:");
	td.appendChild(text);
	tr.appendChild(td);

	td = document.createElement("TD");
	input = document.createElement("INPUT");
	input.setAttribute("type","text");
	input.className = "w300";
	input.setAttribute("value","Section Heading");
	input.onchange = headingChanged;
	td.appendChild(input);
	tr.appendChild(td);
	tbody.appendChild(tr);

	tr = document.createElement("TR");
	td = document.createElement("TD");
	text = document.createTextNode("Visible:");
	td.appendChild(text);
	tr.appendChild(td);
	td = document.createElement("TD");
	select = document.createElement("SELECT");
	select.className = "w300";
	option = document.createElement("OPTION");
	option.setAttribute("value","yes");
	option.setAttribute("selected","true");
	text = document.createTextNode("Yes");
	option.appendChild(text);
	select.appendChild(option);
	option = document.createElement("OPTION");
	option.setAttribute("value","no");
	text = document.createTextNode("No");
	option.appendChild(text);
	select.appendChild(option);
	option = document.createElement("OPTION");
	option.setAttribute("value","owner");
	text = document.createTextNode("Owner");
	option.appendChild(text);
	select.appendChild(option);
	td.appendChild(select);
	tr.appendChild(td);
	tbody.appendChild(tr);

	tr = document.createElement("TR");
	td = document.createElement("TD");
	text = document.createTextNode("After (YYYYMMDD):");
	td.appendChild(text);
	tr.appendChild(td);

	td = document.createElement("TD");
	input = document.createElement("INPUT");
	input.setAttribute("type","text");
	input.className = "w300";
	td.appendChild(input);
	tr.appendChild(td);
	tbody.appendChild(tr);

	tr = document.createElement("TR");
	td = document.createElement("TD");
	text = document.createTextNode("Maximum image width:");
	td.appendChild(text);
	tr.appendChild(td);

	td = document.createElement("TD");
	input = document.createElement("INPUT");
	input.setAttribute("type","text");
	input.className = "w300";
	td.appendChild(input);
	tr.appendChild(td);
	tbody.appendChild(tr);

	tr = document.createElement("TR");
	td = document.createElement("TD");
	text = document.createTextNode("Minimum image width:");
	td.appendChild(text);
	tr.appendChild(td);

	td = document.createElement("TD");
	input = document.createElement("INPUT");
	input.setAttribute("type","text");
	input.className = "w300";
	td.appendChild(input);
	tr.appendChild(td);
	tbody.appendChild(tr);

	table.appendChild(tbody);
	p.appendChild(table);
	section.appendChild(p);

	div = document.createElement("DIV");
	section.appendChild(div);

	//Now create a new tab
	var tab;
	tab = document.createElement("INPUT")
	tab.setAttribute("type","button");
	tab.className = "deselectedTab";
	tab.setAttribute("value","Section Heading");
	tab.onclick = tabClicked;

	//Now figure out where to put everything.
	//First get the editor div's first child.
	var editor = document.getElementById('editor');
	var node = editor.firstChild;
	//and while we are at it, get the tabs
	var tabs = document.getElementById("tabs");
	var tabNumber = 0;

	//Look through the sections until we find any of these types:
	//  references, index-elements, patient-elements, peer-review
	//and then insert the section and its tab
	while (node != null) {
		if (node.nodeName.toLowerCase() == "div") {
			var type = node.getAttribute("type");
			if (type != null) {
				if ((type == "references") || (type == "index-elements") ||
					(type == "patient-elements") || (type == "peer-review")) {
					editor.insertBefore(section,node);
					tabs.insertBefore(tab,tabs.getElementsByTagName("INPUT").item(tabNumber));
					if (currentSection >= tabNumber) currentSection++;
					setHideableElements(section);
					showSection(tabNumber);
					return;
				}
				tabNumber++;
			}
		}
		node = node.nextSibling;
	}

	//If we get here, we didn't find a place. This should never happen,
	//but if it does, the logical thing to do is to append the section.
	editor.appendChild(section);
	tabs.appendChild(tab);
	setHideableElements(section);
	showSection(tabs.getElementsByTagName("INPUT").length - 1);
}

function sectionRemoveClicked() {
	var tabs, tabInputs, tab, editor, section, type, next;
	section = getCurrentSection();
	if (section == null) return;
	type = section.getAttribute("type");
	if (type == null) return;
	if ((type == "document-section") || (type == "image-section")) {
		if (!confirm("Do you want to delete the current section?")) return;
		//Okay, go ahead and remove the section
		editor = section.parentNode;
		next = section.nextSibling;
		editor.removeChild(section);
		//while next isn't null, and while next isn't a section
		while ((next != null) && ((next.nodeType == 3) || (next.getAttribute("type") == null))) {
			section = next;
			next = next.nextSibling;
			editor.removeChild(section);
		}
		//Now remove the tab.
		tab = getCurrentTab();
		tabs = tab.parentNode;
		next = tab.nextSibling;
		tabs.removeChild(tab);
		while ((next != null) && (next.nodeName.toLowerCase() != "input")) {
			tab = next;
			next = next.nextSibling;
			tabs.removeChild(tab);
		}
		next = currentSection;
		currentSection = -1;
		showSection(next);
		//And reset the first tab options
		setFirstTabOptions();
	}
}

function sectionPromoteClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttribute("type");
	if ((type != "document-section") && (type != "image-section")) return;

	var previous = getSection(currentSection - 1);
	if (previous == null) return;
	var type = previous.getAttribute("type");
	if ((type != "document-section") && (type != "image-section")) return;

	//We now have the current and previous section elements
	//and they are swappable, so swap them.
	swap(previous,section);

	//Now swap the tabs
	section = getCurrentTab();
	previous = getTab(currentSection - 1);
	swap(previous,section);
	//Now just set the currentSection; everything is already displayed correctly.
	currentSection--;
	//And reset the first tab options
	setFirstTabOptions();
}

function sectionDemoteClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttribute("type");
	if ((type != "document-section") && (type != "image-section")) return;

	var next = getSection(currentSection + 1);
	if (next == null) return;
	var type = next.getAttribute("type");
	if ((type != "document-section") && (type != "image-section")) return;

	//We now have the current and next section elements
	//and they are swappable, so swap them.
	swap(section,next);

	//Now swap the tabs
	section = getCurrentTab();
	next = getTab(currentSection + 1);
	swap(section,next);
	//Now just set the currentSection; everything is already displayed correctly.
	currentSection++;
	//And reset the first tab options
	setFirstTabOptions();
}

function objectRemoveClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") objectRemove(section);
	else if (typeValueLC == "index-elements") objectRemove(section);
	else if (typeValueLC == "phi") objectRemove(section);
	else if(typeValueLC == "image-section") imageRemove(section);
	setToolEnables();
}

function objectPromoteClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") objectMoveUp(section);
	else if(typeValueLC == "image-section") imageMoveLeft(section);
}

function objectDemoteClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") objectMoveDown(section);
	else if(typeValueLC == "image-section") imageMoveRight(section);
}

function objectInsertParaClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	if (type.value.toLowerCase() != "document-section") return;
	paragraphObjectInsert(section);
}

function objectInsertImagesClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") imageObjectInsert(section);
	else if(typeValueLC == "image-section") imageInsert(section);
}

function objectInsertQuizClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") quizObjectInsert(section);
}

function objectInsertScoredQuestionClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") scoredQuestionObjectInsert(section);
}

function objectInsertCommentBlockClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") commentBlockObjectInsert(section);
}

function objectInsertPatientClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") patientObjectInsert(section);
	else if (typeValueLC == "index-elements") patientObjectInsert(section);
}

function objectInsertIFrameClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "document-section") iframeObjectInsert(section);
}

function objectInsertPHIStudyClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	var typeValueLC = type.value.toLowerCase();
	if (typeValueLC == "phi") {
		var divs = section.getElementsByTagName("DIV");
		var parent = divs[0];
		var div = document.getElementById("empty-phi-study");
		var pList = div.getElementsByTagName("P");
		var pt = pList[0].cloneNode(true);
		parent.appendChild(pt);
		if (IE) pt.scrollIntoView(false);
	}
}

//This function sets the suggest values for an author.
var authorXML = null;
//todo: author suggest function

//This function is for pages that insert objects at the paragraph level,
//like the author, references, and patient pages. The section page has its
//own add... functions.
function cloneParagraph(myEvent) {
	var source = getSource(getEvent(myEvent));
	var parent = source.parentNode.parentNode;
	var nodes = parent.getElementsByTagName("P");
	var nodeToClone = nodes.item(nodes.length-2);
	var clone = nodeToClone.cloneNode(true);
	var lastNode = nodes.item(nodes.length-1);
	parent.insertBefore(clone,lastNode);
	var list = clone.getElementsByTagName("INPUT");
	var type;
	for (var i=0; i<list.length; i++) {
		type = list[i].getAttribute("type");
		if ((type != null) && (type.toLowerCase() == "text"))
			list[i].value = "";
	}
	return clone;
}

// function never actually used??
//Deselect all patient table child nodes.
/*
function deselectChildTables(myEvent) {
	var source = getSource(getEvent(myEvent));
	var parent = source.parentNode.parentNode;
	var nodes = parent.getElementsByTagName("P");
	for (var i=0; i<nodes.length; i++) {
		var child = nodes.item(i).firstChild;
		if (child.tagName == "TABLE") deselect(child);
	}
}
*/

function objectInsertCaptionClicked() {
	var section = getCurrentSection();
	if (section == null) return;
	var type = section.getAttributeNode("type");
	if (type == null) return;
	if (type.value.toLowerCase() != "document-section") return;
	captionObjectInsert(section);
}


//This code handles the section element.
//These variables indicate which section is active.
var currentDiv = null;
var currentParagraph = null;
var currentObject = null;

//Catch selection clicks on objects on section
//pages, handle the border-colors to indicate
//selection, and set the current variables.
function setCurrentObject(myEvent) {
	if (currentObject != null) deselect(currentObject);
	//get the object generating the event
	currentObject = getSource(getEvent(myEvent));
	//handle objects in tables
	var parent = currentObject.parentNode;
	if (parent.tagName == "TD") {
		while (currentObject.tagName != "TABLE") {
			currentObject = currentObject.parentNode;
		}
	}
	//get the paragraph that contains the current object
	currentParagraph = currentObject.parentNode;
	//get the div holding the paragraph
	currentDiv = currentParagraph.parentNode;
	//and select the current object
	select(currentObject);
	setToolEnables();
}

//Insert a new paragraph object on a section page.
function paragraphObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	var newP = document.createElement("P");
	newP.className = "p5";
	newP.setAttribute("item-type","p");
	var newTextArea = document.createElement("TEXTAREA");
	newTextArea.className = "sectionP";
	newTextArea.onclick = setCurrentObject;
	newP.appendChild(newTextArea);
	parent.appendChild(newP);
	newP.scrollIntoView(false);
}

function captionObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	var newCaption = document.createElement("P");
	newCaption.className = "p5";
	newCaption.setAttribute("item-type","text-caption");
	var table = document.createElement("TABLE");
	table.className = "captiontable";
	var tablebody = document.createElement("TBODY");
	tablebody.appendChild(createCheckboxRow("break","Force a break, even if blank"));
	tablebody.appendChild(createCheckboxRow("showhide","Include Show/Hide buttons"));
	tablebody.appendChild(createCheckboxRow("jump","Include Jump buttons"));
	tablebody.appendChild(createTextAreaRow());
	table.appendChild(tablebody);
	newCaption.appendChild(table);
	parent.appendChild(newCaption);
	if (IE) newCaption.scrollIntoView(false);
}

function iframeObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	var newIFrame = document.createElement("P");
	newIFrame.className = "p5";
	newIFrame.setAttribute("item-type","iframe");
	var table = document.createElement("TABLE");
	table.className = "techtable";
	var tablebody = document.createElement("TBODY");

	var tr = document.createElement("TR");
	var td = document.createElement("TD")
	td.colSpan = 2;
	td.style.textAlign = "center";
	td.appendChild(document.createTextNode("External Web Page Frame"));
	tr.appendChild(td);
	tablebody.appendChild(tr);

	tablebody.appendChild(createInputTextRow("External Page URL:", "http://mirc.rsna.org"));
	tablebody.appendChild(createInputTextRow("Frame width:", "640"));
	tablebody.appendChild(createInputTextRow("Frame height:", "480"));
	tablebody.appendChild(createYesNoRow("Enable scrolling"));
	table.appendChild(tablebody);
	newIFrame.appendChild(table);
	parent.appendChild(newIFrame);
	if (newIFrame.scrollIntoView) newIFrame.scrollIntoView(false);
}

function patientObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	var div = document.getElementById("empty-patient");
	var pList = div.getElementsByTagName("P");
	var pt = pList[0].cloneNode(true);
	parent.appendChild(pt);
	if (IE) pt.scrollIntoView(false);
}

function commentBlockObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	var newP = document.createElement("P");
	newP.className = "p5";
	newP.setAttribute("item-type","commentblock");
	var img = document.createElement("IMG");
	img.title = "New Comment Block";
	img.className = "commentblockImg";
	img.src = "/aauth/buttons/commentblock.png";
	img.onclick = setCurrentObject;
	newP.appendChild(img);
	parent.appendChild(newP);
	newP.scrollIntoView(false);
}

function scoredQuestionObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];

	var newSQ = document.createElement("P");
	newSQ.className = "p4";
	newSQ.setAttribute("item-type","scoredquestion");
	newSQ.id = (new Date()).getTime();
	var label = document.createElement("SPAN");
	label.className = "s6";
	label.appendChild(document.createTextNode("Scored Question: "));
	newSQ.appendChild(label);
	var newTextArea = document.createElement("TEXTAREA");
	newTextArea.className = "sectionP";
	newTextArea.onclick = setCurrentObject;
	newSQ.appendChild(newTextArea);
	parent.appendChild(newSQ);
	newSQ.scrollIntoView(false);
}

function quizObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	var newQuiz = document.createElement("P");
	newQuiz.className = "p4";
	newQuiz.setAttribute("item-type","quiz");
	var table = document.createElement("TABLE");
	table.className = "quiz";
	var tablebody = document.createElement("TBODY");
	appendEmptyQuizContext(tablebody);
	appendEmptyQuestion(tablebody);
	appendQuestionButton(tablebody);
	table.appendChild(tablebody);
	newQuiz.appendChild(table);
	parent.appendChild(newQuiz);
	if (IE) newQuiz.scrollIntoView(false);
}

function appendEmptyQuizContext(tablebody) {
	var tr, td, text;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "context";
	td.colSpan = 2;
	text = document.createTextNode("Enter the quiz context. If no context is necessary, leave this field blank.");
	td.appendChild(text);
	td.appendChild(document.createElement("BR"));
	text = document.createElement("TEXTAREA");
	text.className = "context";
	text.onclick = setCurrentObject;
	td.appendChild(text);
	tr.appendChild(td);
	tablebody.appendChild(tr);
}

function appendEmptyQuestion(tablebody) {
	var tr, td, text;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "question";
	td.colSpan = 2;
	text = document.createTextNode("Enter a question.");
	td.appendChild(text);
	td.appendChild(document.createElement("BR"));
	text = document.createElement("TEXTAREA");
	text.className = "question";
	text.onclick = setCurrentObject;
	td.appendChild(text);
	tr.appendChild(td);
	tablebody.appendChild(tr);
	appendEmptyAnswer(tablebody);
	appendEmptyAnswer(tablebody);
	appendAnswerButton(tablebody);
}

function appendEmptyAnswer(tablebody) {
	var tr, td, text;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "ans";
	td.rowSpan = 2;
	text = document.createTextNode("Ans:");
	td.appendChild(text);
	tr.appendChild(td);
	td = document.createElement("TD");
	td.className = "ansa";
	text = document.createTextNode("Enter a possible answer.");
	td.appendChild(text);
	td.appendChild(document.createElement("BR"));
	text = document.createElement("TEXTAREA");
	text.className = "answer";
	text.onclick = setCurrentObject;
	td.appendChild(text);
	tr.appendChild(td);
	tablebody.appendChild(tr);
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "ansr";
	text = document.createTextNode("Enter the response to this answer.");
	td.appendChild(text);
	td.appendChild(document.createElement("BR"));
	text = document.createElement("TEXTAREA");
	text.className = "response";
	text.onclick = setCurrentObject;
	td.appendChild(text);
	tr.appendChild(td);
	tablebody.appendChild(tr);
}

function appendQuestionButton(tablebody) {
	var tr, td, button, text;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "qbutton";
	td.colSpan = 2;
	button = document.createElement("BUTTON");
	button.className = "quiz";
	button.onclick = insertQuestion;
	text = document.createTextNode("Insert another question");
	button.appendChild(text);
	td.appendChild(button);
	tr.appendChild(td);
	tablebody.appendChild(tr);
}

function appendAnswerButton(tablebody) {
	var tr, td, button, text;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "abutton";
	td.colSpan = 2;
	button = document.createElement("BUTTON");
	button.className = "quizans";
	button.onclick = insertAnswer;
	text = document.createTextNode("Insert another answer");
	button.appendChild(text);
	td.appendChild(button);
	tr.appendChild(td);
	tablebody.appendChild(tr);
}

function insertQuestion(myEvent) {
	var source = getSource(getEvent(myEvent));
	var tbody = document.createElement("TBODY");
	appendEmptyQuestion(tbody);
	var row = findRowFor(source);
	insertChildren(tbody,row);
}

function insertAnswer(myEvent) {
	var source = getSource(getEvent(myEvent));
	var tbody = document.createElement("TBODY");
	appendEmptyAnswer(tbody);
	var row = findRowFor(source);
	insertChildren(tbody,row);
}

function findRowFor(el) {
	var x = el.parentNode;
	while ((x != null) && (x.tagName.toUpperCase() != "TR")) x = x.parentNode;
	return x;
}

function insertChildren(src, dest) {
	var parent = dest.parentNode;
	var child = src.firstChild;
	while (child != null) {
		var next = child.nextSibling;
		parent.insertBefore(child,dest);
		child = next;
	}
}

function createCheckboxRow(name, string) {
	var tr, td, input, text;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "checkbox";
	input = document.createElement("INPUT");
	input.setAttribute("value","yes");
	if (name != "") input.setAttribute("name",name);
	input.onclick = setCurrentObject;
	input.setAttribute("type","checkbox");
	td.appendChild(input);
	tr.appendChild(td);
	td = document.createElement("TD");
	td.appendChild(document.createTextNode(string));
	tr.appendChild(td);
	return tr;
}

function createYesNoRow(name) {
	var tr, td, select, option;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.appendChild(document.createTextNode(name));
	tr.appendChild(td);
	td = document.createElement("TD");
	select = document.createElement("SELECT");
	select.className = "w300";
	select.onclick = setCurrentObject;
	option = document.createElement("OPTION");
	option.appendChild(document.createTextNode("no"));
	option.value = "no"
	select.appendChild(option);
	option = document.createElement("OPTION");
	option.appendChild(document.createTextNode("yes"));
	option.value = "yes";
	select.appendChild(option);
	td.appendChild(select);
	tr.appendChild(td);
	return tr;
}

function createInputTextRow(name, text) {
	var tr, td, input;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.appendChild(document.createTextNode(name));
	tr.appendChild(td);
	td = document.createElement("TD");
	input = document.createElement("INPUT");
	input.setAttribute("type","text");
	input.className = "w300";
	input.setAttribute("value",text);
	input.onclick = setCurrentObject;
	td.appendChild(input);
	tr.appendChild(td);
	return tr;
}

function createTextAreaRow() {
	var tr, td, textarea;
	tr = document.createElement("TR");
	td = document.createElement("TD");
	td.className = "checkbox";
	tr.appendChild(td);
	td = document.createElement("TD");
	textarea = document.createElement("TEXTAREA");
	textarea.onclick = setCurrentObject;
	textarea.className = "sectionP40";
	td.appendChild(textarea);
	tr.appendChild(td);
	return tr;
}

//Remove the currently selected object from a section page.
function objectRemove(section) {
	if (currentDiv == null) return;
	if (currentParagraph == null) return;
	if (currentObject == null) return;
	//get the div element and see if it is current.
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];
	if (currentDiv !== parent) return;
	parent.removeChild(currentParagraph);
	currentParagraph = null;
	currentObject = null;
	setToolEnables();
}

//Insert the first selected palette object into the docref field.
function setDocref(myEvent) {
	var source = getSource(getEvent(myEvent));
	var parent = source.parentElement;
	var inputs = parent.getElementsByTagName("INPUT");
	if (inputs.length == 0) return;

	var files = document.getElementById("palette").getElementsByTagName("IMG");
	for (var i=0; i<files.length; i++) {
		var img = files.item(i);
		if (isSelected(img)) {
			var title = img.getAttribute("title");
			inputs[0].value = "[user]" + title;
			return;
		}
	}
}

//Insert all the selected images from the
//palette into the current section page.
function imageObjectInsert(section) {
	var divs = section.getElementsByTagName("DIV");
	var parent = divs[0];

	var files = document.getElementById("palette").getElementsByTagName("IMG");
	for (var i=0; i<files.length; i++) {
		var img = files.item(i);
		if (isSelected(img) && isImage(img)) {
			var newP = document.createElement("P");
			newP.className = "p5";
			newP.setAttribute("item-type","image");
			var clone = img.cloneNode(true);
			deselect(clone);
			clone.onclick = setCurrentObject;
			newP.appendChild(clone);
			parent.appendChild(newP);
			if (IE) newP.scrollIntoView(false);
		}
	}
}

//Check to see if a file in the palette is an image'
//Accept dcm, png, gif, jpg, jpeg, tif, tiff, or any object with
//a name that looks like a DICOM SOPInstanceUID.
function isImage(img) {
	var name = img.title;
	if (name != null) {
		name = name.toLowerCase();
		var ext = name.lastIndexOf(".");
		if (ext != -1) {
			ext = name.substring(ext);
			if (ext == ".dcm") return true;
			else if (ext == ".png") return true;
			else if (ext == ".gif") return true;
			else if (ext == ".jpg") return true;
			else if (ext == ".jpeg") return true;
			else if (ext == ".tif") return true;
			else if (ext == ".tiff") return true;
			else if (ext == ".bmp") return true;
		}
		if (name.replace(/[\.\d]/g,"").length == 0) return true;
	}
	return false;
}

//Move the selected object up on the current section page.
function objectMoveUp(section) {
	if (currentDiv == null) return;
	if (currentParagraph == null) return;

	//get the div element and see if it is current.
	var divs = section.getElementsByTagName("DIV");
	var parent = divs.item(0);
	if (currentDiv !== parent) return;

	//okay, swap the currentParagraph with the one before it.
	var prevP = findPrev(currentParagraph);
	if (prevP == null) return;
	var nextSib = currentParagraph.nextSibling;
	parent.insertBefore(currentParagraph,prevP);
	if (nextSib != null)
		parent.insertBefore(prevP,nextSib);
	else
		parent.appendChild(prevP);
}

//Find the previous sibling with the same tagName as an element.
function findPrev(elem) {
	var prev = elem.previousSibling;
	while ((prev != null) && (prev.tagName != elem.tagName))
		prev = prev.previousSibling;
	return prev;
}

//Move the selected object down on the current section page.
function objectMoveDown(section) {
	if (currentDiv == null) return;
	if (currentParagraph == null) return;

	//get the div element and see if it is current.
	var divs = section.getElementsByTagName("DIV");
	var parent = divs.item(0);
	if (currentDiv !== parent) return;

	//swap the currentParagraph with the one after it.
	var nextP = findNext(currentParagraph);
	if (nextP == null) return;
	var nextSib = nextP.nextSibling;
	parent.insertBefore(nextP,currentParagraph);
	if (nextSib != null)
		parent.insertBefore(currentParagraph,nextSib);
	else
		parent.appendChild(currentParagraph);
}

//Find the next sibling with the same tagName as an element.
function findNext(elem) {
	var next = elem.nextSibling;
	while ((next != null) && (next.tagName != elem.tagName))
		next = next.nextSibling;
	return next;
}

//Set the list of breeds corresponding to a species selection
//in a veterinary medicine patient element
function setBreedList(myEvent) {
	var ptSpecies = getSource(getEvent(myEvent));
	var table = ptSpecies.parentElement.parentElement.parentElement;
	var selectElements = table.getElementsByTagName("SELECT");
	if (selectElements.length == 0) return;
	var ptBreed = selectElements[selectElements.length-1];
	if (ptBreed == null) return;
	ptBreed.options.length = 0;
	var choice = ptSpecies.selectedIndex;
	var breeds = breedList[choice];
	for (var i=0; i!=breeds.length; i++) {
		ptBreed.options[i] = new Option(breeds[i], breeds[i]);
	}
}

//WW/WL Presets
function WWWLPreset(name, ww, wl) {
	this.name = name;
	this.wl = wl;
	this.ww = ww;
}
wwwlPresets = new Array( //WW/WL
	new WWWLPreset("Presets", 0, 0),
	new WWWLPreset("Abdomen/pelvis", 350, 40),
	new WWWLPreset("Bone", 2500, 480),
	new WWWLPreset("Brain", 80, 40),
	new WWWLPreset("Liver", 120, 70),
	new WWWLPreset("Lung", 1500, -500),
	new WWWLPreset("Stroke", 50, 40),
	new WWWLPreset("Subdural", 350, 90)
);

//Set the WW/WL editor div and handle the button click event
function startWWWLEditor() {
	var div = document.getElementById("imginfo");
	if (div) {
		loadWWWLEditor(div.source);
	}
}
function loadWWWLEditor(source) {
	//Make sure there is a dcm image
	var orig = source.getAttribute("original-format");
	var isDCM = orig && (orig.lastIndexOf(".dcm") == orig.length-4)
	if (!isDCM) return;

	//Okay, it's DICOM, load the editor
	removeImgInfo();
	var wwwlEditorDiv = document.createElement("DIV");
	wwwlEditorDiv.id = "wwwlEditorDiv";
	wwwlEditorDiv.source = source;

	var pImage = document.createElement("P");
	var src = dirpath + source.getAttribute("base-image");
	var img = document.createElement("IMG");
	img.style.width = 512;
	img.style.height = "auto";
	img.id = "wwwlIMG";
	img.src = src;
	img.onmousedown = startWWWLDrag;
	pImage.appendChild(img);
	wwwlEditorDiv.appendChild(pImage);

	var mainEditorDiv = document.getElementById("mainEditorDiv");
	var parent = mainEditorDiv.parentNode;
	var nextSibling = mainEditorDiv.nextSibling;
	parent.insertBefore(wwwlEditorDiv,nextSibling);
	mainEditorDiv.style.visibility = "hidden";
	mainEditorDiv.style.display = "none";
	wwwlEditorDiv.style.visibility = "visible";
	wwwlEditorDiv.style.display = "block";
	wwwlEditorDiv.style.overflow = "auto";
	wwwlEditorDiv.style.background = "black";
	wwwlEditorDiv.style.height = findObject(document.body).h;

	//Get the image params
	var params = getDCMParams(source);
	wwwlEditorDiv.params = params;
/**/displayParams(wwwlEditorDiv, params);

	//Display the WW/WL popup
	var size = 1 << params.BitsStored;
	var min = params.RescaleIntercept;
	var max = size * params.RescaleSlope + min;
	img.min = min;
	img.max = max;
	img.size = size;

	var div = document.getElementById("wwwlPopup");
	if (div != null) div.parentNode.removeChild(div);
	div = document.createElement("DIV");
	div.className = "content";
	div.id = "wwwlPopupDiv";

	var pSelect = document.createElement("P");
	var select = document.createElement("SELECT");
	select.id = "SelectPreset";
	for (var k=0; k<wwwlPresets.length; k++) {
		addWWWLOption(select, wwwlPresets[k]);
	}
	select.onchange = selectPreset;
	pSelect.appendChild(select);

	var pTable = document.createElement("P");
	var table = document.createElement("TABLE");
	var tbody = document.createElement("TBODY");
	table.appendChild(tbody);

	var tr = document.createElement("TR");
	tbody.appendChild(tr);
	var td = document.createElement("TD");
	td.appendChild(document.createTextNode("Level:"));
	tr.appendChild(td);
	td = document.createElement("TD");
	var wl = document.createElement("INPUT");
	wl.className = "wwwl";
	wl.type = "text";
	wl.id = "WindowLevel";
	wl.value = params.WindowCenter;
	wl.onkeypress = wwwlCheckEnter;
	td.appendChild(wl);
	tr.appendChild(td);

	tr = document.createElement("TR");
	tbody.appendChild(tr);

	td = document.createElement("TD");
	td.appendChild(document.createTextNode("Width:"));
	tr.appendChild(td);

	td = document.createElement("TD");
	var ww = document.createElement("INPUT");
	ww.className = "wwwl";
	ww.type = "text";
	ww.id = "WindowWidth";
	ww.value = params.WindowWidth;
	ww.onkeypress = wwwlCheckEnter;
	td.appendChild(ww);
	tr.appendChild(td);

	pTable.appendChild(table);

	var pSaveImage = document.createElement("P");
	var b = document.createElement("INPUT");
	b.className = "stdbutton";
	b.type = "button";
	b.value = " Save Image ";
	b.onclick = saveWWWLImage;
	b.title = "Save these values in this image";
	pSaveImage.appendChild(b);

	var pSaveSeries = document.createElement("P");
	b = document.createElement("INPUT");
	b.className = "stdbutton";
	b.type = "button";
	b.value = " Save Series ";
	b.onclick = saveWWWLSeries;
	b.title = "Save these values in all images of the same series as this image";
	pSaveSeries.appendChild(b);

	var pReset = document.createElement("P");
	b = document.createElement("INPUT");
	b.className = "stdbutton";
	b.type = "button";
	b.value = " Reset ";
	b.onclick = wwwlReset;
	pReset.appendChild(b);

	div.appendChild(pSelect);
	div.appendChild(pTable);
	div.appendChild(pSaveImage);
	div.appendChild(pSaveSeries);
	div.appendChild(pReset);

	//showDialog(popupDivId, w, h, title, closeboxFile, heading, div, okHandler, cancelHandler, hide);
	showDialog("wwwlPopup", 190, 216, "Change Level & Width", closeboxURL, null, div, null, null, null, wwwlOK);
}

function addWWWLOption(select, preset) {
	var option = document.createElement("OPTION");
	option.appendChild(document.createTextNode(preset.name));
	select.appendChild(option);
}

function selectPreset() {
	var select = document.getElementById("SelectPreset");
	if (select) {
		var index = select.selectedIndex;
		if (index > 0) {
			var wlInput = document.getElementById("WindowLevel");
			wlInput.value = wwwlPresets[index].wl;
			var wwInput = document.getElementById("WindowWidth");
			wwInput.value = wwwlPresets[index].ww;
			changeWWWL();
			select.selectedIndex = 0;
		}
	}
}

function displayParams(div, params) {
	var p = document.createElement("P");
	p.style.marginLeft = 10;
	var table = document.createElement("TABLE");
	table.style.marginTop = 50;
	table.style.marginBottom = 50;
	var tbody = document.createElement("TBODY");
	addWWWLRow(tbody, "Modality", params.Modality);
	addWWWLRow(tbody, "BitsAllocated", params.BitsAllocated);
	addWWWLRow(tbody, "BitsStored", params.BitsStored);
	addWWWLRow(tbody, "HighBit", params.HighBit);
	addWWWLRow(tbody, "PixelRepresentation", params.PixelRepresentation);
	addWWWLRow(tbody, "RescaleSlope", params.RescaleSlope);
	addWWWLRow(tbody, "RescaleIntercept", params.RescaleIntercept);
	addWWWLRow(tbody, "WindowCenter", params.WindowCenter);
	addWWWLRow(tbody, "WindowWidth", params.WindowWidth);
	table.appendChild(tbody);
	p.appendChild(table);
	div.appendChild(p);
}
function addWWWLRow(tbody, name, value) {
	var tr = document.createElement("TR");
	var td = document.createElement("TD");
	td.appendChild(document.createTextNode(name));
	td.style.background = "black";
	td.style.color = "white";
	tr.appendChild(td);
	td = document.createElement("TD");
	td.appendChild(document.createTextNode(value));
	td.style.background = "black";
	td.style.color = "white";
	tr.appendChild(td);
	tbody.appendChild(tr);
}

function startWWWLDrag(evt) {
	evt = getEvent(evt);
	var source = getSource(evt);
	var startX = evt.clientX;
	var startY = evt.clientY;
	var max = source.max;
	var min = source.min;
	var size = source.size;

	var wlInput = document.getElementById("WindowLevel");
	var startWL = parseInt(wlInput.value);
	var wwInput = document.getElementById("WindowWidth");
	var startWW = parseInt(wwInput.value);

	if (document.addEventListener) {
		document.addEventListener("mousemove", dragWWWL, true);
		document.addEventListener("mouseup", dropWWWL, true);
	}
	else {
		source.attachEvent("onmousemove", dragWWWL);
		source.attachEvent("onmouseup", dropWWWL);
		source.setCapture();
	}
	if (event.stopPropagation) event.stopPropagation();
	else event.cancelBubble = true;
	if (event.preventDefault) event.preventDefault();
	else event.returnValue = false;
	return false;

	function dragWWWL(evt) {
		if (!evt) evt = window.event;
		var deltaY = evt.clientY - startY;
		var wl = startWL + deltaY; //- is north
		//if (wl > max) { wl = max; startY = evt.clientY; startWL = max; }
		//if (wl < min) { wl = min; startY = evt.clientY; startWL = min; }
		wlInput.value = wl;

		var deltaX = evt.clientX - startX;
		var ww = startWW + deltaX; //+ is east
		if (ww < 1) { ww = 1; startX = evt.clientX; startWW = 1; }
		if (ww > size) { ww = size; startX = evt.clientX; startWW = size; }
		wwInput.value = ww;

		if (evt.stopPropagation) evt.stopPropagation();
		else evt.cancelBubble = true;
		return false;
	}

	function dropWWWL(evt) {
		changeWWWL();
		if (!evt) evt = window.event;
		if (document.addEventListener) {
			document.removeEventListener("mouseup", dropWWWL, true);
			document.removeEventListener("mousemove", dragWWWL, true);
		}
		else {
			source.detachEvent("onmousemove", dragWWWL);
			source.detachEvent("onmouseup", dropWWWL);
			source.releaseCapture();
		}
		if (evt.stopPropagation) event.stopPropagation();
		else evt.cancelBubble = true;
		return false;
	}
}

function getDCMParams(source) {
	var p = new Object();
	src = dirpath + source.getAttribute("original-format");
	var req = new AJAX();
	req.GET(src, "params&" + req.timeStamp(), null);
	if (req.success()) {
		var xml = req.responseXML();
		if (xml) {
			xml = xml.firstChild;
			p.Modality = xml.getAttribute("Modality");
			p.BitsAllocated = getParamAsInt( xml.getAttribute("BitsAllocated"), 16 );
			p.BitsStored = getParamAsInt( xml.getAttribute("BitsStored"), 12 );
			p.HighBit = getParamAsInt( xml.getAttribute("HighBit"), 11 );
			p.PixelRepresentation = getParamAsInt( xml.getAttribute("PixelRepresentation"), 0 );
			p.RescaleSlope = getParamAsFloat( xml.getAttribute("RescaleSlope"), 1 );
			p.RescaleIntercept = getParamAsFloat( xml.getAttribute("RescaleIntercept"), 0 );
			p.WindowCenter = getParamAsInt( xml.getAttribute("WindowCenter"), 1000 );
			p.WindowWidth = getParamAsInt( xml.getAttribute("WindowWidth"), 1000 );
		}
	}
	return p;
}

function getParamAsFloat(string, def) {
	try { return parseFloat(string); }
	catch (e) { return def; }
}

function getParamAsInt(string, def) {
	try { return Math.round( parseFloat(string) ); }
	catch (e) { return def; }
}

function wwwlCheckEnter(evt) {
	var e = getEvent(evt);
	var key = e.keyCode;
	if (key == 13) changeWWWL();
}

function wwwlReset() {
	var div = document.getElementById("wwwlEditorDiv");
	var params = div.params;
	var wlInput = document.getElementById("WindowLevel");
	var wwInput = document.getElementById("WindowWidth");
	wlInput.value = params.WindowCenter;
	wwInput.value = params.WindowWidth;
	changeWWWL();
}

function changeWWWL() {
	var div = document.getElementById("wwwlEditorDiv");
	var source = div.source;
	var wlInput = document.getElementById("WindowLevel");
	var wwInput = document.getElementById("WindowWidth");
	var wl = parseInt(wlInput.value);
	var ww = parseInt(wwInput.value);
	src = dirpath + source.getAttribute("original-format") + "?jpeg&ww="+ww+"&wl="+wl;
	var img = document.getElementById("wwwlIMG");
	var w = img.clientWidth;
	var h = img.clientHeight;
	img.src = src;
	img.style.width = w;
	img.style.height = h;
}

function saveWWWLImage() {
	doWWWLSave("");
}
function saveWWWLSeries() {
	doWWWLSave("&series");
}
function doWWWLSave(series) {
	var div = document.getElementById("wwwlEditorDiv");
	var source = div.source;
	var wlInput = document.getElementById("WindowLevel");
	var wwInput = document.getElementById("WindowWidth");
	var wl = parseInt(wlInput.value);
	var ww = parseInt(wwInput.value);
	var doc = docpath.substring(docpath.lastIndexOf("/")+1);
	var src = dirpath + source.getAttribute("original-format");
	var req = new AJAX();
	var qs = "update"+series+"&ww="+ww+"&wl="+wl+"&doc="+doc + "&"+req.timeStamp();
	req.GET(src, qs, null);
	if (req.success()) {
		var xml = req.responseXML();
		if (xml) {
			xml = xml.firstChild;
			if (xml.tagName == "NOTOK") alert("Unable to save the modified image values.");
			else alert("The modified image values are\n"
					  +"being saved in a background process.\n"
					  +"You may continue to modify this or\n"
					  +"other images.");
		}
	}
}

function wwwlOK() {
	var popup = document.getElementById("wwwlPopup");
	if (popup) popup.parentNode.removeChild(popup);
	hidePopups(); //just in case

	var wwwlEditorDiv = document.getElementById("wwwlEditorDiv");
	var source = wwwlEditorDiv.source;

	var mainEditorDiv = document.getElementById("mainEditorDiv");
	wwwlEditorDiv.style.visibility = "hidden";
	wwwlEditorDiv.style.display = "none";
	mainEditorDiv.style.visibility = "visible";
	mainEditorDiv.style.display = "block";
	var parent = wwwlEditorDiv.parentNode;
	parent.removeChild(wwwlEditorDiv);
}

function imgMouseEnter(event) {
	var evt = getEvent(event);
	var source = getSource(evt);
	var sourcePos = findObject(source);
	var div = document.getElementById("imginfo");
	if (div) document.body.removeChild(div);
	div = document.createElement("DIV");
	div.source = source;
	div.id = "imginfo";
	div.className = "imginfo";
	div.onmouseout = imgMouseLeave;
	var srcName = source.src;
	srcName = srcName.substring(srcName.lastIndexOf("/") + 1);
	var height = 45;
	div.appendChild(document.createTextNode(srcName));
	div.appendChild(document.createElement("BR"));

	if (imageHasBeenSaved(source)) {
		var a = document.createElement("A");
		a.onclick = startSVGEditor;
		a.appendChild(document.createTextNode("Edit annotations (Double-click)"));
		div.appendChild(a);
		div.appendChild(document.createElement("BR"));
		height += 15;

		var a = document.createElement("A");
		a.onclick = startCaptionEditor;
		a.appendChild(document.createTextNode("Edit captions (CTRL-double-click)"));
		div.appendChild(a);
		div.appendChild(document.createElement("BR"));
		height += 15;

		var orig = source.getAttribute("original-format");
		var isDCM = orig && (orig.lastIndexOf(".dcm") == orig.length-4)
		if (isDCM) {
			a = document.createElement("A");
			a.onclick = startWWWLEditor;
			a.appendChild(document.createTextNode("Adjust level and width (ALT-double-click)"));
			div.appendChild(a);
			div.appendChild(document.createElement("BR"));
			height += 15;
		}
	}

	var x = sourcePos.x + sourcePos.w/4 - sourcePos.scrollLeft;
	var y = sourcePos.y + (sourcePos.h * 3)/4 - sourcePos.scrollTop;
	div.style.height = height;
	div.style.left = x;
	div.style.top = y;
	div.style.zIndex = 50;
	document.body.appendChild(div);
}

function imgMouseLeave(event) {
	var div = document.getElementById("imginfo");
	if (div) {
		var source = div.source;
		var x = window.event.clientX;
		var y = window.event.clientY;
		if (!containsXY(source, x, y) && !containsXY(div, x, y)) {
			document.body.removeChild(div);
		}
	}
}

function removeImgInfo() {
	var div = document.getElementById("imginfo");
	if (div) document.body.removeChild(div);
}

function containsXY(obj, x, y) {
	var pos = findObject(obj);
	if (pos) {
		if ((pos.x <= x) && (x < (pos.x + pos.w))
			&& (pos.y <= y) && (y < (pos.y + pos.h))) {
			return true;
		}
	}
	return false;
}

//Set the caption editor div and handle the button click event.
function startCaptionEditor() {
	var div = document.getElementById("imginfo");
	if (div) {
		loadCaptionEditor(div.source);
	}
}
function loadCaptionEditor(source) {
	removeImgInfo();
	var captionEditorDiv = document.createElement("DIV");
	captionEditorDiv.id = "captionEditorDiv";
	captionEditorDiv.source = source;

	var p0 = document.createElement("P");
	var p1 = document.createElement("P");
	var p2 = document.createElement("P");
	var p3 = document.createElement("P");
	var ta1 = document.createElement("TEXTAREA");
	var ta2 = document.createElement("TEXTAREA");
	var b = document.createElement("INPUT");

	captionEditorDiv.className = "capDiv";
	p0.className = "capCenter";
	p1.className = "capTextP";
	p2.className = "capTextP";
	p3.className = "capCenter";
	ta1.className = "capTextArea";
	ta2.className = "capTextArea";

	var text = source.getAttribute("alwayscaption");
	if (text != null) ta1.value = text.replace(/\\/g,"");
	var text = source.getAttribute("clickcaption").replace(/\\/g,"");
	if (text != null) ta2.value = text;
	b.type = "button";
	b.value = " OK ";
	b.onclick = captionOK;

	var src = source.getAttribute("src");
	var img = document.createElement("IMG");
	img.src = src;

	p0.appendChild(img);
	p1.appendChild(document.createTextNode("Caption for continuous display: "));
	p2.appendChild(document.createTextNode("Caption for display when clicked: "));
	p3.appendChild(b);

	captionEditorDiv.appendChild(p0);
	captionEditorDiv.appendChild(p1);
	captionEditorDiv.appendChild(ta1);
	captionEditorDiv.appendChild(p2);
	captionEditorDiv.appendChild(ta2);
	captionEditorDiv.appendChild(p3);

	var mainEditorDiv = document.getElementById("mainEditorDiv");
	var parent = mainEditorDiv.parentNode;
	var nextSibling = mainEditorDiv.nextSibling;
	parent.insertBefore(captionEditorDiv,nextSibling);
	mainEditorDiv.style.visibility = "hidden";
	mainEditorDiv.style.display = "none";
	captionEditorDiv.style.visibility = "visible";
	captionEditorDiv.style.display = "block";
	captionEditorDiv.style.overflow = "auto";
}

function captionOK() {
	var captionEditorDiv = document.getElementById("captionEditorDiv");
	var source = captionEditorDiv.source;
	var taList = captionEditorDiv.getElementsByTagName("TEXTAREA");
	source.setAttribute("alwayscaption", taList[0].value);
	source.setAttribute("clickcaption", taList[1].value);

	var mainEditorDiv = document.getElementById("mainEditorDiv");
	captionEditorDiv.style.visibility = "hidden";
	captionEditorDiv.style.display = "none";
	mainEditorDiv.style.visibility = "visible";
	mainEditorDiv.style.display = "block";
	var parent = captionEditorDiv.parentNode;
	parent.removeChild(captionEditorDiv);
	saveClicked();
}

//==================== special text functions ==================
//Filter an XML string.
//Fix up <br> and <hr> elements to make them well-formed.
//Escape any angle brackets in element value text.
function filter(text) {
	var t = text;
	var s = "";
	var tag;

	//First, kludge the end tags of param elements
	s = "";
	t = t.replace(/<\/param>/g, "");
	while ((tag = findTag(t)) != null) {
		var name = trim( t.substring(tag.index + 1, tag.index + 7) );
		if (name.toLowerCase() == "param") {
			var k = t.indexOf(">", tag.index);
			if (k == -1) {
				alert("Unable to parse the text.\nClick OK to continue.");
				break; //this should never happen, so bail out and hope for the best
			}
			s += t.substring(0, tag.index + tag[0].length -1) + "></param>";
			t = t.substring(tag.index + tag[0].length);
		}
		else {
			s += t.substring(0, tag.index + tag[0].length);
			t = t.substring(tag.index + tag[0].length);
		}
	}
	t = s + t;

	//Now start over and filter the other stuff;
	s = "";
	t = t.replace(/<br[\s]*>/g,"<br/>").replace(/<\/br[\s]*>/g,"");
	t = t.replace(/<hr[\s]*>/g,"<hr/>").replace(/<\/hr[\s]*>/g,"");
	t = t.replace(/\&/g,"&amp;");
	while ((tag = findTag(t)) != null) {
		s += t.substring(0,tag.index).replace(/>/g,"&gt;").replace(/</g,"&lt;") + tag[0];
		t = t.substring(tag.index + tag[0].length);
	}
	return s + t.replace(/>/g,"&gt;").replace(/</g,"&lt;");
}

function escapeChars(text) {
	return	text
			//	.replace(/&amp;/g,"&")
			//	.replace(/&lt;/g,"<")
			//	.replace(/&gt;/g,">")
			//	.replace(/&quot;/g,"\"")
			//	.replace(/&apos;/g,"'")
				.replace(/&/g,"&amp;")
				.replace(/>/g,"&gt;")
				.replace(/</g,"&lt;")
				.replace(/\"/g,"&quot;")
				.replace(/'/g,"&apos;");
}

function findTag(s) {
	var tagExp = /<[\/]?[A-Za-z][\w\-]*(\s+[A-Za-z][\w\-]*\s*=\s*\"[^\"]*\")*\s*[\/]?\s*>/;
	return s.match(tagExp);
}



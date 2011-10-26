function loaded() {
	if (ui == "classic") {
		var tools = new Array();
		tools[tools.length] = new PopupTool("/icons/save.png", "Create the MIRCdocument", null, save);
		tools[tools.length] = new PopupTool("/icons/home.png", "Return to the home page", "/query", null);
		setPopupToolPanel( tools );
	}
	//if (messageText != "") alert(messageText.replace(/\|/g,"\n"));
}
window.onload = loaded;

function newTemplate(context) {
	var selectElement = document.getElementById("templatename");
	var options = selectElement.getElementsByTagName("OPTION");
	var filename = options[selectElement.selectedIndex].value;
	var path = context+'?file='+filename;
	window.open(path,'_self');
}

var fileCount = 0;

function captureFile() {
	var selectedfileElement = document.getElementById("selectedfile" + fileCount);
	fileCount++;

	var filelabelElement = document.getElementById("filelabel");
	while (filelabelElement.firstChild) filelabelElement.removeChild(filelabelElement.firstChild);
	var newText = document.createTextNode("Include another file:");
	filelabelElement.appendChild(newText);

	var newInput = selectedfileElement.cloneNode(true); //document.createElement("INPUT");
	newInput.setAttribute("name","selectedfile" + fileCount);
	newInput.setAttribute("id","selectedfile" + fileCount);
	newInput.value = "";

	var br = document.createElement("BR");

	selectedfileElement.disabled = true;
	var parent = selectedfileElement.parentNode;
	parent.appendChild(br);
	parent.appendChild(newInput);
}

//Process all the input and submit the document
function save() {
	//Fix the text elements
	checkText("title");
	checkText("name");
	checkText("affiliation");
	checkText("contact");
	checkText("username");

	//Fix the abstract and all the sections
	var textareas = document.getElementsByTagName("TEXTAREA");
	for (var i=0; i<textareas.length; i++) {
		var name = textareas[i].name;
		setText(name, name+"-text");
	}
	//Enable all the non-blank selectedfile elements
	for (var i=0; i<fileCount; i++) {
		var sf = document.getElementById("selectedfile"+i);
		if (sf != null) {
			var t = sf.value;
			t = t.replace(/^\s+/g,"")	//trim the text
			t = t.replace(/\s+$/g,"");
			if (t != "") sf.disabled = false;
		}
	}

	var selectElement = document.getElementById("libSelect");
	if (selectElement) ssid = selectElement.value;

	var form = document.getElementById("formID");
	form.action = "/bauth/"+ssid;
	form.target = "_self";
	form.submit();
}

//Process a single paragraph section, converting to paragraphs
//and inserting the text into an input element for return in the form.
function setText(inId,outId) {
	var inElement = document.getElementById(inId);
	if (inElement == null) return false;
	var outElement = document.getElementById(outId);
	outElement.value = makeParagraphs(inElement.value);
	return true;
}

//Process a single text input field,
//making sure that it is well-formed.
function checkText(id) {
	var elem = document.getElementById(id);
	if (elem) elem.value = filter(elem.value);
}

//Create paragraph and list elements from a content string
function makeParagraphs(text) {
	var t = text;
	t = t.replace(/^\s+/g,"");					//trim the text
	t = t.replace(/\s+$/g,"");
	t = t.replace(/\r/g,"");					//flush the returns

	t = "<p>\n" + t + "\n</p>";					//wrap the text
	t = t.replace(/\n(\s*\n)+/g,"\n</p><p>");	//split into paragraphs

	t = t.replace(/((\n#[^\n]*)+)/g,"\n<ol>$&\n</ol>");		//insert the <ol> elements
	t = t.replace(/((\n\-[^\n]*)+)/g,"\n<ul>$&\n</ul>");	//insert the <ul> elements
	t = t.replace(/^[#\-]([^\n]*)/gm,"<li>$1</li>");		//insert the <li> elements

	t = t.replace(/\n(\s*\n)+/g,"\n");			//flush multiple newlines

	t = t.replace(/<p>\s*<\/p>/g,"");			//flush empty paragraphs

	//force at least one paragraph
	//if (t.replace(/\s*/g,"").length == 0) t = "<p></p>\n";

	t = filter(t);								//try to fix any escapable characters
	return t;
}

//Fix up <br> and <hr> elements to make them well-formed.
//Escape any angle brackets in element value text.
function filter(text) {
	var t = text;
	t = t.replace(/<br[\s]*>/g,"<br/>").replace(/<\/br[\s]*>/g,"");
	t = t.replace(/<hr[\s]*>/g,"<hr/>").replace(/<\/hr[\s]*>/g,"");
	t = t.replace(/\&/g,"&amp;");
	var s = "";
	var tag;
	while ((tag = findTag(t)) != null) {
		s += t.substring(0,tag.index).replace(/>/g,"&gt;").replace(/</g,"&lt;") + tag[0];
		t = t.substring(tag.index + tag[0].length);
	}
	return s + t.replace(/>/g,"&gt;").replace(/</g,"&lt;");
}

function findTag(s) {
	var tagExp = /<[\/]?[A-Za-z][\w\-]*(\s+[A-Za-z][\w\-]*\s*=\s*\"[^\"]*\")*\s*[\/]?\s*>/;
	return s.match(tagExp);
}


function loaded() {
	var tools = new Array();
	tools[tools.length] = new PopupTool("/icons/home.png", "Return to the admin page", "/ssadmin", null);
	setPopupToolPanel( tools );
	var here = document.getElementById("here");
	if ((here != null) && here.scrollIntoView) here.scrollIntoView(true); //align to top of page
}
window.onload = loaded;

function loaded() {
	var tools = new Array();
	tools[tools.length] = new PopupTool("/icons/home.png", "Return to the summary request page", "/summary/"+ssid, null);
	setPopupToolPanel( tools );
}
window.onload = loaded;

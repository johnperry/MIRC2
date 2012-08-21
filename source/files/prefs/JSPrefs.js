//Class to encapsulate a user's MIRC preferences
function Prefs() {
	this.xml = null;
	this.name = "";
	this.affiliation = "";
	this.contact = "";
	this.username = "";
	this.myrsna = false;

	var req = new AJAX();
	req.GET("/prefs/xml", req.timeStamp(), null);
	if (req.success()) {
		var respXML = req.responseXML();
		var root = respXML.documentElement;
		this.xml = root;
		this.name = getAttr(root, "name");
		this.affiliation = getAttr(root, "affiliation");
		this.contact = getAttr(root, "contact");
		this.username = getAttr(root, "username");
		this.myrsna = (getAttr(root, "myrsna") == "true");
	}
	function getAttr(root, attr) {
		var value = root.getAttribute(attr);
		return value ? value: "";
	}
}

//Class to encapsulate a user's MIRC preferences
function Prefs() {
	this.xml = null;
	this.name = "";
	this.affiliation = "";
	this.contact = "";
	this.username = "";

	var req = new AJAX();
	req.GET("/prefs/xml", req.timeStamp(), null);
	if (req.success()) {
		var respXML = req.responseXML();
		this.xml = respXML.documentElement;
		this.name = getAttr("name");
		this.affiliation = getAttr("affiliation");
		this.contact = getAttr("contact");
		this.username = getAttr("username");
	}
	function getAttr(attr) {
		var value = this.xml.getAttribute(attr);
		return value ? value: "";
	}
}

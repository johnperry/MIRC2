function sendScores(url) {

	var xml = "<Scores>";

	var ps = document.getElementsByTagName("P");
	var tables = document.getElementsByTagName("TABLE");
	if (ps.length != tables.length) {
		alert(ps.length + " paragraph elements found\n"
			 +tables.length + " table elements found");
	}

	for (var i=0; i<ps.length; i++) {
		var p = ps[i];
		var qid = p.getElementsByTagName("INPUT")[0].value;
		xml += "<Question id=\""+qid+"\">";

		var table = tables[i];
		var trs = table.getElementsByTagName("TR");

		for (var k=1; k<trs.length; k++) {
			var tr = trs[k];
			var tds = tr.getElementsByTagName("TD");
			var td = tds[3];
			var inputs = td.getElementsByTagName("INPUT");
			var score = inputs[0].value;
			var uid = inputs[1].value;
			xml += "<Answer id=\""+uid+"\" score=\""+score+"\"/>";
		}

		xml += "</Question>";
	}

	xml += "</Scores>";

	var form = document.createElement("FORM");
	form.method = "POST";
	form.action = url;
	form.encoding = "application/x-www-form-urlencoded";
	form.acceptCharset = "UTF-8";
	form.target = "_self";

	var input = document.createElement("INPUT");
	input.type = "hidden";
	input.name = "xml";
	input.value = xml;
	form.appendChild(input);

	var div = document.createElement("DIV");
	div.style.visibility = "hidden";
	div.appendChild(form);
	document.body.appendChild(div);

	form.submit();
}

function adjustScores(theEvent) {
	var source = getSource(getEvent(theEvent));
	var score = source.value;
	var answer = trim(source.parentNode.previousSibling.firstChild.nodeValue);
	var tbody = source.parentNode.parentNode.parentNode;
	var trs = tbody.getElementsByTagName("TR");
	for (var k=1; k<trs.length; k++) { //skip the heading row
		var tr = trs[k];
		var tds = tr.getElementsByTagName("TD");
		trAnswer = trim(tds[2].firstChild.nodeValue);
		if (trAnswer == answer) tds[3].firstChild.value = score;
	}
}

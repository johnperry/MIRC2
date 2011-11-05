function get(name) {
	var email = trim(document.getElementById("email").value);
	if (email == "") {
		alert("Please enter an email address.");
	}
	else {
		var pname = trim(document.getElementById("pname").value);
		var iname = trim(document.getElementById("iname").value);
		var interest = getSelectedRadioButton("interest");
		var sitetype = getSelectedRadioButton("sitetype");

		var url = "/download/"+name
					+"?email="+email
						+"&pname="+pname
							+"&iname="+iname
								+"&interest="+interest
									+"&sitetype="+sitetype;
		window.open(url, "_self");
	}
}

function getSelectedRadioButton(name) {
	var radios = document.getElementsByName(name);
	for (var i=0; i<radios.length; i++) {
		if (radios[i].checked) return radios[i].value;
	}
	return "";
}

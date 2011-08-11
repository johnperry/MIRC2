function get(name) {
	var email = document.getElementById("email").value;
	var url = "/download/"+name+"?email="+email;
	window.open(url, "_self");
}

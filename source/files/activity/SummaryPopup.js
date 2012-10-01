//Load a popup to submit a summary report and display the response from the server
function showSubmissionPopup(url) {
	var id = "submissionPopupID";
	var pop = document.getElementById(id);
	if (pop) pop.parentNode.removeChild(pop);

	var div = document.createElement("DIV");
	div.className = "content";
	var w = 500;
	var h = 200;
	var iframe = document.createElement("IFRAME");
	iframe.style.width = w - 30;
	iframe.style.height = h - 55;
	iframe.src = url;
	div.appendChild(iframe);
	var closebox = "/icons/closebox.gif";
	showDialog(id, w, h, "Summary Report Result", closebox, null, div, null, null);
}

function AdjustResultsHeight() {
	var dv = document.getElementById('qrresults');
	var h = getHeight() - dv.offsetTop;
	if (h < 50) h = 50;
	dv.style.height = h;
}

function getHeight() {
	if (document.all) return document.body.clientHeight;
	return window.innerHeight - 10;
}

window.onresize = AdjustResultsHeight;
window.onload = AdjustResultsHeight;


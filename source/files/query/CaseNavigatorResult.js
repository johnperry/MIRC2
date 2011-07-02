var currentcase = 1;
var maxcase = caselist.length;

function load(n) {
	currentcase += n;
	if (currentcase < 1) currentcase = 1;
	if (currentcase > maxcase) currentcase = maxcase;
	var s = document.getElementById('casenumberdisplay');
	if (maxcase > 0) {
		s.innerHTML = "Case " + currentcase + " of " + maxcase;
		window.open(caselist[currentcase-1],"MIRCinnerframe");
	}
	else s.innerHTML = "no cases";
	return false;
}

function adjustHeight() {
	var f = document.getElementById('MIRCinnerframeID');
	var h = getHeight() - f.offsetTop;
	if (50 > h) h = 50;
	f.style.height = h;
}

function getHeight() {
	if (document.all) return document.body.clientHeight;
	return window.innerHeight - 22;
}

function cnonload() {
	adjustHeight();
	if (randomizeList) randomize(caselist);
	load(0);
}

window.onresize = adjustHeight;
window.onload = cnonload;

var today = new Date();
var seed = today.getTime();

function rnd() {
	seed = (seed*9301+49297)%233280;
	return seed/233280.0;
}

function rand(number) {
	return Math.ceil(rnd()*number);
}

function randomize(list) {
	var temp; var i; var j;
	var listlen = list.length;
	for (i=0; i<listlen-1; i++) {
		j = rand(listlen - i) + i - 1;
		if (j >= listlen) j = listlen - 1;
		temp = caselist[i];
		list[i] = list[j];
		list[j] = temp;
	}
}

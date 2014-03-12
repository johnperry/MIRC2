var myStuffItems = new Array (
		new Item("My Preferences", showMyPreferences),
		new Item("My Conferences", showMyConferences),
		new Item("My Files", showMyFiles) );

var localLibrariesItems = new Array (
		new Item("My Teaching Files", null) );

var authorItems = new Array (
		new Item("Local Library", null, "localLibraryItem"),
		new Menu("Select Local Library", localLibrariesItems),
		new Item("", null),
		new Item("Basic Author Tool", showBasicAuthorTool, "basicAuthorItem"),
		new Item("Advanced Author Tool", showAdvancedAuthorTool, "advAuthorItem"),
		new Item("Zip Service", showZipService, "zipItem"),
		new Item("Submit Service", showSubmitService, "subItem"),
		new Item("Author Summary Report", showAuthorSummary),
		new Item("Approval Queue", showApprovalQueue, "approvalQueueItem") );

var adminItems = new Array (
		new Item("Log Viewer", showLogViewer),
		new Item("System Properties", showSysProps),
		new Item("User Manager", showUserManager, "userManager"),
		new Item("Query Service Admin", showQueryServiceAdmin),
		new Item("File Service Admin", showFileServiceAdmin),
		new Item("Storage Service Admin", showStorageServiceAdmin),
		new Item("", null),
		new Item("Configuration", showPipelineConfig),
		new Item("Status", showPipelineStatus),
		new Item("Quarantines", showPipelineQuarantines),
		new Item("ID Map", showIDMap),
		new Item("Object Tracker", showObjectTracker),
		new Item("Database Verifier", showDBVerifier),
		new Item("DICOM Anonymizer Configurator", showDicomAnonConfig),
		new Item("Script Editor", showScriptEditor),
		new Item("Lookup Table Editor", showLUTEditor),
		new Item("Set Logger Levels", setLoggerLevels),
		new Item("", null),
		new Item("Shutdown", shutdown) );

var helpItems = new Array (
		new Item("Switch to Integrated User Interface", switchToIntegratedUI),
		new Item("", null),
		new Item("CTP Wiki", showWiki),
		new Item("CTP DICOM Anonymizer", showAnonymizer),
		new Item("CTP DICOM Anonymizer Configurator", showAnonymizerConfigurator),
		new Item("", null),
		new Item("MIRC Help", showHelpPopup),
		new Item("About MIRC", showAboutPopup),
		new Item("", null),
		new Item("Download Software", downloadMIRC, "downloadItem"),
		new Item("", null),
		new Item("Notes", showDisclaimerPopup, "disclaimerItem") );

var myStuffMenu = new Menu("My Stuff",myStuffItems, "myStuffMenu");
var authorMenu = new Menu("Author", authorItems, "authorMenu");
var adminMenu = new Menu("Admin", adminItems, "adminMenu");
var helpMenu = new Menu("Help", helpItems);

var menuBar = new MenuBar("menuBar", new Array (myStuffMenu, authorMenu, adminMenu, helpMenu));

function setMenuEnables() {
	menuBar.setEnable("myStuffMenu", user.isLoggedIn);
	menuBar.setEnable("authorMenu", user.hasRole("author"));
	menuBar.setEnable("approvalQueueItem", user.hasRole("publisher"));
	menuBar.setEnable("adminMenu", user.hasRole("admin"));
	menuBar.setEnable("userManager", (user.usersClass.indexOf("FileImpl") != -1));
	menuBar.setEnable("downloadItem", (downloadenb == "yes"));
	menuBar.setEnable("disclaimerItem", (disclaimerURL != ""));

	//set the author menu enables for the currently selected local library
	if (localLibraries.length > 0) {
		if (selectedLocalLibrary > localLibraries.length) selectedLocalLibrary = 0;
		var lib = localLibraries[selectedLocalLibrary];
		authorItems[0].title = lib.title;

		menuBar.setEnable("basicAuthorItem", (lib.authenb == "yes"));
		menuBar.setEnable("advAuthorItem", (lib.authenb == "yes"));
		menuBar.setEnable("zipItem", (lib.zipenb == "yes"));
		menuBar.setEnable("subItem", (lib.subenb == "yes"));
	}
}

//Handlers for the My Stuff menu
//
switchToIntegratedUI
function switchToIntegratedUI(event, item) {
	window.open("/query?UI=integrated","_self");
}

function showMyPreferences(event, item) {
	window.open("/prefs","_self");
}

function showMyConferences(event, item) {
	window.open("/confs","_self");
}

function showMyFiles(event, item) {
	window.open("/files?home=/query","_self");
}

//Handlers for the Author menu
//
function selectLocalLibrary(event, item) {
	selectedLocalLibrary = item.libIndex;
	setMenuEnables();
}

function showBasicAuthorTool(event, item) {
	var ssid = localLibraries[selectedLocalLibrary].id;
	window.open("/bauth/"+ssid,"_self");
}

function showAdvancedAuthorTool(event, item) {
	var ssid = localLibraries[selectedLocalLibrary].id;
	window.open("/aauth/"+ssid,"_self");
}

function showZipService(event, item) {
	var ssid = localLibraries[selectedLocalLibrary].id;
	window.open("/zip/"+ssid,"_self");
}

function showSubmitService(event, item) {
	var ssid = localLibraries[selectedLocalLibrary].id;
	window.open("/submit/"+ssid,"_self");
}

function showAuthorSummary(event, item) {
	var ssid = localLibraries[selectedLocalLibrary].id;
	window.open("/summary/"+ssid,"_self");
}

function showApprovalQueue(event, item) {
	var query = "";
	query += "firstresult=" + 1;
	query += "&maxresults=" + 25;
	query += "&orderby=lmdate";
	query += "&server=" + getLocalServerIDs();
	query += "&pubreq=yes";
	query += "&queryUID=none";
	window.open("/query?"+query, "_self");
}

function getLocalServerIDs() {
	var ids = "";
	for (var i=0; i<allServers.length; i++) {
		if (allServers[i].isLocal) {
			if (ids != "") ids += ":";
			ids += i;
		}
	}
	return ids;
}

//Handlers for the Admin menu
//
function showLogViewer(event, item) {
	window.open("/logs?home=/query","_self");
}

function showSysProps(event, item) {
	window.open("/system?home=/query","_self");
}

function showUserManager(event, item) {
	window.open("/users?home=/query","_self");
}

function showQueryServiceAdmin(event, item) {
	window.open("/qsadmin","_self");
}

function showFileServiceAdmin(event, item) {
	window.open("/fsadmin?home=/query","_self");
}

function showStorageServiceAdmin(event, item) {
	window.open("/ssadmin","_self");
}

//Handlers for the CTP menu
function showPipelineConfig(event, item) {
	window.open("/configuration?home=/query","_self");
}

function showPipelineStatus(event, item) {
	window.open("/status?home=/query","_self");
}

function showPipelineQuarantines(event, item) {
	window.open("/quarantines?home=/query","_self");
}

function showIDMap(event, item) {
	window.open("/idmap?home=/query","_self");
}

function showObjectTracker(event, item) {
	window.open("/objecttracker?home=/query","_self");
}

function showDBVerifier(event, item) {
	window.open("/databaseverifier?home=/query","_self");
}

function showDicomAnonConfig(event, item) {
	window.open("/daconfig?home=/query","_self");
}

function showScriptEditor(event, item) {
	window.open("/script?home=/query","_self");
}

function showLUTEditor(event, item) {
	window.open("/lookup?home=/query","_self");
}

function setLoggerLevels(event, item) {
	window.open("/level?home=/query","_self");
}

function shutdown(event, item) {
	window.open("/shutdown","_self");
}

//Handlers for the Help menu
//
function showWiki(event, item) {
	window.open("http://mircwiki.rsna.org/index.php?title=CTP-The_RSNA_Clinical_Trial_Processor","help");
}
function showAnonymizer(event, item) {
	window.open("http://mircwiki.rsna.org/index.php?title=The_CTP_DICOM_Anonymizer","help");
}
function showAnonymizerConfigurator(event, item) {
	window.open("http://mircwiki.rsna.org/index.php?title=The_CTP_DICOM_Anonymizer_Configurator","help");
}
function downloadMIRC(event, item) {
	window.open("/download?ui=classic","_self");
}

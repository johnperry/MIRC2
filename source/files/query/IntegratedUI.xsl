<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:preserve-space elements="*"/>

<xsl:param name="options"/>
<xsl:param name="species"/>

<xsl:variable name="normalizedSitename" select="normalize-space(/mirc/@sitename)"/>
<xsl:variable name="sitename">
	<xsl:if test="$normalizedSitename"><xsl:value-of select="$normalizedSitename"/></xsl:if>
	<xsl:if test="not($normalizedSitename)"><xsl:text>RSNA TFS</xsl:text></xsl:if>
</xsl:variable>

<xsl:template match="*|@*|text()">
	<xsl:copy>
		<xsl:apply-templates select="*|@*|text()"/>
	</xsl:copy>
</xsl:template>

<xsl:template match="/mirc">
<html>

<head>
	<meta name="keywords"
		content="radiology software, MIRC, teaching, teaching file, ct, mri, mr, ultrasound, open-source
				radiography, education, radiology, DICOM, software, presentations, conferences, java"/>
	<meta name="description"
		content="MIRC is a free software server program to store radiology teaching files.
				It was created and is supported by the Radiological Society of North America (RSNA)."/>
	<title><xsl:value-of select="$sitename"/></title>
	<link rel="stylesheet" href="/JSPopup.css" type="text/css"/>
	<link rel="stylesheet" href="/JSPage.css" type="text/css"/>
	<link rel="stylesheet" href="/query/IntegratedUIMenus.css" type="text/css"/>
	<link rel="stylesheet" href="/query/IntegratedUI.css" type="text/css"/>
	<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSAJAX.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSUser.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSCookies.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/prefs/JSPrefs.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSTree.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSLoginPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSPage.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSSplitPane.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSScrollableTable.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSMenu.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/activity/SummaryPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/QueryServicePopups.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/IntegratedUIConfs.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/IntegratedUIFiles.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/IntegratedUI.js">;</script>
	<xsl:call-template name="params"/>
	<xsl:call-template name="vet-data"/>
</head>

<body>
	<xsl:call-template name="header"/>

	<div id="main">
		<div id="left" class="leftpane">
			<xsl:call-template name="leftpane"/>
		</div>
		<div id="center" class="centerpane">&#160;</div>
		<div id="right" class="rightpane">
			<xsl:call-template name="rightpane"/>
		</div>
	</div>

	<xsl:call-template name="footer"/>
	<xsl:call-template name="AdvancedQuery"/>
</body>

</html>
</xsl:template>

<xsl:template name="header">
<div class="header">
	<div class="headerLeft">
		<span class="sitename"><xsl:value-of select="$sitename"/></span>
	</div>
	<div class="headerRight">

		<div style="float:right; margin-top:7px;">
			<xsl:text>&#160;&#160;</xsl:text>
			<input type="button" value="Search" onclick="search();"/>
			<xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
			<span class="headerText">Welcome </span>
			<span class="headerText" id="usernameSpan">&#160;</span>
			<xsl:text>&#160;&#160;</xsl:text>
			<span class="headerText" id="myAccount">
				<xsl:text>|&#160;&#160;</xsl:text>
				<a href="javascript:preferences()">My Account</a>
				<xsl:text>&#160;&#160;</xsl:text>
			</span>
			<span class="headerText">
				<xsl:text>|&#160;&#160;</xsl:text>
				<a href="javascript:loginLogout();" id="loginoutAnchor">Login</a>
				<xsl:text>&#160;&#160;</xsl:text>
			</span>
		</div>

		<div style="float:right;">
			<input type="text" id="freetext" value="Search..." onkeyup="keyClicked(event);"/>
			<br/>
			<div class="modifiers">
				<a href="javascript:showAdvancedQueryPopup();">Advanced Search</a>
				<span>&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;</span>
				<a href="javascript:showServersPopup();">Select Libraries</a>
			</div>
		</div>

		<div style="float:right; margin-top:7px;">
			<input type="button" value="Clear" onclick="clearModifiers();"/>
			<xsl:text>&#160;&#160;</xsl:text>
		</div>

	</div>
</div>
</xsl:template>

<xsl:template name="leftpane">
	<div id="Cases">
		<!--<div class="L1">Cases</div>-->
		<div class="L2" id="MyDocuments"><a href="javascript:queryMineNew();">My Cases</a><br/></div>
		<div class="L2" id="AllDocuments"><a href="javascript:queryAllNew();">Completed Cases</a><br/></div>
		<div class="L2" id="DraftDocuments">
			<a href="javascript:queryTemp();"
			   			title="Draft cases can be viewed and claimed by any user with the author privilege.">
				Draft Cases
			</a>
			<br/>
		</div>
		<div class="L2" id="ApprovalQueue"><a href="javascript:approvalQueueNew();">Case Approval Queue</a><br/></div>

		<div id="Conferences">
			<div class="MenuBar" id="confMenuBar">;</div>
			<div id="confs">;</div>
		</div>

		<div id="FileCabinets">
			<div class="MenuBar" id="fileMenuBar">;</div>
			<div id="cabs">;</div>
		</div>

		<div class="L2x" id="CaseOfTheDay">
			<xsl:variable name="url" select="normalize-space(news/url)"/>
			<xsl:variable name="title" select="normalize-space(news/title)"/>
			<a href="{$url}" target="shared" xtitle="{$title}">Case of the Day</a>
			<br/>
			<xsl:if test="news/image">
				<img class="cod"
					 src="{normalize-space(news/image)}"
					 width="128"
					 xtitle="{$title}"
					 onclick="window.open('{$url}', 'shared');"/>
				<br/>
			</xsl:if>
		</div>

		<xsl:if test="@downloadenb='yes'">
			<div class="L2x" id="Download">
				<a href="javascript:downloadService()" title="Get the latest MIRC software.">
					Download Software
				</a>
				<br/>
			</div>
		</xsl:if>
	</div>

	<div id="AuthorTools">
		<hr/>
		<!--<div class="L1">Author Tools</div>-->
		<div class="L2" id="bat">
			<a href="javascript:basicAuthorTool('ss1')" title="Use this tool to create cases using images on your computer.">
				Basic Author Tool
			</a>
			<br/>
		</div>
		<div class="L2" id="aat">
			<a href="javascript:advAuthorTool('ss1')" title="Use this tool to create cases using images from the file cabinets on the server.">
				Advanced Author Tool
			</a>
			<br/>
		</div>
		<div class="L2" id="ssvc">
			<a href="javascript:submitService('ss1')" title="Use this service to insert cases exported from another site.">
				Submit Service
			</a>
			<br/>
		</div>
		<div class="L2" id="zsvc">
			<a href="javascript:zipService('ss1')" title="Use this service to create cases from a zip file of images.">
				Zip Service
			</a>
			<br/>
		</div>
		<div class="L2" id="srpt">
			<a href="javascript:showAuthorSummary('ss1')" title="Display an author summary report.">
				Author Summary
			</a>
			<br/>
		</div>
	</div>

	<div id="Admin">
		<hr/>
		<div class="L2"><a href="/users?home=/query">User Manager</a><br/></div>
		<div class="L2"><a href="/qsadmin">Query Service</a><br/></div>
		<div class="L2"><a href="/fsadmin?home=/query">File Service</a><br/></div>
		<div class="L2"><a href="/ssadmin">Storage Service</a><br/></div>
		<div class="L2"><a href="/activity" target="shared">Activity Report</a><br/></div>
		<hr/>
		<div class="L2"><a href="/daconfig?home=/query">DICOM Anonymizer</a><br/></div>
		<div class="L2"><a href="/script">Script Editor</a><br/></div>
		<div class="L2"><a href="/lookup">Lookup Table Editor</a><br/></div>
		<div class="L2"><a href="/system">System Properties</a><br/></div>
		<div class="L2"><a href="/configuration">CTP Configuration</a><br/></div>
		<div class="L2"><a href="/status">CTP Status</a><br/></div>
		<div class="L2"><a href="/quarantines">Quarantines</a><br/></div>
		<div class="L2"><a href="/logs?home=/query">Log Viewer</a><br/></div>
		<div class="L2"><a href="/level">Logger Levels</a><br/></div>
		<div class="L2"><a href="javascript:listCookies();">List Cookies</a><br/></div>
		<hr/>
		<div class="L2"><a href="/reset" title="Reset the DICOM and TCE Service templates and clear the cache">Reset Templates</a><br/></div>
	</div>
</xsl:template>

<xsl:template name="rightpane">
	No results.
</xsl:template>

<xsl:template name="footer">
<div class="footer">
    <div class="footerlogo">
    	<img src="/mirc/images/RSNAinformaticsLogo.jpg"/>
    	<span class="productname">MIRC TEACHING FILE SYSTEM</span>
	</div>
	<div class="footerRight">
		<a href="/query?UI=classic">Classic UI</a>&#160;&#160;
		|&#160;&#160;<a href="javascript:showHelpPopup();">Help</a>&#160;&#160;
		|&#160;&#160;<a href="javascript:showAboutPopup();">About TFS</a>&#160;&#160;
		|&#160;&#160;<a href="http://mircwiki.rsna.org/index.php?title=MIRC_Articles" target="wiki">RSNA MIRC Wiki</a>&#160;&#160;
	</div>
</div>
</xsl:template>

<xsl:template name="params">
	<script>
		var sitename = "<xsl:value-of select="$sitename"/>";
		var mode = "<xsl:value-of select="@mode"/>";
		var version = "<xsl:value-of select="@version"/>";
		var rsnaVersion = "<xsl:value-of select="@rsnaVersion"/>";
		var date = "<xsl:value-of select="@date"/>";
		var serverURL = "<xsl:value-of select="normalize-space(@siteurl)"/>";
		var selectedLocalLibrary = 0;
		var localLibraries = <xsl:text>new Array(</xsl:text>
			<xsl:for-each select="Libraries/Library[@local='yes']">
				new LocalLibrary(
					"<xsl:value-of select="@id"/>",
					"<xsl:value-of select="normalize-space(title)"/>",
					"<xsl:value-of select="@authenb"/>",
					"<xsl:value-of select="@subenb"/>",
					"<xsl:value-of select="@zipenb"/>"
				<xsl:text>)</xsl:text>
				<xsl:if test="position() != last()">,</xsl:if>
			</xsl:for-each>);
		var allServers = <xsl:text>new Array(</xsl:text>
			<xsl:for-each select="Libraries/Library">
				new Library(
					"<xsl:value-of select="@enabled"/>",
					"<xsl:value-of select="@deflib"/>",
					"<xsl:value-of select="@address"/>",
					"<xsl:value-of select="normalize-space(title)"/>",
					"<xsl:value-of select="@local"/>"
				<xsl:text>)</xsl:text>
				<xsl:if test="position() != last()">,</xsl:if>
			</xsl:for-each>
			);
		var disclaimerURL = '<xsl:value-of select="normalize-space(@disclaimerurl)"/>';
		var codURL = '<xsl:value-of select="normalize-space(news/url)"/>';
		var sessionPopup = '<xsl:value-of select="normalize-space(@popup)"/>';
	</script>
</xsl:template>

<!--Advanced Query Popup -->
<xsl:template name="AdvancedQuery">
<div class="content" style="display:none;" id="AdvancedQuery">
	<table width="100%">
		<tr>
			<td align="center" valign="top" style="height:30px; width:75%;">
				<xsl:call-template name="page-buttons"/>
			</td>
			<td valign="top" rowspan="2"
				style="font-family:sans-serif;font-size:10pt; padding:25 0 20 10;">
				<xsl:call-template name="query-modifiers"/>
			</td>
		</tr>
		<tr>
			<td valign="top" id="querypages"><xsl:call-template name="query-pages"/></td>
			<td/>
		</tr>
	</table>
	<div class="search">
		<input id="go" class="go" type="button" value="Go" onclick="doAdvancedQuery();"/>
	</div>
</div>
</xsl:template>

<xsl:template name="query-modifiers">
	<input type="checkbox" name="unknown" id="unknown" onchange="setModifierValues();" value="yes"
		title="Format documents as unknown cases">Display as unknowns</input>
	<br />
	<input type="checkbox" name="icons" id="icons" onchange="setModifierValues();" value="no"
		title="Hide the icon images in MSTF and Tab displays">Suppress icon images</input>
	<br/>
	<div class="formatselect">
		<select name="orderby" id="orderby" onchange="setModifierValues();" title="Select the order of query results">
			<option value="title">Document title</option>
			<option value="library">Library</option>
			<option value="author">Author</option>
			<option value="specialty">Specialty</option>
			<option value="lmdate" selected="">Last modified date</option>
		</select>
	</div>
	<div class="formatselect">
		<select name="maxresults" id="maxresults" onchange="setModifierValues();" title="Choose the maximum number of results from each library">
			<option value="10">10 results/site</option>
			<option value="25" selected="">25 results/site</option>
			<option value="50">50 results/site</option>
			<option value="100">100 results/site</option>
			<option value="500">500 results/site</option>
		</select>
	</div>
	<div class="formatselect">
		<select name="display" id="display" onchange="setModifierValues();" title="Choose the format in which documents will be displayed">
			<option value="" selected="">Document format</option>
			<option value="page">Page format</option>
			<option value="tab">Tab format</option>
			<option value="mstf">MSTF format</option>
		</select>
	</div>
	<div class="formatselect">
		<select name="bgcolor" id="bgcolor" onchange="setModifierValues();" title="Choose the background shade for display">
			<option value="" selected="">Document background</option>
			<option value="light">Light background</option>
			<option value="dark">Dark background</option>
		</select>
	</div>
	<input class="clearbutton" type="button" value="Clear all query fields" onclick="clearQueryFields();"/>
</xsl:template>

<xsl:template name="page-buttons">
	<input class="tab" type="button" onclick="bclick('div1',event)" value="Basic"    id="page1tab"/>
	<input class="tab" type="button" onclick="bclick('div2',event)" value="Document" id="page2tab"/>
	<input class="tab" type="button" onclick="bclick('div3',event)" value="Content"  id="page3tab"/>
	<input class="tab" type="button" onclick="bclick('div4',event)" value="Clinical" id="page4tab"/>
	<input class="tab" type="button" onclick="bclick('div5',event)" value="Image"    id="page5tab"/>
	<input class="tab" type="button" onclick="bclick('div6',event)" value="Patient"  id="page6tab"/>
</xsl:template>

<xsl:template name="query-pages">
	<div id="div1" style="visibility:visible;display:block">
		<xsl:call-template name="basic"/>
	</div>
	<div id="div2" style="visibility:hidden;display:none">
		<xsl:call-template name="document"/>
	</div>
	<div id="div3" style="visibility:hidden;display:none">
		<xsl:call-template name="content"/>
	</div>
	<div id="div4" style="visibility:hidden;display:none">
		<xsl:call-template name="clinical"/>
	</div>
	<div id="div5" style="visibility:hidden;display:none">
		<xsl:call-template name="image"/>
	</div>
	<div id="div6" style="visibility:hidden;display:none">
		<xsl:call-template name="patient"/>
	</div>
</xsl:template>

<xsl:template name="basic">
	<table width="100%" border="1">
		<tr>
			<td width="25%">Free Text Search:</td>
			<td><input style="width:100%" name="document" id="aqfreetext"></input></td>
		</tr>
		<tr>
			<td>Title:</td>
			<td><input style="width:100%" name="title"></input></td>
		</tr>
		<tr>
			<td>Author:</td>
			<td><input style="width:100%" name="author"></input></td>
		</tr>
		<tr>
			<td>Abstract:</td>
			<td><input style="width:100%" name="abstract"></input></td>
		</tr>
		<tr>
			<td>Keywords:</td>
			<td><input style="width:100%" name="keywords"></input></td>
		</tr>
	</table>
</xsl:template>

<xsl:template name="document">
	<table width="100%" border="1">
		<tr>
			<td width="25%">Document type:</td>
			<td>
				<select style="width:100%" name="document-type">
					<xsl:copy-of select="$options/enumerated-values/document-type/option"/>
				</select>
			</td>
		</tr>
		<tr>
			<td>Specialty:</td>
			<td>
				<select style="width:100%" name="category">
					<xsl:copy-of select="$options/enumerated-values/category/option"/>
				</select>
			</td>
		</tr>
		<tr>
			<td>Level:</td>
			<td>
				<select style="width:100%" name="level">
					<xsl:copy-of select="$options/enumerated-values/level/option"/>
				</select>
			</td>
		</tr>
		<tr>
			<td>Access:</td>
			<td>
				<select style="width:100%" name="access">
					<xsl:copy-of select="$options/enumerated-values/access/option"/>
				</select>
			</td>
		</tr>
		<tr>
			<td>Language:</td>
			<td><input style="width:100%" name="language"></input></td>
		</tr>
		<tr>
			<td>Peer-review:</td>
			<td>
				<input type="checkbox" name="peer-review" value="yes">Peer-reviewed documents only</input>
			</td>
		</tr>
	</table>
</xsl:template>

<xsl:template name="content">
	<table width="100%" border="1">
		<tr>
			<td width="25%">History:</td>
			<td><input style="width:100%" name="history"></input></td>
		</tr>
		<tr>
			<td>Findings:</td>
			<td><input style="width:100%" name="findings"></input></td>
		</tr>
		<tr>
			<td>Diagnosis:</td>
			<td><input style="width:100%" name="diagnosis"></input></td>
		</tr>
		<tr>
			<td>Differential Diagnosis:</td>
			<td><input style="width:100%" name="differential-diagnosis"></input></td>
		</tr>
		<tr>
			<td>Discussion:</td>
			<td><input style="width:100%" name="discussion"></input></td>
		</tr>
	</table>
</xsl:template>

<xsl:template name="clinical">
	<table width="100%" border="1">
		<tr>
			<td width="25%">Anatomy:</td>
			<td><input style="width:100%" name="anatomy"></input></td>
		</tr>
		<tr>
			<td>Pathology:</td>
			<td><input style="width:100%" name="pathology"></input></td>
		</tr>
		<tr>
			<td>Organ system:</td>
			<td><input style="width:100%" name="organ-system"></input></td>
		</tr>
		<tr>
			<td>Modalities:</td>
			<td><input style="width:100%" name="modality"></input></td>
		</tr>
		<tr>
			<td>Code:</td>
			<td><input style="width:100%" name="code"></input></td>
		</tr>
	</table>
</xsl:template>

<xsl:template name="image">
	<table width="100%" border="1">
		<tr>
			<td width="25%">Format:</td>
			<td>
				<select style="width:100%" NAME="imageformat">
					<xsl:copy-of select="$options/enumerated-values/imageformat/option"/>
				</select>
			</td>
		</tr>
		<tr>
			<td>Compression:</td>
			<td>
				<select style="width:100%" NAME="imagecompression">
					<xsl:copy-of select="$options/enumerated-values/imagecompression/option"/>
				</select>
			</td>
		</tr>
		<tr>
			<td>Modality:</td>
			<td><input style="width:100%" name="imagemodality"></input></td>
		</tr>
		<tr>
			<td>Anatomy:</td>
			<td><input style="width:100%" name="imageanatomy"></input></td>
		</tr>
		<tr>
			<td>Pathology:</td>
			<td><input style="width:100%" name="imagepathology"></input></td>
		</tr>
	</table>
</xsl:template>

<xsl:template name="patient">
	<table width="100%" border="1">
		<xsl:if test="not(@showptids='no')">
			<tr>
				<td width="25%">Name:</td>
				<td><input style="width:100%" name="pt-name"></input></td>
			</tr>
			<tr>
				<td>ID:</td>
				<td><input style="width:100%" name="pt-id"></input></td>
			</tr>
			<tr>
				<td>MRN:</td>
				<td><input style="width:100%" name="pt-mrn"></input></td>
			</tr>
		</xsl:if>
		<tr>
			<td>Age (min [-max]):</td>
			<td>
				<input style="width:12%" name="years"></input>Years;
				<input style="width:12%" name="months"></input>Months;
				<input style="width:12%" name="weeks"></input>Weeks;
				<input style="width:12%" name="days"></input>Days
			</td>
		</tr>
		<tr>
			<td>Sex:</td>
			<td>
				<select style="width:100%" NAME="pt-sex">
					<xsl:if test="@mode='rad' or @mode=''">
						<xsl:copy-of select="$options/enumerated-values/rad-pt-sex/option"/>
					</xsl:if>
					<xsl:if test="@mode='vet'">
						<xsl:copy-of select="$options/enumerated-values/vet-pt-sex/option"/>
					</xsl:if>
				</select>
			</td>
		</tr>
		<xsl:if test="@mode='rad' or @mode=''">
			<tr>
				<td>Race:</td>
				<td><input style="width:100%" name="pt-race"></input></td>
			</tr>
		</xsl:if>
		<xsl:if test="@mode='vet'">
			<tr>
				<td>Species:</td>
				<td>
					<select style="width:100%" name="pt-species" id="pt-species" onchange="setBreedList()">
						<option value=""/>
						<xsl:for-each select="$species/vet/species">
							<option value="{@name}">
								<xsl:value-of select="@name"/>
							</option>
						</xsl:for-each>
					</select>
				</td>
			</tr>
			<tr>
				<td>Breed:</td>
				<td>
					<select style="width:100%" id="pt-breed" name="pt-breed">
						<option value=""></option>
					</select>
				</td>
			</tr>
		</xsl:if>
	</table>
</xsl:template>

<xsl:template name="vet-data">
	<xsl:if test="@mode='vet'">
		<script>
		var breeds = new Array(
			new Array(""),
			<xsl:for-each select="$species/vet/species">
				new Array(
					<xsl:for-each select="breed">
						<xsl:text>"</xsl:text>
						<xsl:value-of select="@name"/>
						<xsl:text>"</xsl:text>
						<xsl:if test="position()!=last()">,</xsl:if>
					</xsl:for-each>
				)<xsl:if test="position()!=last()">,</xsl:if>
			</xsl:for-each>
		);
		</script>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>
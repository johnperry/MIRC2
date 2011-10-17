<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:preserve-space elements="*"/>

<xsl:param name="options"/>
<xsl:param name="species"/>

<xsl:template match="*|@*|text()">
	<xsl:copy>
		<xsl:apply-templates select="*|@*|text()"/>
	</xsl:copy>
</xsl:template>

<xsl:template match="/mirc">
<html>

<head>
	<title><xsl:value-of select="@sitename"/></title>
	<link rel="stylesheet" href="/JSMenu.css" type="text/css"/>
	<link rel="stylesheet" href="/JSPopup.css" type="text/css"/>
	<link rel="stylesheet" href="/query/ClassicUI.css" type="text/css"/>
	<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSAJAX.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSUser.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSCookies.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSMenu.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSLoginPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/QueryServicePopups.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/ClassicUIMenus.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/ClassicUI.js">;</script>
	<xsl:call-template name="params"/>
	<xsl:call-template name="vet-data"/>
</head>

<body>
	<xsl:call-template name="header"/>

	<div id="menuBar">x</div>

	<div class="main">
		<div id="pagetitle" class="pagetitle">Search MIRC</div>
		<div class="selectlibraries">
			<input type="button"
				   onclick="showServersPopup();"
				   value="Select Libraries to Search"/>
		</div>

		<form name="queryform" id="queryform" action="" method="POST" target="_self" accept-charset="UTF-8">
			<xsl:call-template name="maincontent"/>
			<div class="search">
				<input id="go" type="submit" value="Go"
						onclick="setStatusLine('Searching...');setCookies();setServerIDs();">
				</input>
			</div>
			<input id="server" type="hidden" name="server" value=""/>
		</form>
		<xsl:call-template name="news"/>
	</div>
</body>

</html>
</xsl:template>

<xsl:template name="header">
<div class="header" style="background:url(/query/{@masthead}); background-repeat: no-repeat;">
	<xsl:if test="@showsitename='yes'">
		<div class="sitename">
			<span><xsl:value-of select="@sitename"/></span>
		</div>
	</xsl:if>
	<div class="sitelogo" style="height:{@mastheadheight}">&#160;</div>
</div>
</xsl:template>

<xsl:template name="maincontent">
<div class="maincontent" id="maincontent">
	<table width="90%">
		<tr>
			<td align="center" valign="top" style="height:30px; width:75%;">
				<xsl:call-template name="page-buttons"/>
			</td>
			<td valign="top" rowspan="2"
				style="font-family:sans-serif;font-size:10pt; padding-left:10px;padding-top:15;">
				<xsl:call-template name="query-modifiers"/>
			</td>
		</tr>
		<tr>
			<td valign="top"><xsl:call-template name="query-pages"/></td>
			<td/>
		</tr>
	</table>
</div>
</xsl:template>

<xsl:template name="news">
	<xsl:choose>
		<xsl:when test="news/image">
			<div class="news" id="news">
				<a href="{news/url}" target="cod">
					<img src="{news/image}" width="128"/>
				</a>
				<xsl:text> </xsl:text>
				<a href="{news/url}" target="cod">Case of the Day</a>
			</div>
		</xsl:when>
		<xsl:when test="news/title">
			<div class="news" id="news">
				<br/>Today's Interesting Document:<br/>
				<a href="{news/url}" target="cod">
					<xsl:value-of select="news/title"/>
				</a>
			</div>
		</xsl:when>
	</xsl:choose>
</xsl:template>

<xsl:template name="query-modifiers">
	<input type="checkbox" name="unknown" id="unknown" value="yes"
		title="Format documents as unknown cases">Display as unknowns</input>
	<br />
	<input type="checkbox" name="showimages" id="showimages" value="yes"
		title="Show images in query results">Show images in results</input>
	<br />
	<input type="checkbox" name="casenavigator" id="casenavigator" value="yes"
		title="Display results in the Case Navigator">Case navigator</input>
	<br />
	<input type="checkbox" name="randomize" id="randomize" value="yes"
		title="Randomly order results in the Case Navigator">Randomize results</input>
	<br />
	<input type="checkbox" name="icons" id="icons" value="no"
		title="Hide the icon images in MSTF and Tab displays">Suppress icon images</input>
	<br/>
	<div class="formatselect">
		<select name="orderby" id="orderby" title="Select the order of query results">
			<option value="lmdate" selected="">Last modified date</option>
			<option value="pubdate">Creation date</option>
			<option value="title">Document title</option>
		</select>
	</div>
	<div class="formatselect">
		<select name="maxresults" id="maxresults" title="Choose the maximum number of results per site">
			<option value="10">10 results/site</option>
			<option value="25" selected="">25 results/site</option>
			<option value="50">50 results/site</option>
			<option value="100">100 results/site</option>
			<option value="500">500 results/site</option>
		</select>
	</div>
	<div class="formatselect">
		<select name="display" id="display" title="Choose the format in which documents will be displayed">
			<option value="" selected="">Document format</option>
			<option value="page">Page format</option>
			<option value="tab">Tab format</option>
			<option value="mstf">MSTF format</option>
		</select>
	</div>
	<div class="formatselect">
		<select name="bgcolor" id="bgcolor" title="Choose the background shade for display">
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
			<td><input style="width:100%" name="document" id="freetext"></input></td>
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
			<td>Category:</td>
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

<xsl:template name="params">
	<script>
		var downloadenb = "<xsl:value-of select="@downloadenb"/>";
		var mode = "<xsl:value-of select="@mode"/>";
		var version = "<xsl:value-of select="@version"/>";
		var date = "<xsl:value-of select="@date"/>";
		var serverURL = "<xsl:value-of select="@siteurl"/>";
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
		var disclaimerURL = '<xsl:value-of select="@disclaimerurl"/>';
		var sessionPopup = '<xsl:value-of select="@popup"/>';
	</script>
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
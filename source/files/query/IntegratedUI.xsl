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
	<link rel="stylesheet" href="/JSPopup.css" type="text/css"/>
	<link rel="stylesheet" href="/JSPage.css" type="text/css"/>
	<link rel="stylesheet" href="/query/IntegratedUI.css" type="text/css"/>
	<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSAJAX.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSUser.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSCookies.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSPrefs.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSTree.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSLoginPopup.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSPage.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/JSSplitPane.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/QueryServicePopups.js">;</script>
	<script language="JavaScript" type="text/javascript" src="/query/IntegratedUI.js">;</script>
	<xsl:call-template name="params"/>
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
</body>

</html>
</xsl:template>

<xsl:template name="header">
<div class="header">
	<div class="headerLeft">
		<span class="sitename"><xsl:value-of select="@sitename"/></span>
	</div>
	<div class="headerRight">

		<div style="float:right; margin-top:7px;">
			<xsl:text>&#160;&#160;</xsl:text>
			<input type="button" value="Search" onclick="repeatSearch();"/>
			<xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
			<span class="headerText">Welcome </span>
			<span class="headerText" id="usernameSpan">&#160;</span>
			<xsl:text>&#160;&#160;</xsl:text>
			<span class="headerText" id="myAccount">
				<xsl:text>|&#160;&#160;</xsl:text>
				<a href="/prefs">My Account</a>
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
			<p><a href="javascript:showServersPopup();">Select libraries</a></p>
		</div>

		<div style="float:right; margin-top:7px;">
			<input type="button" value="Show all" onclick="clearModifiers();"/>
			<xsl:text>&#160;&#160;</xsl:text>
		</div>

	</div>
</div>
</xsl:template>

<xsl:template name="leftpane">
	<div id="Cases">
		<!--<div class="L1">Cases</div>-->
		<div class="L2" id="MyDocuments"><a href="javascript:queryMine();">My Cases</a><br/></div>
		<div class="L2" id="AllDocuments"><a href="javascript:queryAll();">All Cases</a><br/></div>

		<div class="L2x" id="ApprovalQueue"><a href="javascript:approvalQueue();">Approval Queue</a><br/></div>

		<div id="Conferences">
			<div class="L2x"><a href="/confs">Conferences</a></div>
			<div id="confs">;</div>
		</div>

		<div id="FileCabinets">
			<div class="L2x"><a href="/files">File Cabinets</a><br/></div>
			<div id="cabs">;</div>
		</div>

		<div class="L2x" id="CaseOfTheDay"><a href="{news/url}" target="shared">Case of the Day</a><br/></div>
	</div>

	<div id="AuthorTools">
		<hr/>
		<!--<div class="L1">Author Tools</div>-->
		<div class="L2"><a href="/bauth/ss1">Basic Author Tool</a><br/></div>
		<div class="L2"><a href="/aauth/ss1">Advanced Author Tool</a><br/></div>
		<div class="L2"><a href="javascript:submitService('ss1')"
			title="Use this service to insert cases exported from another site">Submit Service</a><br/></div>
		<div class="L2"><a href="/zip/ss1"
			title="Use this service to create cases from a zip file of images">Zip Service</a><br/></div>
	</div>

	<div id="Admin">
		<hr/>
		<!--<div class="L1">Admin Tools</div>-->
		<div class="L2"><a href="/users?home=/query">User Manager</a><br/></div>
		<div class="L2"><a href="/qsadmin">Query Service</a><br/></div>
		<div class="L2"><a href="/fsadmin?home=/query">File Service</a><br/></div>
		<div class="L2"><a href="/ssadmin">Storage Service</a><br/></div>
		<div class="L2"><a href="/daconfig?home=/query">DICOM Anonymizer</a><br/></div>
		<div class="L2"><a href="/script">Script Editor</a><br/></div>
		<div class="L2"><a href="/lookup">Lookup Table Editor</a><br/></div>
		<div class="L2"><a href="/system">System Properties</a><br/></div>
		<div class="L2"><a href="/configuration">CTP Configuration</a><br/></div>
		<div class="L2"><a href="/status">CTP Status</a><br/></div>
		<div class="L2"><a href="/logs?home=/query">Log Viewer</a><br/></div>
		<div class="L2"><a href="/level">Logger Levels</a><br/></div>
	</div>
</xsl:template>

<xsl:template name="rightpane">
	No results.
</xsl:template>

<xsl:template name="footer">
<div class="footer">
	<div class="footerRight">
		&#160;&#160;<a href="/query?UI=classic">Classic UI</a>&#160;&#160;
		|&#160;&#160;<a href="javascript:showModifiersPopup();">Query Modifiers</a>&#160;&#160;
		|&#160;&#160;<a href="javascript:showHelpPopup();">Help</a>&#160;&#160;
		|&#160;&#160;<a href="javascript:showAboutPopup();">About MIRC</a>&#160;&#160;
		|&#160;&#160;<a href="http://mircwiki.rsna.org/index.php?title=MIRC_Articles" target="wiki">RSNA MIRC Wiki</a>&#160;&#160;
	</div>
	<img src="/query/RSNA.jpg"/>
	<span class="productname">TEACHING FILE SYSTEM</span>
</div>
</xsl:template>

<xsl:template name="params">
	<script>
		var mode = "<xsl:value-of select="@mode"/>";
		var version = "<xsl:value-of select="@version"/>";
		var date = "<xsl:value-of select="@date"/>";
		var serverURL = "<xsl:value-of select="@siteurl"/>";
		var selectedLocalLibrary = 0;
		var localLibraries = <xsl:text>new Array(</xsl:text>
			<xsl:for-each select="Libraries/Library[@local='yes' and @enabled='yes']">
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
			<xsl:for-each select="Libraries/Library[@enabled='yes']">
				new Library(
					"<xsl:value-of select="@enabled"/>",
					"<xsl:value-of select="@address"/>",
					"<xsl:value-of select="normalize-space(title)"/>",
					"<xsl:value-of select="@local"/>"
				<xsl:text>)</xsl:text>
				<xsl:if test="position() != last()">,</xsl:if>
			</xsl:for-each>
			);
		var disclaimerURL = '<xsl:value-of select="@disclaimerurl"/>';
		var codURL = '<xsl:value-of select="news/url"/>';
		var sessionPopup = '<xsl:value-of select="@popup"/>';
	</script>
</xsl:template>

</xsl:stylesheet>
<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="ssid"/>
<xsl:param name="result"/>
<xsl:variable name="siteurl" select="/mirc/@siteurl"/>

<xsl:template match="/mirc">
	<html>
		<head>
			<title>Submit Service - <xsl:value-of select="$ssid"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/submit/SubmitService.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/submit/SubmitService.js">;</script>
			<script>
				var ui = '<xsl:value-of select="$ui"/>';
			</script>
		</head>
		<body>
			<xsl:if test="$ui='classic'">
				<div class="closebox">
					<img src="/icons/home.png"
						 onclick="window.open('/query','_self');"
						 title="Return to the home page"/>
					<br/>
					<img src="/icons/save.png"
						 onclick="save();"
						 title="Submit the MIRCdocument"/>
				</div>
			</xsl:if>

			<h1><xsl:value-of select="Libraries/Library[@id=$ssid]/title"/> (<xsl:value-of select="$ssid"/>)</h1>
			<h2>Submit Service</h2>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" enctype="multipart/form-data" >
				<input type="hidden" name="ui" value="{$ui}"/>

				<p class="note">
					This page may be used by authors to submit MIRCdocuments to this MIRC site.
					A submission is required to be encapsulated in a zip file as specified in
					<a href="http://mircwiki.rsna.org/index.php?title=The_MIRC_Protocol_for_Document_Exchange" target="wiki">
					The MIRC Protocol for Document Exchange</a>.
				</p>

				<p class="center">
					Select a MIRCdocument zip file: <input class="file" name="file" type="file"/>
				</p>

				<p class="center">
					<input type="button" value="Submit the MIRCdocument" onclick="save();"/>
				</p>

				<xsl:if test="$result">
					<h3>Submission Results</h3>
					<xsl:call-template name="lines">
						<xsl:with-param name="text" select="$result"/>
					</xsl:call-template>
				</xsl:if>
			</form>
		</body>
	</html>
</xsl:template>

<xsl:template name="lines">
	<xsl:param name="text"/>
	<xsl:if test="contains($text,'|')">
		<xsl:variable name="first" select="substring-before($text,'|')"/>
		<xsl:choose>
		<xsl:when test="starts-with($first,'@')">
			<p class="link">
				<xsl:variable name="href" select="substring-after($first,'@')"/>
				<a href="{$href}" target="doc"><xsl:value-of select="$href"/></a>
			</p>
		</xsl:when>
		<xsl:otherwise>
			<p class="normal">
				<xsl:value-of select="$first"/>
			</p>
		</xsl:otherwise>
		</xsl:choose>
		<xsl:call-template name="lines">
			<xsl:with-param name="text" select="substring-after($text,'|')"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>

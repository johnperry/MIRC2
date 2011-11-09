<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="ssid"/>
<xsl:param name="result"/>
<xsl:param name="config"/>
<xsl:variable name="localLibraries" select="$config/mirc/Libraries/Library[@local='yes' and @enabled='yes' and @subenb='yes']"/>

<xsl:template match="/mirc">
	<html>
		<head>
			<title>Submit Service - <xsl:value-of select="$ssid"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/submit/SubmitService.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<xsl:call-template name="data"/>
			<script language="JavaScript" type="text/javascript" src="/submit/SubmitService.js">;</script>
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

			<h1>Submit Service</h1>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" enctype="multipart/form-data" >
				<input type="hidden" name="ui" value="{$ui}"/>

				<p class="note">
					This page may be used by authors to submit MIRCdocuments to this MIRC site.
					A submission is required to be encapsulated in a zip file as specified in
					<a href="http://mircwiki.rsna.org/index.php?title=The_MIRC_Protocol_for_Document_Exchange" target="wiki">
					The MIRC Protocol for Document Exchange</a>.
				</p>

				<p class="instruction">Select a MIRCdocument zip file:</p>
				<p class="centered"><input class="file" name="file" type="file"/></p>

				<xsl:if test="count($localLibraries)&gt;1">
					<p class="instruction">Select the library in which to store the MIRCdocument:</p>
					<p class="centered">
						<select id="libSelect" name="libSelect">
							<xsl:for-each select="$localLibraries">
								<option value="{@id}">
									<xsl:if test="@id=$ssid">
										<xsl:attribute name="selected">true</xsl:attribute>
									</xsl:if>
									<xsl:value-of select="title"/>
								</option>
							</xsl:for-each>
						</select>
					</p>
				</xsl:if>

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

<xsl:template name="data">
	<script language="JavaScript" type="text/javascript">
		var ui = '<xsl:value-of select="$ui"/>';
		<xsl:choose>
			<xsl:when test="$localLibraries[@id=$ssid]">
				var ssid = '<xsl:value-of select="$ssid"/>';
			</xsl:when>
			<xsl:otherwise>
				var ssid = '<xsl:value-of select="$localLibraries[position()=1]/@id"/>';
			</xsl:otherwise>
		</xsl:choose>
	</script>
</xsl:template>

</xsl:stylesheet>

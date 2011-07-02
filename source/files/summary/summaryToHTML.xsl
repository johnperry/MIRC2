<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="ssid"/>
<xsl:param name="show-titles"/>
<xsl:param name="show-names"/>
<xsl:param name="show-dates"/>
<xsl:param name="show-access"/>

<xsl:param name="context">
	<xsl:value-of select="/IndexSummary/Context"/>
</xsl:param>

<xsl:template match="/IndexSummary">
	<html>
		<head>
			<title>Author Summary Report - <xsl:value-of select="$ssid"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/summary/summaryToHTML.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/summary/summaryToHTML.js">;</script>
			<script>var ssid = '<xsl:value-of select="$ssid"/>';</script>
		</head>
		<body>
		<div class="closebox">
			<img src="/icons/home.png"
				 onclick="window.open('/summary/{$ssid}','_self');"
				 title="Return to the summary request page"/>
		</div>
			<center>
				<br/>
				<h1>Author Summary Report - <xsl:value-of select="$ssid"/></h1>
				<h2><xsl:value-of select="StorageService"/></h2>
				<table border="0">
					<tr>
						<td>Starting date (inclusive):</td>
						<td><xsl:value-of select="StartDate"/></td>
					</tr>
					<tr>
						<td>Ending date (inclusive):</td>
						<td><xsl:value-of select="EndDate"/></td>
					</tr>
					<tr>
						<td>Number of indexed documents:</td>
						<td><xsl:value-of select="IndexedDocs"/></td>
					</tr>
					<tr>
						<td>Number of selected documents:</td>
						<td><xsl:value-of select="DocsInRange"/></td>
					</tr>
					<tr>
						<td>Number of unowned selected documents:</td>
						<td><xsl:value-of select="UnownedDocs"/></td>
					</tr>
				</table>
				<br/>
				<table border="1">
					<tr>
						<th><br/><br/>Username</th>
						<th><br/>Number of <br/>Documents</th>
						<th><br/>Selected <br/>Documents</th>
						<th>Selected <br/>Public <br/>Documents</th>
						<xsl:if test="$show-titles = 'yes'">
							<th>Documents</th>
						</xsl:if>
					</tr>
					<xsl:apply-templates select="Owner"/>
				</table>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template match="Owner">
	<tr>
		<td><xsl:value-of select="username"/></td>
		<td class="n"><xsl:value-of select="IndexedDocs"/></td>
		<td class="n"><xsl:value-of select="DocsInRange"/></td>
		<td class="n"><xsl:value-of select="PublicDocsInRange"/></td>
		<xsl:if test="$show-titles = 'yes'">
			<td><ol>
				<xsl:apply-templates select="doc"/>
			</ol></td>
		</xsl:if>
	</tr>
</xsl:template>

<xsl:template match="doc">
	<li>
		<a href="{$context}/{path}" target="_blank">
			<xsl:value-of select="title"/>
		</a>
		<xsl:if test="$show-names = 'yes'">
			<xsl:apply-templates select="name"/>
		</xsl:if>
		<xsl:if test="$show-dates = 'yes'">
			<xsl:apply-templates select="pubdate"/>
		</xsl:if>
		<xsl:if test="$show-access = 'yes'">
			<xsl:apply-templates select="access"/>
		</xsl:if>
	</li>
</xsl:template>

<xsl:template match="name">
	<xsl:if test="normalize-space(.)">
		<br/>&#160;&#160;&#160;&#160;&#160;&#160;<xsl:value-of select="."/>
	</xsl:if>
</xsl:template>

<xsl:template match="pubdate">
	<xsl:if test="normalize-space(.)">
		<br/>&#160;&#160;&#160;&#160;&#160;&#160;Pub.Date: <xsl:value-of select="."/>
	</xsl:if>
</xsl:template>

<xsl:template match="access">
	<xsl:if test="normalize-space(.)">
		<br/>&#160;&#160;&#160;&#160;&#160;&#160;Access: <xsl:value-of select="."/>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>

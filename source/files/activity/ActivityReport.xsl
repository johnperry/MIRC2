<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="report"/>

<xsl:param name="context">
	<xsl:value-of select="/IndexSummary/Context"/>
</xsl:param>

<xsl:template match="/ActivityReport">
	<html>
		<head>
			<title>Activity Report - <xsl:value-of select="@name"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/activity/ActivityReport.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
		</head>
		<body>
			<center>
				<br/>
				<h1>Activity Report</h1>
				<h2><xsl:value-of select="@name"/></h2>
				<table border="0">
					<tr>
						<td>Site ID:</td>
						<td><xsl:value-of select="@id"/></td>
					</tr>
					<tr>
						<td>Site URL:</td>
						<td><xsl:value-of select="@url"/></td>
					</tr>
					<tr>
						<td>Number of users:</td>
						<td><xsl:value-of select="@users"/></td>
					</tr>
					<tr>
						<td>TFS version:</td>
						<td><xsl:value-of select="@version"/></td>
					</tr>
				</table>
				<br/>
				<table border="1">
					<tr>
						<th><br/><br/>Library</th>
						<th><br/><br/>ID</th>
						<th><br/>Number of<br/>Documents</th>
						<th>Advanced<br/>Author<br/>Service</th>
						<th>Basic<br/>Author<br/>Service</th>
						<th><br/>Submit<br/>Service</th>
						<th><br/>Zip<br/>Service</th>
						<th><br/>DICOM<br/>Service</th>
						<th><br/>TCE<br/>Service</th>
						<th><br/>MyRSNA<br/>Uploads</th>
						<th><br/>Documents<br/>Displayed</th>
					</tr>
					<xsl:apply-templates select="Library">
						<xsl:sort select="@title"/>
					</xsl:apply-templates>
					<xsl:call-template name="totals"/>
				</table>
				<br/>
				<p>
					<input type="button" value="Format as XML"
						title="Export this report as XML"
						onclick="window.open('/activity?format=xml', '_self');"/>
				</p>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template match="Library">
	<tr>
		<td><xsl:value-of select="@title"/></td>
		<td class="n"><xsl:value-of select="@ssid"/></td>
		<td class="n"><xsl:value-of select="@docs"/></td>
		<td class="n"><xsl:value-of select="@aauth"/></td>
		<td class="n"><xsl:value-of select="@bauth"/></td>
		<td class="n"><xsl:value-of select="@sub"/></td>
		<td class="n"><xsl:value-of select="@zip"/></td>
		<td class="n"><xsl:value-of select="@dcm"/></td>
		<td class="n"><xsl:value-of select="@tce"/></td>
		<td class="n"><xsl:value-of select="@myrsna"/></td>
		<td class="n"><xsl:value-of select="@storage"/></td>
	</tr>
</xsl:template>

<xsl:template name="totals">
	<xsl:variable name="libs" select="Library"/>
	<tr>
		<td><b>Totals:</b></td>
		<td/>
		<td class="t"><xsl:value-of select="sum($libs/@docs)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@aauth)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@bauth)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@sub)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@zip)"/></td>
		<td class="t"><xsl:value-of select="sum($libs//@dcm)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@tce)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@myrsna)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@storage)"/></td>
	</tr>
</xsl:template>

</xsl:stylesheet>

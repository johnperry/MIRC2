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
						<th rowspan="3">Library</th>
						<th rowspan="3">ID</th>
						<th rowspan="3">Documents<br/>Stored</th>
						<th colspan="8">
							Activity in the Last 30 Days
						</th>
					</tr>
					<tr>
						<th colspan="6">New Documents Created</th>
						<th rowspan="2">MyRSNA<br/>Uploads</th>
						<th rowspan="2">Document<br/>Display<br/>Requests</th>
					</tr>
					<tr>
						<th>Advanced<br/>Author<br/>Service</th>
						<th>Basic<br/>Author<br/>Service</th>
						<th><br/>Submit<br/>Service</th>
						<th><br/>Zip<br/>Service</th>
						<th><br/>DICOM<br/>Service</th>
						<th><br/>TCE<br/>Service</th>
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

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@docs"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@aauth"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@bauth"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@sub"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@zip"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@dcm"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@tce"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@myrsna"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@storage"/>
		</xsl:call-template>

	</tr>
</xsl:template>

<xsl:template name="totals">
	<xsl:variable name="libs" select="Library"/>
	<tr>
		<td class="tl">Totals:</td>
		<td class="tl"/>
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

<xsl:template name="suppress-zero">
	<xsl:param name="n"/>
	<xsl:if test="$n=0">
		<td class="n"><xsl:text>&#160;</xsl:text></td>
	</xsl:if>
	<xsl:if test="not($n=0)">
		<td class="n"><xsl:value-of select="$n"/></td>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>

<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:template match="/SummaryReport">
	<html>
		<head>
			<title>Activity Summary Report</title>
			<link rel="stylesheet" href="/BaseStyles.css" type="text/css"/>
			<link rel="Stylesheet" type="text/css" media="all" href="/activity/ActivityReport.css"></link>
		</head>
		<body>
			<center>
				<br/>
				<h1>Activity Summary Report</h1>
				<xsl:apply-templates select="ActivitySummary">
					<xsl:sort select="@name"/>
				</xsl:apply-templates>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template match="ActivitySummary">
	<h2><xsl:value-of select="@name"/></h2>
	<table border="0">
		<tr>
			<td>Site ID:</td>
			<td><xsl:value-of select="@siteID"/></td>
		</tr>
		<tr>
			<td>Site URL:</td>
			<td><xsl:value-of select="@url"/></td>
		</tr>
		<tr>
			<td>Site IP address:</td>
			<td><xsl:value-of select="@ip"/></td>
		</tr>
		<tr>
			<td>Number of users:</td>
			<td><xsl:value-of select="@users"/></td>
		</tr>
		<tr>
			<td>TFS version:</td>
			<td><xsl:value-of select="@version"/></td>
		</tr>
		<tr>
			<td>Admin email:</td>
			<td><xsl:value-of select="@email"/></td>
		</tr>
	</table>

	<table border="1">
		<tr>
			<th rowspan="3">Month</th>
			<th rowspan="3">Documents<br/>Stored</th>
		</tr>
		<tr>
			<th colspan="6">New Documents Created</th>
			<th rowspan="2">MyRSNA<br/>Uploads</th>
			<th rowspan="2">Slides<br/>Exports</th>
			<th rowspan="2">Document<br/>Display<br/>Requests</th>
			<th rowspan="2">Documents<br/>Displayed</th>
			<th rowspan="2">Active<br/>Users</th>
		</tr>
		<tr>
			<th>Advanced<br/>Author<br/>Service</th>
			<th>Basic<br/>Author<br/>Service</th>
			<th><br/>Submit<br/>Service</th>
			<th><br/>Zip<br/>Service</th>
			<th><br/>DICOM<br/>Service</th>
			<th><br/>TCE<br/>Service</th>
		</tr>
		<xsl:apply-templates select="MonthlyReport"/>
	</table>
	<br/>
</xsl:template>

<xsl:template match="MonthlyReport">
	<xsl:variable name="rearrangedDate">
		<xsl:value-of select="substring(@date,5,2)"/>/<xsl:value-of select="substring(@date,1,4)"/>
	</xsl:variable>
	<tr>
		<td><xsl:value-of select="$rearrangedDate"/></td>

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
			<xsl:with-param name="n" select="@slides"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@storage"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@docsDisplayed"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@activeUsers"/>
		</xsl:call-template>
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

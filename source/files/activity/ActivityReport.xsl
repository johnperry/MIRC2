<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="report"/>

<xsl:template match="/ActivityReport">
	<html>
		<head>
			<title>Activity Report - <xsl:value-of select="@name"/></title>
			<link rel="stylesheet" href="/BaseStyles.css" type="text/css"/>
			<link rel="stylesheet" href="/JSPopup.css" type="text/css"/>
			<link rel="Stylesheet" type="text/css" media="all" href="/activity/ActivityReport.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/activity/SummaryPopup.js">;</script>
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
					<tr>
						<td>Admin email:</td>
						<td><xsl:value-of select="@email"/></td>
					</tr>
				</table>

				<xsl:apply-templates select="MonthlyReport"/>

				<br/>

				<p>
					<input type="button" value="Send the summary report to the RSNA"
						onclick="showSubmissionPopup('http://mirc.rsna.org/activity/submit?report={$report}');"/>
					<br/><br/>
					<input type="button" value="Format the full report as XML"
						onclick="window.open('/activity?format=xml', '_self');"/>
					&#160;&#160;
					<input type="button" value="Format the summary report as XML"
						onclick="window.open('/activity?format=xml&amp;type=summary', '_self');"/>
				</p>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template match="MonthlyReport">
	<br/>
	<xsl:variable name="rearrangedDate">
		<xsl:value-of select="substring(@date,5,2)"/>/<xsl:value-of select="substring(@date,1,4)"/>
	</xsl:variable>
	<h2 class="MonthlyReport">Report for <xsl:value-of select="$rearrangedDate"/></h2>
	<table border="1">
		<tr>
			<th rowspan="3">Library</th>
			<th rowspan="3">ID</th>
			<th rowspan="3">Documents<br/>Stored</th>
			<th colspan="11">
				Activity During <xsl:value-of select="$rearrangedDate"/>
			</th>
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
		<xsl:apply-templates select="Library">
			<xsl:sort select="@title"/>
		</xsl:apply-templates>
		<xsl:call-template name="totals"/>
	</table>
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
			<xsl:with-param name="n" select="@slides"/>
		</xsl:call-template>

		<xsl:call-template name="suppress-zero">
			<xsl:with-param name="n" select="@storage"/>
		</xsl:call-template>

		<xsl:call-template name="docsDisplayed"/>

		<xsl:call-template name="activeUsers"/>

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
		<td class="t"><xsl:value-of select="sum($libs/@dcm)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@tce)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@myrsna)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@slides)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@storage)"/></td>
		<td class="t"><xsl:value-of select="sum($libs/@docsDisplayed)"/></td>
		<td class="t"><xsl:value-of select="@activeUsers"/></td>
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

<xsl:template name="docsDisplayed">
	<xsl:variable name="n" select="@docsDisplayed"/>
	<xsl:if test="$n=0">
		<td class="n"><xsl:text>&#160;</xsl:text></td>
	</xsl:if>
	<xsl:if test="not($n=0)">
		<td class="n">
			<a href="/activity/documents?date={../@date}&amp;ssid={@ssid}" target="details">
				<xsl:value-of select="$n"/>
			</a>
		</td>
	</xsl:if>
</xsl:template>

<xsl:template name="activeUsers">
	<xsl:variable name="n" select="@activeUsers"/>
	<xsl:if test="$n=0">
		<td class="n"><xsl:text>&#160;</xsl:text></td>
	</xsl:if>
	<xsl:if test="not($n=0)">
		<td class="n">
			<a href="/activity/users?date={../@date}&amp;ssid={@ssid}" target="details">
				<xsl:value-of select="$n"/>
			</a>
		</td>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>

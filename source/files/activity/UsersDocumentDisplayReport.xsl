<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="date"/>
<xsl:param name="ssid"/>

<xsl:variable name="prefs" select="/UsersDocumentDisplayList/Preferences/User"/>

<xsl:template match="/UsersDocumentDisplayList">
	<xsl:apply-templates select="Library"/>
</xsl:template>

<xsl:template match="Library">
	<xsl:variable name="rearrangedDate">
		<xsl:value-of select="substring(@date,5,2)"/>/<xsl:value-of select="substring(@date,1,4)"/>
	</xsl:variable>
	<html>
		<head>
			<title>Display Activity Report - <xsl:value-of select="@name"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/activity/ActivityReport.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
		</head>
		<body>
			<center>
				<br/>
				<h1>Display Activity Report</h1>
				<h2><xsl:value-of select="@title"/></h2>
				<h2><xsl:value-of select="$rearrangedDate"/></h2>
				<table border="1">
					<xsl:call-template name="headings"/>
					<xsl:apply-templates select="User">
						<xsl:sort select="@n" order="descending" data-type="number"/>
					</xsl:apply-templates>
				</table>
				<br/>
				<p>
					<input type="button" value="Format this report as XML"
						onclick="window.open('/activity/users/xml?date={$date}&amp;ssid={$ssid}', '_self');"/>
				</p>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template name="headings">
	<tr>
		<th>Username</th>
		<th>Name</th>
		<th>Documents<br/>Displayed</th>
	</tr>
</xsl:template>

<xsl:template match="User">
	<tr>
		<td>
			<xsl:value-of select="@username"/>
		</td>
		<td>
			<xsl:variable name="un" select="@username"/>
			<xsl:value-of select="$prefs[@username=$un]/@name"/>
		</td>
		<td class="right">
			<a href="/activity/user?ssid={../@ssid}&amp;date={../@date}&amp;username={@username}" target="userlist">
				<xsl:value-of select="@n"/>
			</a>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>

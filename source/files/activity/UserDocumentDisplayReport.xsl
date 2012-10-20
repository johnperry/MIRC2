<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:template match="/UserDocumentDisplayList">
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
				<h2>
					<xsl:value-of select="../User/@name"/>
					<xsl:text> (</xsl:text>
					<xsl:value-of select="../User/@username"/>
					<xsl:text>)</xsl:text>
				</h2>
				<table border="1">
					<xsl:apply-templates select="User/Document"/>
				</table>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template match="Document">
	<tr>
		<td class="right"><xsl:number/></td>
		<td>
			<a href="{@docKey}" targer="document">
				<xsl:value-of select="@title"/>
			</a>
		</td>
	</tr>
</xsl:template>

</xsl:stylesheet>

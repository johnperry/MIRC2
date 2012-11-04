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
				<table border="1">
					<xsl:call-template name="headings"/>
					<xsl:apply-templates select="Document">
						<xsl:sort select="@n" order="descending" data-type="number"/>
					</xsl:apply-templates>
				</table>
			</center>
		</body>
	</html>
</xsl:template>

<xsl:template name="headings">
	<tr>
		<th>Document Title</th>
		<th>Count</th>
	</tr>
</xsl:template>

<xsl:template match="Document">
	<tr>
		<td>
			<a href="{@docKey}" targer="document">
				<xsl:value-of select="@title"/>
			</a>
		</td>
		<td class="right"><xsl:value-of select="@n"/></td>
	</tr>
</xsl:template>

</xsl:stylesheet>

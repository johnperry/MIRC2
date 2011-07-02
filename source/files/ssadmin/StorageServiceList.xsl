<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/list">
	<xsl:variable name="ssid" select="Library/@id"/>
	<xsl:variable name="line" select="@line"/>
	<html>
		<head>
			<title>Storage Service List - <xsl:value-of select="$ssid"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/ssadmin/StorageServiceList.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/ssadmin/StorageServiceList.js">;</script>
		</head>
		<body>

			<div class="closebox">
				<img src="/icons/home.png"
					 onclick="window.open('/ssadmin','_self');"
					 title="Return to the admin page"/>
			</div>

			<h1><xsl:value-of select="Library/title"/> (<xsl:value-of select="$ssid"/>)</h1>
			<h2>Index Listing</h2>

			<center>

			<xsl:choose>
			<xsl:when test="doc">

				<table border="1">
				<xsl:for-each select="doc">
					<xsl:variable name="here"><xsl:number/></xsl:variable>
					<tr>
						<td class="number">
							<xsl:if test="$here=$line">
								<xsl:attribute name="id">here</xsl:attribute>
							</xsl:if>
							<xsl:value-of select="$here"/>
						</td>
						<td class="delete">
							<input type="button" value="Delete"
									onclick="window.open('/ssadmin/deleteDocument/{$ssid}/{@path}?line={$here}','_self');"/>
						</td>
						<td class="edit">
							<input type="button" value="Edit"/>
						</td>
						<td class="title">
							<a href="/storage/{$ssid}/{@path}" target="doc">
								<xsl:value-of select="title"/>
							</a>
						</td>
						<td class="dates">
							PD:<xsl:value-of select="pubdate"/>
							<br/>
							LM:<xsl:value-of select="lmdate"/>
						</td>
					</tr>
				</xsl:for-each>
				</table>

			</xsl:when>
			<xsl:otherwise>
				<p>The index is empty.</p>
			</xsl:otherwise>
			</xsl:choose>

			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
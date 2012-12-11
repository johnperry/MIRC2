<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="upload"/>
<xsl:param name="admin"/>

<xsl:template match="/files">
	<html>
		<head>
			<title>Download Software</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/download/DownloadServlet.css"></link>
		</head>
		<body>
			<xsl:if test="$ui='classic'">
				<div class="closebox">
					<img src="/icons/home.png"
						 onclick="window.open('/','_self');"
						 title="Go to the server home page"/>
				</div>
			</xsl:if>

			<h1>Download Software</h1>

			<center>
				<p class="note">
					The table below provides access to teaching file and clinical trial
					tools developed under the RSNA MIRC project.
				</p>
				<p class="note">
					The MIRC Committee has changed the name of the teaching file system
					from MIRC to TFS. To obtain the latest version for installing a new
					site or upgrading a previous one, click the TFS&#8209;installer.jar
					link in this table.
				</p>
				<br/>
				<table border="1">
					<tr>
						<th>File</th>
						<th>Description</th>
						<th class="size">Size</th>
						<th>Version</th>
					</tr>
					<xsl:for-each select="file">
						<tr>
							<td>
								<a href="/download/{@name}"><xsl:value-of select="@name"/></a>
							</td>
							<td>
								<xsl:if test="@desc">
									<xsl:value-of select="@desc"/>
								</xsl:if>
								<xsl:if test="not(@desc)">&#160;</xsl:if>
							</td>
							<td class="size"><xsl:value-of select="format-number(@size,'#,##0')"/></td>
							<td>
								<xsl:choose>
									<xsl:when test="string-length(normalize-space(@build)) != 0">
										<xsl:value-of select="@build"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="@lastModified"/>
									</xsl:otherwise>
								</xsl:choose>
							</td>
						</tr>
					</xsl:for-each>
				</table>

				<xsl:variable name="footerlinks" select="($upload='yes') or ($admin='yes')"/>
				<xsl:if test="$footerlinks">
					<br/>
					<p class="center">
						<xsl:if test="$upload='yes'">
							<a href="/download/upload?ui={$ui}">Upload a file</a>
							<br/>
						</xsl:if>
						<xsl:if test="($admin='yes') or ($upload='yes')">
							<a href="/download/report" target="report">Report</a>
						</xsl:if>
					</p>
				</xsl:if>
			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="upload"/>

<xsl:template match="/files">
	<html>
		<head>
			<title>Download Service</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/download/DownloadServlet.css"></link>
			<script language="JavaScript" type="text/javascript" src="/download/DownloadServlet.js">;</script>
		</head>
		<body>
			<xsl:if test="$ui='classic'">
				<div class="closebox">
					<img src="/icons/home.png"
						 onclick="window.open('/','_self');"
						 title="Go to the server home page"/>
				</div>
			</xsl:if>

			<h1>Download Service</h1>

			<center>
				<p class="note">
					Please enter your email address in the field below. This field is only used
					to allow the MIRC Committee to track the use of MIRC, CTP, and related software.
				</p>
				<p class="center">
					<input type="text" class="email" name="email" id="email"/>
				</p>
				<table border="1">
					<tr>
						<th>File</th>
						<th>Description</th>
						<th class="size">Size</th>
						<th>Build Time</th>
						<th>Upload Time</th>
					</tr>
					<xsl:for-each select="file">
						<tr>
							<td><a href="javascript:get('{@name}');"><xsl:value-of select="@name"/></a></td>
							<td><xsl:value-of select="@desc"/></td>
							<td class="size"><xsl:value-of select="format-number(@size,'#,##0')"/></td>
							<td><xsl:value-of select="@build"/></td>
							<td><xsl:value-of select="@lastModified"/></td>
						</tr>
					</xsl:for-each>
				</table>

				<xsl:if test="$upload='yes'">
					<br/>
					<p class="center">
						<a href="/download/upload?ui={$ui}">Upload a file</a>
					</p>
				</xsl:if>
			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
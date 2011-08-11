<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/files">
	<html>
		<head>
			<title>Download Service</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/download/DownloadServlet.css"></link>
			<script language="JavaScript" type="text/javascript" src="/download/DownloadServlet.js">;</script>
		</head>
		<body>
			<div class="closebox">
				<img src="/icons/home.png"
					 onclick="window.open('/','_self');"
					 title="Go to the server home page"/>
			</div>

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
						<th>Last Modified</th>
						<th>Build Time</th>
					</tr>
					<xsl:for-each select="file">
						<tr>
							<td><a href="javascript:get('{@name}');"><xsl:value-of select="@name"/></a></td>
							<td><xsl:value-of select="@lastModified"/></td>
							<td><xsl:value-of select="@build"/></td>
						</tr>
					</xsl:for-each>
				</table>
			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
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
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
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

			<h1>Download Software</h1>

			<center>
				<p class="note">
					Please enter your email address and other information in the table below, then click the
					file you wish to download. This information is only used to allow the MIRC Committee to track
					the interest in CTP, TFS, and related software.
				</p>
				<table border="1">
					<tr>
						<td class="rowname">Email:</td>
						<td class="rowdata"><input type="text" class="tabledata" name="email" id="email"/></td>
					</tr>
					<tr>
						<td class="rowname">Name:</td>
						<td class="rowdata"><input type="text" class="tabledata" name="pname" id="pname"/></td>
					</tr>
					<tr>
						<td class="rowname">Institution name:</td>
						<td class="rowdata"><input type="text" class="tabledata" name="iname" id="iname"/></td>
					</tr>
					<tr>
						<td class="rowname">Interest:</td>
						<td class="rowdata">
							<input type="radio" name="interest" value="personal">Personal use</input>
							<br/>
							<input type="radio" name="interest" value="institution">Institutional use</input>
						</td>
					</tr>
					<tr>
						<td class="rowname">Site type:</td>
						<td class="rowdata">
							<input type="radio" name="sitetype" value="public">Public</input>
							<br/>
							<input type="radio" name="sitetype" value="private">Private</input>
						</td>
					</tr>
				</table>
				<p class="note">
					Note: the MIRC Committee recently
					changed the name of the teaching file system from MIRC to TFS. Click TFS&#8209;installer.jar
					to obtain the latest RSNA teaching file software.
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

				<xsl:variable name="footerlinks" select="($upload='yes') or ($admin='yes')"/>
				<xsl:if test="$footerlinks">
					<br/>
					<p class="center">
						<xsl:if test="$upload='yes'">
							<a href="/download/upload?ui={$ui}">Upload a file</a>
							<br/>
						</xsl:if>
						<xsl:if test="$admin='yes'">
							<a href="/download/report" target="report">Report</a>
						</xsl:if>
					</p>
				</xsl:if>
			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
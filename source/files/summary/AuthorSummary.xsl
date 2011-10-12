<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="ssid"/>
<xsl:param name="userIsAdmin"/>
<xsl:param name="today"/>

<xsl:template match="/mirc">

<html>
	<head>
		<title>Author Summary Request - <xsl:value-of select="$ssid"/></title>
		<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
		<link rel="Stylesheet" type="text/css" media="all" href="/summary/AuthorSummary.css"></link>
		<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
		<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
		<script language="JavaScript" type="text/javascript" src="/summary/AuthorSummary.js">;</script>
		<script>
			var ui = '<xsl:value-of select="$ui"/>';
		</script>
	</head>
	<body>
		<xsl:if test="$ui='classic'">
			<div class="closebox">
				<img src="/icons/home.png"
					 onclick="window.open('/query','_self');"
					 title="Return to the home page"/>
				<br/>
				<img src="/icons/save.png"
					 onclick="save();"
					 title="Submit the request"/>
			</div>
		</xsl:if>

		<center>
			<h1>Author Summary Request - <xsl:value-of select="$ssid"/></h1>
			<h2><xsl:value-of select="Libraries/Library[@id=$ssid]/title"/></h2>

			<form id="formID" method="post" action="" accept-charset="UTF-8">
			<input type="hidden" name="ui" value="{$ui}"/>

			<table border="1">

				<xsl:if test="$userIsAdmin='yes'">
					<tr>
						<td>Username (* to select all users):</td>
						<td><input type="text" name="user" value="*"/></td>
					</tr>
				</xsl:if>

				<tr>
					<td>Start date (inclusive, YYYYMMDD):</td>
					<td><input type="text" name="start"/></td>
				</tr>

				<tr>
					<td>End date (inclusive, YYYYMMDD):</td>
					<td><input type="text" name="end" value="{$today}"/></td>
				</tr>

				<tr>
					<td>Show document titles on web page:</td>
					<td><input type="checkbox" name="title" value="yes" checked="true"/></td>
				</tr>

				<tr>
					<td>Show author names on web page:</td>
					<td><input type="checkbox" name="name" value="yes" checked="true"/></td>
				</tr>

				<tr>
					<td>Show creation dates on web page:</td>
					<td><input type="checkbox" name="date" value="yes" checked="true"/></td>
				</tr>

				<tr>
					<td>Show access on web page:</td>
					<td><input type="checkbox" name="access" value="yes" checked="true"/></td>
				</tr>

				<tr>
					<td>Output format:</td>
					<td>
						<select name="format">
							<option value="csv">Spreadsheet</option>
							<option value="html" selected="true">Web page</option>
							<option value="xml">XML</option>
						</select>
					</td>
				</tr>

			</table>
			<br/>

			<p class="center">
				<input type="button" value="Submit the request" onclick="save();"/>
			</p>

			</form>

		</center>

	</body>
</html>

</xsl:template>

</xsl:stylesheet>

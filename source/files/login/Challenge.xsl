<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="url"/>
<xsl:param name="ssid"/>

<xsl:template match="/mirc">
<html>

<head>
	<title><xsl:value-of select="@sitename"/></title>
	<link rel="stylesheet" href="/query/ClassicUI.css" type="text/css"/>
	<style>
		h1 {text-align: center; margin-top: 15px;}
		h2 {text-align: center; margin-top: 5px;}
		h3 {text-align: center; margin-top: 5px;}
		#username, #password {width:150;}
		.note {
			margin-left: 25%; margin-right: 25%; font-size: x-small; font-weight: normal;
			font-family: Verdana, Arial, Helvetica, sans-serif; text-align: left;
		}

}
	</style>
</head>

<body onload="document.getElementById('username').focus();">
	<div class="header" style="background:url(/query/{@masthead}); background-repeat: no-repeat;">
		<xsl:if test="@showsitename='yes'">
			<div class="sitename">
				<span><xsl:value-of select="@sitename"/></span>
			</div>
		</xsl:if>
		<div class="sitelogo" style="height:{@mastheadheight}">&#160;</div>
	</div>

	<form method="post">
		<h1>Requested Resource Requires Authentication</h1>
		<xsl:if test="$ssid">
			<xsl:variable name="lib" select="Libraries/Library[@id=$ssid]"/>
			<xsl:if test="$lib">
				<h2><xsl:value-of select="$lib/title"/></h2>
			</xsl:if>
		</xsl:if>
		<center>
		<h3><xsl:value-of select="$url"/></h3>
		<p class="note">
			The requested resource cannot be accessed with
			your current credentials. Please log in as a user
			who has permission to view the resource.
		</p>
		<table>
			<tr>
				<td>Username:</td>
				<td><input id="username" type="text" name="username"/></td>
			</tr>
			<tr>
				<td>Password:</td>
				<td><input id="password" type="password" name="password"/></td>
			</tr>
		</table>
		<br/>
		<input type="submit" value="&#160;&#160;&#160;Login&#160;&#160;&#160;"/>
		<br/><br/>
		(<a href="/query">Open this site's query page in this window</a>)
		</center>
		<input type="hidden" name="url" value="{$url}"/>
	</form>
</body>

</html>
</xsl:template>

</xsl:stylesheet>
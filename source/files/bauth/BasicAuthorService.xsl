<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="ssid"/>
<xsl:param name="libraryTitle"/>
<xsl:param name="prefs"/>
<xsl:param name="templates"/>
<xsl:param name="textext"/>

<xsl:template match="/MIRCdocument">
	<xsl:variable name="title" select="title"/>
	<html>
		<head>
			<title>Basic Author Service - <xsl:value-of select="$ssid"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/bauth/BasicAuthorService.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/bauth/BasicAuthorService.js">;</script>
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
						 title="Create the MIRCdocument"/>
				</div>
			</xsl:if>

			<h1><xsl:value-of select="$libraryTitle"/> (<xsl:value-of select="$ssid"/>)</h1>
			<h2>Basic Author Service</h2>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" enctype="multipart/form-data" >
			<input type="hidden" name="ui" value="{$ui}"/>

			<p class="note">
				This page may be used by authors to create a MIRCdocument and submit it to the library.
			</p>

			<p class="instruction">If you wish to switch to a different template, select it here:</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Template:</td>
					<td class="text-field">
						<select class="input-length" name="templatename" id="templatename"
								onchange="newTemplate('/bauth/{$ssid}');">
							<xsl:for-each select="$templates/templates/template">
								<option value="{file}">
									<xsl:if test="title = $title">
										<xsl:attribute name="selected">true</xsl:attribute>
									</xsl:if>
									<xsl:value-of select="title"/>
								</option>
							</xsl:for-each>
						</select>
					</td>
				</tr>
			</table>
			</p>

			<p class="instruction">Compose a title - consider a short phrase that includes the history or diagnosis:</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Title:</td>
					<td class="text-field">
						<input class="input-length" name="title" id="title"
							   value="{$title}"/>
					</td>
				</tr>
			</table>
			</p>

			<p class="instruction">Add author and owner information:</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Author's name:</td>
					<td class="text-field">
						<input class="input-length" name="name" value="{$prefs/@name}"/>
					</td>
				</tr>
				<tr>
					<td class="text-label">Author's affiliation:</td>
					<td class="text-field">
						<input class="input-length" name="affiliation" value="{$prefs/@affiliation}"/>
					</td>
				</tr>
				<tr>
					<td class="text-label">Author's phone or email:</td>
					<td class="text-field">
						<input class="input-length" name="contact" value="{$prefs/@contact}"/>
					</td>
				</tr>
				<tr>
					<td class="text-label">Owner's username:</td>
					<td class="text-field">
						<input class="input-length" name="username" value="{$prefs/@username}"/>
					</td>
				</tr>
			</table>
			</p>

			<p class="instruction">Enter any desired content:</p>
			<p class="center">
			<table border="1">

				<xsl:if test="abstract">
					<tr>
						<td class="text-label" valign="top">Abstract:</td>
						<td class="text-field">
							<textarea name="abstract" id="abstract">
								<xsl:text> </xsl:text>
								<xsl:value-of select="abstract"/>
							</textarea>
							<input type="hidden" name="abstract-text" id="abstract-text"></input>
						</td>
					</tr>
				</xsl:if>

				<xsl:for-each select="section">
					<xsl:if test="(@heading != 'Files') and (@heading != 'Notes')">
						<tr>
							<td class="text-label" valign="top">
								<xsl:value-of select="@heading"/>
								<xsl:text>:</xsl:text>
							</td>
							<td class="text-field">
								<xsl:apply-templates select="p | textblock"/>
							</td>
						</tr>
					</xsl:if>
				</xsl:for-each>

			</table>
			</p>

			<p class="instruction">Insert any desired images and files:</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Anonymize DICOM objects:</td>
					<td class="text-field">
						<input type="checkbox" checked="true" name="anonymize"/>
					</td>
				</tr>
				<tr>
					<td class="text-label" id="filelabel" valign="bottom">Include a file:</td>
					<td class="text-field">
						<input onchange="captureFile();" size="75" name="selectedfile0" id="selectedfile0" type="file"/>
					</td>
				</tr>
			</table>
			</p>

			<p class="instruction">Click this button to create the MIRCdocument:
				<input type="button" value="Submit" onclick="save();"/>
			</p>
			<br/>

			</form>
		</body>
	</html>
</xsl:template>

<xsl:template match="p">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="textblock">
	<textarea name="{@name}" id="{@name}">
		<xsl:text> </xsl:text>
	</textarea>
	<input type="hidden" name="{@name}-text" id="{@name}-text"/>
</xsl:template>

<xsl:template match="ul">
	<ul class="no-bullet"><xsl:apply-templates select="checkbox"/></ul>
</xsl:template>

<xsl:template match="ol">
	<ol><xsl:apply-templates select="checkbox"/></ol>
</xsl:template>

<xsl:template match="checkbox">
	<li>
		<input type="checkbox" name="{@name}" value="yes">
			<xsl:value-of select="."/>
		</input>
	</li>
</xsl:template>

<xsl:template match="select">
	<select name="{@name}" class="input-length">
		<xsl:apply-templates select="option"/>
	</select>
</xsl:template>

<xsl:template match="option">
	<option><xsl:value-of select="."/></option>
</xsl:template>

<xsl:template match="br | text()">
	<xsl:copy/>
</xsl:template>

</xsl:stylesheet>

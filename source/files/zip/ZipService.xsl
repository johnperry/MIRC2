<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>
<xsl:param name="ssid"/>
<xsl:param name="name"/>
<xsl:param name="affiliation"/>
<xsl:param name="contact"/>
<xsl:param name="username"/>
<xsl:param name="read"/>
<xsl:param name="update"/>
<xsl:param name="export"/>
<xsl:param name="textext"/>
<xsl:param name="skipext"/>
<xsl:param name="skipprefix"/>
<xsl:param name="result"/>
<xsl:variable name="siteurl" select="/mirc/@siteurl"/>

<xsl:template match="/mirc">
	<html>
		<head>
			<title>Zip Service - <xsl:value-of select="$ssid"/></title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/zip/ZipService.css"></link>
			<xsl:call-template name="result"/>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/zip/ZipService.js">;</script>
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
						 title="Submit the zip file"/>
				</div>
			</xsl:if>

			<h1><xsl:value-of select="Libraries/Library[@id=$ssid]/title"/> (<xsl:value-of select="$ssid"/>)</h1>
			<h2>Zip Service</h2>

			<form id="formID" action="" method="POST" accept-charset="UTF-8" enctype="multipart/form-data" >
			<input type="hidden" name="ui" value="{$ui}"/>

			<p class="note">
				This page may be used by authors to submit zip files to this library.
				A zip file is processed as described in
				<a href="http://mircwiki.rsna.org/index.php?title=The_Zip_Service_User%27s_Manual" target="wiki">
				The Zip Service User's Manual</a>.
			</p>

			<p class="instruction">Add author and document owner information:</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Author's name:</td>
					<td class="text-field">
						<input class="input-length" name="name" value="{$name}"/>
					</td>
				</tr>
				<tr>
					<td class="text-label">Author's affiliation:</td>
					<td class="text-field">
						<input class="input-length" name="affiliation" value="{$affiliation}"/>
					</td>
				</tr>
				<tr>
					<td class="text-label">Author's phone or email:</td>
					<td class="text-field">
						<input class="input-length" name="contact" value="{$contact}"/>
					</td>
				</tr>
				<tr>
					<td class="text-label">Document owner's username:</td>
					<td class="text-field">
						<input class="input-length" name="username" value="{$username}"/>
					</td>
				</tr>
			</table>
			</p>

			<p class="instruction">Decide who can view the document:</p>
			<p class="subnote">
				Enter an asterisk to make the documents public. Blank the field to make the documents private.
				Enter usernames and/or role names separated by commas to restrict documents to specific
				users or classes of users. Usernames are individually enclosed in square brackets; classes of users are
				not. For example, "staff, [drjones], [drsmith]".
			</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Read privilege:</td>
					<td class="text-field">
						<input class="input-length" name="read">
							<xsl:attribute name="value"><xsl:value-of select="$read"/></xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td class="text-label">Update privilege:</td>
					<td class="text-field">
						<input class="input-length" name="update">
							<xsl:attribute name="value"><xsl:value-of select="$update"/></xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td class="text-label">Export privilege:</td>
					<td class="text-field">
						<input class="input-length" name="export">
							<xsl:attribute name="value"><xsl:value-of select="$export"/></xsl:attribute>
						</input>
					</td>
				</tr>
			</table>
			</p>

			<p class="instruction">Specify how the submission is to be processed:</p>
			<p class="center">
			<table border="1">
				<tr>
					<td class="text-label">Text file extensions:</td>
					<td class="text-field">
						<span class="small-font">Enter a list of extensions designating text files, separated by commas:</span><br/>
						<input class="input-length" name="textext">
							<xsl:attribute name="value"><xsl:value-of select="$textext"/></xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td class="text-label">Skip file extensions:</td>
					<td class="text-field">
						<span class="small-font">Enter a list of extensions designating files to be skipped, separated by commas:</span><br/>
						<input class="input-length" name="skipext">
							<xsl:attribute name="value"><xsl:value-of select="$skipext"/></xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td class="text-label">Skip directory prefixes:</td>
					<td class="text-field">
						<span class="small-font">Enter a list of prefixes designating directories to be skipped, separated by commas:</span><br/>
						<input class="input-length" name="skipprefix">
							<xsl:attribute name="value"><xsl:value-of select="$skipprefix"/></xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td class="text-label">Overwrite template values:</td>
					<td class="text-field">
						<input type="checkbox" name="overwrite" value="overwrite" checked="true"/>
						<span class="small-font">Check this box for normal operation; uncheck it to give preference to the values in the template.</span>
					</td>
				</tr>
				<tr>
					<td class="text-label">Anonymize DICOM Objects:</td>
					<td class="text-field">
						<input type="checkbox" name="anonymize" value="anonymize"/>
						<span class="small-font">Check this box to anonymize DICOM files in the submission.</span>
					</td>
				</tr>
			</table>
			</p>

			<p class="center">
			<table>
				<tr>
					<td class="text-label"><b>Select the zip file:&#160;&#160;</b></td>
					<td class="text-field">
						<input class="file"
							onchange="formID.filename.value = selectedFile.value;"
							name="filecontent" id="selectedFile" type="file"/>
					</td>
				</tr>
			</table>
			</p>

			<p class="centered">
				<input type="button" value="Submit the Zip file" onclick="save();"/>
			</p>
			<br/>

			<input ID="filename" name="filename" type="hidden"/>

			</form>

		</body>
	</html>
</xsl:template>

<xsl:template name="result">
	<script>
		var messageText = '<xsl:value-of select="$result"/>';
	</script>
</xsl:template>

</xsl:stylesheet>

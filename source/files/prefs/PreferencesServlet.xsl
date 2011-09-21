<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="pageui"/>

<xsl:template match="/User">
	<html>
		<head>
			<title>Preferences</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/prefs/PreferencesServlet.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/prefs/PreferencesServlet.js">;</script>
			<script>
				var ui = '<xsl:value-of select="$pageui"/>';
			</script>
		</head>
		<body>
			<xsl:if test="$pageui='classic'">
				<div class="closebox">
					<img src="/icons/home.png"
						 onclick="window.open('/query','_self');"
						 title="Return to the home page"/>
					<br/>
					<img src="/icons/save.png"
						 onclick="save();"
						 title="Create the preferences"/>
				</div>
			</xsl:if>

			<h1>Preferences for <xsl:value-of select="@username"/></h1>

			<form id="formID" method="post" action="" accept-charset="UTF-8">
			<input type="hidden" name="pageui" value="{$pageui}"/>

			<p class="note">
				If you wish to change the parameters of your account, make the
				entries in the fields below and then click the save button in the upper right corner.
			</p>

			<p class="note">Select your user interface preference:</p>
			<center>
				<table border="1">
					<tr>
						<td>Query Service user interface</td>
						<td>
							<input type="radio" name="UI" value="classic">
								<xsl:if test="not(@UI='integrated')">
									<xsl:attribute name="checked"/>
								</xsl:if>
								Classic
							</input>
							<br/>
							<input type="radio" name="UI" value="integrated">
								<xsl:if test="@UI='integrated'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								Integrated
							</input>
						</td>
					</tr>
				</table>
			</center>

			<p class="note">To change your <b>MIRC</b> password, use this table:</p>
			<center>
				<table border="1">
					<tr>
						<td>Enter your new password</td>
						<td><input class="text" type="password" name="password1"/></td>
					</tr>
					<tr>
						<td>Enter your new password again</td>
						<td><input class="text" type="password" name="password2"/></td>
					</tr>
				</table>
			</center>

			<p class="note">
				Enter your information as it is to appear in MIRCdocuments in the table below.
			</p>
			<center>
				<table border="1">
					<tr>
						<td>Name</td>
						<td><input class="text" type="text" name="name" value="{@name}"/></td>
					</tr>
					<tr>
						<td>Institution or department affiliation</td>
						<td><input class="text" type="text" name="affiliation" value="{@affiliation}"/></td>
					</tr>
					<tr>
						<td>Contact infomation</td>
						<td><input class="text" type="text" name="contact" value="{@contact}"/></td>
					</tr>
				</table>
			</center>

			<p class="note">
				If you wish to grant MIRC access to your <b>myRSNA</b> account,
				enter the information in the table below.
			</p>
			<center>
				<table border="1">
					<tr>
						<td>Include myRSNA files in the file cabinet</td>
						<td>
							<input type="checkbox" name="myrsna" value="yes">
								<xsl:if test="myrsna/@enabled='yes'">
									<xsl:attribute name="checked"/>
								</xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td>Enter your myRSNA username</td>
						<td><input class="text" type="text" name="myrsnaUsername" value="{myrsna/@username}"/></td>
					</tr>
					<tr>
						<td>Enter your myRSNA password</td>
						<td><input class="text" type="password" name="myrsnaPassword" value="{myrsna/@password}"/></td>
					</tr>
				</table>
			</center>

			<p class="note">
				If you wish to export MIRCdocuments to other MIRC sites,
				enter the information in the table below. For each site, enter a name,
				the URL of the site's Submit Service, and your username and password on
				that site. You can find the Submit Service URL by logging into the site
				with your browser and selecting the Submit Service. To remove a site from
				the list, remove its name or URL from the table.

			</p>
			<center>
				<table class="wide" border="1">
					<tr>
						<th class="name">Site Name</th>
						<th class="url">Submit Service URL</th>
						<th class="un">Username</th>
						<th class="pw">Password</th>
					</tr>
					<xsl:for-each select="export/site">
						<tr>
							<xsl:variable name="n"><xsl:number/></xsl:variable>
							<td class="name"><input class="text" type="text" name="export-name[{$n}]" value="{@name}"/></td>
							<td class="url"><input class="text" type="text" name="export-url[{$n}]" value="{@url}"/></td>
							<td class="un"><input class="text" type="text" name="export-un[{$n}]" value="{@username}"/></td>
							<td class="pw"><input class="text" type="password" name="export-pw[{$n}]" value="{@password}"/></td>
						</tr>
					</xsl:for-each>
					<tr>
						<td class="name"><input class="text" type="text" name="export-name[0]"/></td>
						<td class="url"><input class="text" type="text" name="export-url[0]"/></td>
						<td class="un"><input class="text" type="text" name="export-un[0]"/></td>
						<td class="pw"><input class="text" type="password" name="export-pw[0]"/></td>
					</tr>
				</table>
				<p>
					<input type="button" value="Save these preferences" onclick="save();"/>
				</p>
			</center>

			<br/>
			</form>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
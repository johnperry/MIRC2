<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/mirc">
	<html>
		<head>
			<title>File Service Admin</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/fsadmin/FileServiceAdmin.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/fsadmin/FileServiceAdmin.js">;</script>
		</head>
		<body>
			<div class="closebox">
				<img src="/icons/home.png"
					 onclick="window.open('/query','_self');"
					 title="Return to the home page"/>
				<br/>
				<img src="/icons/save.png"
					 onclick="save();"
					 title="Save"/>
			</div>

			<h1>File Service Admin</h1>
			<form id="formID" action="" method="POST" accept-charset="UTF-8">

			<p class="note">
				The table below controls the primary configuration parameters
				of the File Service.
			</p>

			<center>
				<table border="1">
					<tr>
						<td>Shared files timeout (in hours, default=0) :</td>
						<td><input class="text" type="text" name="timeout" value="{FileService/@timeout}"/></td>
					</tr>
					<tr>
						<td>Maximum upload size (in MB, default=75) :</td>
						<td><input class="text" type="text" name="maxsize" value="{FileService/@maxsize}"/></td>
					</tr>
				</table>
			</center>

			<p class="note">
				Note: The File Service DICOM Service is controlled through the Admin/Pipelines menu.
			</p>

			</form>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
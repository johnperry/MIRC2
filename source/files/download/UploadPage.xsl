<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="ui"/>

<xsl:template match="/mirc">
	<html>
		<head>
			<title>Download Service</title>
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

			<h1><xsl:value-of select="@sitename"/></h1>
			<h2>Download Service: Upload File</h2>

			<center>
				<p class="center">
					Browse to a file on your computer and click the <b>Upload</b> button.
				</p>
				<form action="" method="POST" accept-charset="UTF-8" enctype="multipart/form-data" >
					<p class="center">
						<input class="file" name="file" type="file"/>
						<input name="ui" type="hidden" value="{$ui}"/>
					</p>
					<input type="submit" value="Upload"/>
				</form>
			</center>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
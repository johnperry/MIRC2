<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="url"/>

<xsl:template match="/Quiz">
	<html>
		<head>
			<title>Answer Summary</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/quizmgr/ScoredQuiz.css"></link>
		</head>
		<body>
			<center>
				<h1>Answer Summary</h1>
				<h2><xsl:value-of select="title"/></h2>
				<xsl:for-each select="ScoredQuestion">
					<table border="1">
						<tr><th colspan="2"><xsl:value-of select="text()"/></th></tr>
						<xsl:for-each select="Answer">
						<xsl:sort select="@n" order="descending"/>
							<tr>
								<td class="score"><xsl:value-of select="@n"/></td>
								<td class="answer"><xsl:value-of select="."/></td>
							</tr>
						</xsl:for-each>
					</table>
					<br/>
				</xsl:for-each>
			</center>
		</body>
	</html>
</xsl:template>

</xsl:stylesheet>

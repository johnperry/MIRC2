<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:template match="/Users">
	<html>
		<head>
			<title>Quiz Standings</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/quizmgr/ScoredQuiz.css"></link>
		</head>
		<body>
			<center>
				<h1>Quiz Summary</h1>
				<table border="1">
					<xsl:for-each select="User">
						<tr>
							<td class="name"><xsl:value-of select="@name"/></td>
							<td class="user"><xsl:value-of select="@id"/></td>
							<td class="score"><xsl:value-of select="@score"/></td>
						</tr>
					</xsl:for-each>
				</table>
			</center>
		</body>
	</html>
</xsl:template>

</xsl:stylesheet>

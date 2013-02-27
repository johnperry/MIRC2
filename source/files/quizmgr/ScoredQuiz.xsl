<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="url"/>

<xsl:template match="/Quiz">
	<html>
		<head>
			<title>Quiz Scorecard</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/BaseStyles.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/quizmgr/ScoredQuiz.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/quizmgr/ScoredQuiz.js">;</script>
		</head>
		<body>
			<center>
				<h1>Quiz Scorecard</h1>
				<h2><xsl:value-of select="title"/></h2>
				<xsl:for-each select="ScoredQuestion">
					<p class="ScoredQuestion">
						<input type="hidden" value="{@id}"/>
					</p>
					<table border="1">
						<tr><th colspan="4"><xsl:value-of select="text()"/></th></tr>
						<xsl:for-each select="Answer">
							<tr>
								<td class="name"><xsl:value-of select="@name"/></td>
								<td class="user"><xsl:value-of select="@id"/></td>
								<td class="answer"><xsl:value-of select="."/></td>
								<td class="score">
									<input type="text" value="{@score}"/>
									<input type="hidden" value="{@id}"/>
								</td>
							</tr>
						</xsl:for-each>
					</table>
					<br/>
				</xsl:for-each>
				<br/>
				<input type="button" value="Submit" onclick="sendScores('{$url}');"/>
			</center>
		</body>
	</html>
</xsl:template>

</xsl:stylesheet>

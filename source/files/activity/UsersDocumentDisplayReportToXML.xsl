<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" encoding="utf-8"/>

<xsl:param name="date"/>
<xsl:param name="ssid"/>

<xsl:variable name="prefs" select="/UsersDocumentDisplayList/Preferences/User"/>

<xsl:template match="/UsersDocumentDisplayList">
	<UsersDocumentDisplayList>
		<xsl:apply-templates select="Library"/>
	</UsersDocumentDisplayList>
</xsl:template>

<xsl:template match="Library">
	<Library date="{@date}" ssid="{@ssid}" title="{@title}">
		<xsl:apply-templates select="User"/>
	</Library>
</xsl:template>

<xsl:template match="User">
	<xsl:variable name="un" select="@username"/>
	<User n="{@n}" username="{@username}" name="{$prefs[@username=$un]/@name}"/>
</xsl:template>

</xsl:stylesheet>

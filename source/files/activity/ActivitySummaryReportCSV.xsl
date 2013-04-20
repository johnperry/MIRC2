<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="utf-8"/>

<xsl:template match="/SummaryReport">

<xsl:call-template name="headings"/>
<xsl:for-each select="ActivitySummary">
	<xsl:for-each select="MonthlyReport">
		<xsl:text>="</xsl:text><xsl:value-of select="../@siteID"/><xsl:text>",</xsl:text>
		<xsl:text>"</xsl:text><xsl:value-of select="../@url"/><xsl:text>",</xsl:text>
		<xsl:text>"</xsl:text><xsl:value-of select="../@ip"/><xsl:text>",</xsl:text>
		<xsl:text>"</xsl:text><xsl:value-of select="../@name"/><xsl:text>",</xsl:text>
		<xsl:value-of select="../@version"/><xsl:text>,</xsl:text>
		<xsl:value-of select="../@email"/><xsl:text>,</xsl:text>
		<xsl:value-of select="../@users"/><xsl:text>,</xsl:text>
		<xsl:text>"</xsl:text><xsl:value-of select="@date"/><xsl:text>",</xsl:text>
		<xsl:value-of select="@activeUsers"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@libs"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@docs"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@docsDisplayed"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@storage"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@aauth"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@bauth"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@sub"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@zip"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@dcm"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@tce"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@myrsna"/><xsl:text>,</xsl:text>
		<xsl:value-of select="@slides"/><xsl:text>
</xsl:text>
	</xsl:for-each>
</xsl:for-each>
</xsl:template>


<xsl:template name="headings">
Site ID,URL,IP,Site Name,Version,Admin email,Accounts,Report Date,Active Users,Libraries,Total Docs,Docs Displayed,Display Reqs,Adv Auth,Basic Auth,Submit,Zip,DICOM,TCE,MyRSNA,Slides
</xsl:template>

</xsl:stylesheet>

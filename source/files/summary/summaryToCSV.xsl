<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="utf-8"/>

<xsl:param name="show-titles"/>
<xsl:param name="show-names"/>
<xsl:param name="show-dates"/>

<xsl:template match="/IndexSummary">
Starting date (inclusive):,<xsl:value-of select="StartDate"/>
Ending date (inclusive):,<xsl:value-of select="EndDate"/>
Number of indexed documents:,<xsl:value-of select="IndexedDocs"/>
Number of selected documents:,<xsl:value-of select="DocsInRange"/>

Username,No.Docs,Sel.Docs,Pub.Docs

<xsl:apply-templates select="Owner"/>
</xsl:template>

<xsl:template match="Owner">
<xsl:value-of select="username"/>
<xsl:text>,</xsl:text>
<xsl:value-of select="IndexedDocs"/>
<xsl:text>,</xsl:text>
<xsl:value-of select="DocsInRange"/>
<xsl:text>,</xsl:text>
<xsl:value-of select="PublicDocsInRange"/>
<xsl:text>
</xsl:text>
</xsl:template>

</xsl:stylesheet>

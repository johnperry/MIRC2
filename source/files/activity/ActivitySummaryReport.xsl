<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8"/>

<xsl:template match="/ActivityReport">
	<ActivitySummary
		siteID="{@id}"
		name="{@name}"
		url="{@url}"
		ip="{@ip}"
		users="{@users}"
		version="{@version}"
		email="{@email}">
		<xsl:apply-templates select="MonthlyReport"/>
	</ActivitySummary>
</xsl:template>

<xsl:template match="MonthlyReport">
	<xsl:variable name="libs" select="Library"/>
	<MonthlyReport
		date="{@date}"
		libs="{count($libs)}"
		docs="{sum($libs/@docs)}"
		aauth="{sum($libs/@aauth)}"
		bauth="{sum($libs/@bauth)}"
		sub="{sum($libs/@sub)}"
		zip="{sum($libs/@zip)}"
		dcm="{sum($libs/@dcm)}"
		tce="{sum($libs/@tce)}"
		myrsna="{sum($libs/@myrsna)}"
		slides="{sum($libs/@slides)}"
		storage="{sum($libs/@storage)}"
		docsDisplayed="{sum($libs/@docsDisplayed)}"
		activeUsers="{@activeUsers}" />
</xsl:template>

</xsl:stylesheet>

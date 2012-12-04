<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1">

<xsl:param name="today"/>

<xsl:template match="@*|text()">
	<xsl:copy/>
</xsl:template>

<xsl:template match="*">
	<xsl:if test="not( (name()='phi') ) and
				  not( (@visible='owner') or (@visible='no') or
				 	   (@after and (number($today) &lt; number(@after))) )">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|text()" />
		</xsl:copy>
	</xsl:if>
</xsl:template>

<xsl:template match="/">
	<xsl:apply-templates/>
</xsl:template>

</xsl:stylesheet>

<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="queryUID"/>

<xsl:template match="/formdata">

	<MIRCquery>
		<xsl:call-template name="queryattributes" />
		<xsl:call-template name="queryidstring"/>

		<xsl:apply-templates/>

		<xsl:call-template name="patientelements" />

		<xsl:call-template name="imageelements" />

	</MIRCquery>

</xsl:template>

<xsl:template name="queryattributes" >
	<xsl:for-each select="firstresult|maxresults|unknown|bgcolor|display|icons|orderby|special" >
		<xsl:attribute name="{local-name()}" >
			<xsl:value-of select="."/>
		</xsl:attribute>
	</xsl:for-each>
</xsl:template>

<xsl:template name="queryidstring" >
	<xsl:if test = "$queryUID" >
		<xsl:attribute name="queryUID" >
			<xsl:value-of select="$queryUID"/>
		</xsl:attribute>
	</xsl:if>
</xsl:template>

<xsl:template match="document" >
	<xsl:value-of select="."/>
</xsl:template>

<xsl:template match="node()" >
	<xsl:variable name="x" select="normalize-space(.)"/>
	<xsl:if test="$x">
		<xsl:element name="{local-name()}" >
			<xsl:value-of select="$x"/>
		</xsl:element>
	</xsl:if>
</xsl:template>


<xsl:template match="bgcolor|display|icons|orderby"/>
<xsl:template match="firstresult|maxresults|queryUID|unknown|showimages|casenavigator|randomize|server" />
<xsl:template match="imagemodality|imageanatomy|imageformat|imagecompression|imagepathology" />
<xsl:template match="pt-name|pt-id|pt-mrn|pt-sex|pt-race|pt-species|pt-breed" />
<xsl:template match="years|months|weeks|days" />

<xsl:template name="patientelements" >
	<xsl:variable name="content" select="normalize-space(concat(pt-name, pt-id, pt-mrn, pt-sex, pt-race, pt-species, pt-breed, years, months, weeks, days))"/>
	<xsl:if test="$content">
		<patient>
			<xsl:for-each select = "pt-name | pt-id | pt-mrn | pt-sex | pt-race | pt-species | pt-breed" >
				<xsl:variable name="x" select="normalize-space(.)"/>
				<xsl:if test="$x">
					<xsl:element name="{local-name()}" >
						<xsl:value-of select="$x"/>
					</xsl:element>
				</xsl:if>
			</xsl:for-each>
			<xsl:variable name="age" select="normalize-space(concat(years, months, weeks, days))"/>
			<xsl:if test="$age">
				<pt-age>
					<xsl:for-each select = "years | months | weeks | days" >
						<xsl:variable name="x" select="normalize-space(.)"/>
						<xsl:if test="$x">
							<xsl:element name="{local-name()}" >
								<xsl:value-of select="$x"/>
							</xsl:element>
						</xsl:if>
					</xsl:for-each>
				</pt-age>
			</xsl:if>
		</patient>
	</xsl:if>
</xsl:template>

<xsl:template name="imageelements" >
	<xsl:variable name="image" select="normalize-space(concat(imagemodality, imageformat, imagecompression, imageanatomy, imagepathology))"/>
	<xsl:if test="$image">
		<image>
			<xsl:for-each select="imagemodality | imageformat | imagecompression |
								  imageanatomy | imagepathology" >
				<xsl:variable name="x" select="normalize-space(.)"/>
				<xsl:if test="$x">
					<xsl:element name="{substring(local-name(),6)}" >
						<xsl:value-of select="$x"/>
					</xsl:element>
				</xsl:if>
			</xsl:for-each>
		</image>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>

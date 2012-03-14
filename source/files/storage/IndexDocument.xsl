<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="lmdate">0</xsl:param>
<xsl:param name="lmstring"></xsl:param>

<xsl:template match="/MIRCdocument">
	<doc>
		<MIRCdocument>
			<xsl:copy-of select="@path"/>
			<xsl:copy-of select="@temp"/>
			<xsl:call-template name="title"/>
			<xsl:copy-of select="alternative-title"/>
			<xsl:copy-of select="author"/>
			<xsl:copy-of select="abstract"/>
			<xsl:copy-of select="alternative-abstract"/>
			<xsl:copy-of select="access"/>
			<xsl:copy-of select="category"/>
			<xsl:copy-of select="level"/>
			<xsl:call-template name="images"/>
			<xsl:call-template name="access"/>
			<lmdate><xsl:value-of select="$lmstring"/></lmdate>
		</MIRCdocument>
		<sm>
			<img n="{count(//image)}"/>
			<ann n="{count(//image[alternative-image[@role='annotation']])}"/>
			<pubdate><xsl:value-of select="translate(//publication-date,'/.-','')"/></pubdate>
			<lmdate><xsl:value-of select="$lmdate"/></lmdate>
			<xsl:call-template name="access"/>
			<xsl:call-template name="owners"/>
			<xsl:apply-templates select="authorization"/>
		</sm>
	</doc>
</xsl:template>

<xsl:template name="title">
	<xsl:choose>
		<xsl:when test="string-length(normalize-space('title')) != 0">
			<xsl:copy-of select="title"/>
		</xsl:when>
		<xsl:otherwise>
			<title>Untitled</title>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template name="images">
	<images>
		<xsl:for-each select="//image-section/image">
			<xsl:sort select="alternative-image[@role='annotation']/@src" order="descending"/>
			<xsl:if test="position() &lt; 7">
				<image src="{@src}" an="{alternative-image[@role='annotation']/@src}"/>
			</xsl:if>
		</xsl:for-each>
	</images>
</xsl:template>

<xsl:template name="access">
	<xsl:variable name="read" select="authorization/read"/>
	<access>
		<xsl:choose>
			<xsl:when test="(count($read) = 0) or contains($read,'*')">
				<xsl:text>public</xsl:text>
			</xsl:when>
			<xsl:when test="(count($read) != 0) and (string-length(normalize-space($read)) = 0)">
				<xsl:text>owner</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>restricted</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</access>
</xsl:template>

<xsl:template match="authorization">
	<read>
		<xsl:call-template name="users">
			<xsl:with-param name="list" select="owner"/>
		</xsl:call-template>
		<xsl:call-template name="roles">
			<xsl:with-param name="list" select="read"/>
		</xsl:call-template>
	</read>
</xsl:template>

<xsl:template name="owners">
	<owner>
		<xsl:call-template name="users">
			<xsl:with-param name="list" select="authorization/owner"/>
		</xsl:call-template>
	</owner>
</xsl:template>

<xsl:template name="users">
	<xsl:param name="list"/>
	<xsl:variable name="nlist" select="concat(normalize-space(translate($list,',;','  ')),' ')"/>
	<xsl:variable name="first" select="substring-before($nlist,' ')"/>
	<xsl:variable name="rest" select="substring-after($nlist,' ')"/>
	<xsl:if test="$first">
		<user>
			<xsl:value-of select="translate($first,'[]','')"/>
		</user>
		<xsl:call-template name="users">
			<xsl:with-param name="list" select="$rest"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>

<xsl:template name="roles">
	<xsl:param name="list"/>
	<xsl:variable name="nlist" select="concat(normalize-space(translate($list,',;','  ')),' ')"/>
	<xsl:variable name="first" select="substring-before($nlist,' ')"/>
	<xsl:variable name="rest" select="substring-after($nlist,' ')"/>
	<xsl:if test="$first">
		<xsl:choose>
			<xsl:when test="starts-with($first,'[')">
				<user>
					<xsl:value-of select="translate($first,'[]','')"/>
				</user>
			</xsl:when>
			<xsl:otherwise>
				<role>
					<xsl:value-of select="$first"/>
				</role>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:call-template name="roles">
			<xsl:with-param name="list" select="$rest"/>
		</xsl:call-template>
	</xsl:if>
</xsl:template>

</xsl:stylesheet>
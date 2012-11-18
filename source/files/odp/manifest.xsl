<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/images">

<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0">
 <manifest:file-entry manifest:media-type="application/vnd.oasis.opendocument.presentation" manifest:version="1.2" manifest:full-path="/"/>
 <xsl:apply-templates select="image"/>
 <manifest:file-entry manifest:media-type="image/png" manifest:full-path="Pictures/10000000000000200000002000309F1C.png"/>
 <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="content.xml"/>
 <manifest:file-entry manifest:media-type="text/xml" manifest:full-path="styles.xml"/>
</manifest:manifest>

</xsl:template>

<xsl:template match="image">
 <manifest:file-entry manifest:media-type="image/jpeg" manifest:full-path="Pictures/{@src}"/>
</xsl:template>

</xsl:stylesheet>
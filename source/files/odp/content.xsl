<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet
				version="1.0"
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
				xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
				xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
				xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
				xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
				xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
				xmlns:xlink="http://www.w3.org/1999/xlink"
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0"
				xmlns:number="urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0"
				xmlns:presentation="urn:oasis:names:tc:opendocument:xmlns:presentation:1.0"
				xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
				xmlns:chart="urn:oasis:names:tc:opendocument:xmlns:chart:1.0"
				xmlns:dr3d="urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0"
				xmlns:math="http://www.w3.org/1998/Math/MathML"
				xmlns:form="urn:oasis:names:tc:opendocument:xmlns:form:1.0"
				xmlns:script="urn:oasis:names:tc:opendocument:xmlns:script:1.0"
				xmlns:ooo="http://openoffice.org/2004/office"
				xmlns:ooow="http://openoffice.org/2004/writer"
				xmlns:oooc="http://openoffice.org/2004/calc"
				xmlns:dom="http://www.w3.org/2001/xml-events"
				xmlns:xforms="http://www.w3.org/2002/xforms"
				xmlns:xsd="http://www.w3.org/2001/XMLSchema"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				xmlns:smil="urn:oasis:names:tc:opendocument:xmlns:smil-compatible:1.0"
				xmlns:anim="urn:oasis:names:tc:opendocument:xmlns:animation:1.0"
				xmlns:rpt="http://openoffice.org/2005/report"
				xmlns:of="urn:oasis:names:tc:opendocument:xmlns:of:1.2"
				xmlns:xhtml="http://www.w3.org/1999/xhtml"
				xmlns:grddl="http://www.w3.org/2003/g/data-view#"
				xmlns:officeooo="http://openoffice.org/2009/office"
				xmlns:tableooo="http://openoffice.org/2009/table"
				xmlns:drawooo="http://openoffice.org/2010/draw"
				xmlns:field="urn:openoffice:names:experimental:ooo-ms-interop:xmlns:field:1.0"
				office:version="1.2"
				grddl:transformation="http://docs.oasis-open.org/office/1.2/xslt/odf2rdf.xsl" >

<xsl:param name="images"/>

<xsl:template match="/MIRCdocument">
	<office:document-content
				xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
				xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
				xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
				xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
				xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
				xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
				xmlns:xlink="http://www.w3.org/1999/xlink"
				xmlns:dc="http://purl.org/dc/elements/1.1/"
				xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0"
				xmlns:number="urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0"
				xmlns:presentation="urn:oasis:names:tc:opendocument:xmlns:presentation:1.0"
				xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
				xmlns:chart="urn:oasis:names:tc:opendocument:xmlns:chart:1.0"
				xmlns:dr3d="urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0"
				xmlns:math="http://www.w3.org/1998/Math/MathML"
				xmlns:form="urn:oasis:names:tc:opendocument:xmlns:form:1.0"
				xmlns:script="urn:oasis:names:tc:opendocument:xmlns:script:1.0"
				xmlns:ooo="http://openoffice.org/2004/office"
				xmlns:ooow="http://openoffice.org/2004/writer"
				xmlns:oooc="http://openoffice.org/2004/calc"
				xmlns:dom="http://www.w3.org/2001/xml-events"
				xmlns:xforms="http://www.w3.org/2002/xforms"
				xmlns:xsd="http://www.w3.org/2001/XMLSchema"
				xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
				xmlns:smil="urn:oasis:names:tc:opendocument:xmlns:smil-compatible:1.0"
				xmlns:anim="urn:oasis:names:tc:opendocument:xmlns:animation:1.0"
				xmlns:rpt="http://openoffice.org/2005/report"
				xmlns:of="urn:oasis:names:tc:opendocument:xmlns:of:1.2"
				xmlns:xhtml="http://www.w3.org/1999/xhtml"
				xmlns:grddl="http://www.w3.org/2003/g/data-view#"
				xmlns:officeooo="http://openoffice.org/2009/office"
				xmlns:tableooo="http://openoffice.org/2009/table"
				xmlns:drawooo="http://openoffice.org/2010/draw"
				xmlns:field="urn:openoffice:names:experimental:ooo-ms-interop:xmlns:field:1.0"
				office:version="1.2"
				grddl:transformation="http://docs.oasis-open.org/office/1.2/xslt/odf2rdf.xsl" >
		<xsl:call-template name="scripts"/>
		<xsl:call-template name="automatic-styles"/>
		<xsl:call-template name="body"/>
	</office:document-content>
</xsl:template>

<xsl:template name="body">
	<office:body>
		<office:presentation>
			<xsl:apply-templates select="title"/>
			<xsl:apply-templates select="section/p
											| section/image
												| image-section/image"/>
		</office:presentation>
	</office:body>
</xsl:template>

<xsl:template match="title">
	<xsl:variable name="n"><xsl:number level="any"/></xsl:variable>
	<draw:page draw:style-name="dp1" draw:id="Title-{$n}" draw:name="Title-{$n}" draw:master-page-name="Default" presentation:presentation-page-layout-name="AL1T32">
		<draw:frame presentation:style-name="pr1" draw:text-style-name="P1" draw:layer="layout" svg:width="25.199cm" svg:height="17.935cm" svg:x="1.4cm" svg:y="0.837cm" presentation:class="subtitle" presentation:user-transformed="true">
			<draw:text-box>
				<text:p>
					<text:span text:style-name="T1"><xsl:value-of select="normalize-space(.)"/></text:span>
				</text:p>
				<xsl:apply-templates select="../author/name"/>
			</draw:text-box>
		</draw:frame>
		<draw:frame draw:style-name="gr1" draw:text-style-name="P1" draw:layer="layout" svg:width="12.000cm" svg:height="0.963cm" svg:x="16.000cm" svg:y="19.746cm">
			<draw:text-box>
				<text:p>
					<text:span text:style-name="T1">RSNA MIRC Teaching File System</text:span>
				</text:p>
			</draw:text-box>
		</draw:frame>
	</draw:page>
</xsl:template>

<xsl:template match="name">
	<text:p>
		<text:span text:style-name="T1"><xsl:value-of select="normalize-space(.)"/></text:span>
	</text:p>
</xsl:template>

<xsl:template match="p">
	<xsl:variable name="n"><xsl:number level="any"/></xsl:variable>
	<draw:page draw:style-name="dp1" draw:id="Paragraph-{$n}" draw:name="Paragraph-{$n}" draw:master-page-name="Default" presentation:presentation-page-layout-name="AL2T1">
		<draw:frame presentation:style-name="pr3" draw:layer="layout" svg:width="25.199cm" svg:height="3.506cm" svg:x="1.4cm" svg:y="0.837cm" presentation:class="title" presentation:user-transformed="true">
			<draw:text-box>
			<text:p><xsl:value-of select="normalize-space(../@heading)"/></text:p>
			</draw:text-box>
		</draw:frame>
		<draw:frame presentation:style-name="pr4" draw:layer="layout" svg:width="25.199cm" svg:height="13.859cm" svg:x="1.4cm" svg:y="4.914cm" presentation:class="outline" presentation:user-transformed="true">
			<draw:text-box>
				<text:p><xsl:value-of select="normalize-space(.)"/></text:p>
			</draw:text-box>
		</draw:frame>
	</draw:page>
</xsl:template>

<xsl:template match="image">
	<xsl:variable name="n"><xsl:number level="any"/></xsl:variable>
	<xsl:variable name="src" select="@src"/>
	<xsl:variable name="img" select="$images/images/image[@name=$src]"/>
	<xsl:if test="$img">
		<draw:page draw:style-name="dp1" draw:id="Image-{$n}" draw:name="Image-{$n}" draw:master-page-name="Default">
			<draw:frame
					draw:style-name="gr3"
					draw:text-style-name="P2"
					draw:layer="layout"
					svg:width="{$img/@w}"
					svg:height="{$img/@h}"
					svg:x="{$img/@x}"
					svg:y="{$img/@y}">
				<draw:image
						xlink:href="Pictures/{$img/@src}"
						xlink:type="simple"
						xlink:show="embed"
						xlink:actuate="onLoad">
					<text:p />
				</draw:image>
			</draw:frame>
			<xsl:if test="image-caption">
				<draw:frame draw:style-name="gr1" draw:text-style-name="P1" draw:layer="layout" svg:width="26.000cm" svg:height="3.000cm" svg:x="1.000cm" svg:y="0.100cm">
					<draw:text-box>
						<xsl:apply-templates select="image-caption[@display='always']"/>
						<xsl:apply-templates select="image-caption[@display='click']"/>
					</draw:text-box>
				</draw:frame>
			</xsl:if>
		</draw:page>
		<xsl:apply-templates select="alternative-image[@role='annotation']">
			<xsl:with-param name="n" select="$n"/>
		</xsl:apply-templates>
	</xsl:if>
</xsl:template>

<xsl:template match="alternative-image[@role='annotation']">
	<xsl:param name="n"/>
	<xsl:variable name="src" select="@src"/>
	<xsl:variable name="img" select="$images/images/image[@name=$src]"/>
	<xsl:if test="$img">
		<draw:page draw:style-name="dp1" draw:id="Annotation-{$n}" draw:name="Annotation-{$n}" draw:master-page-name="Default">
			<draw:frame
					draw:style-name="gr3"
					draw:text-style-name="P2"
					draw:layer="layout"
					svg:width="{$img/@w}"
					svg:height="{$img/@h}"
					svg:x="{$img/@x}"
					svg:y="{$img/@y}">
				<draw:image
						xlink:href="Pictures/{$img/@src}"
						xlink:type="simple"
						xlink:show="embed"
						xlink:actuate="onLoad">
					<text:p />
				</draw:image>
			</draw:frame>
		</draw:page>
	</xsl:if>
</xsl:template>

<xsl:template match="image-caption">
	<text:p>
		<text:span text:style-name="T1"><xsl:value-of select="normalize-space(.)"/></text:span>
	</text:p>
</xsl:template>

<xsl:template name="scripts">
  <office:scripts />
</xsl:template>

<xsl:template name="automatic-styles">
  <office:automatic-styles>
    <style:style style:name="dp1" style:family="drawing-page">
      <style:drawing-page-properties presentation:background-visible="true" presentation:background-objects-visible="true" presentation:display-footer="true" presentation:display-page-number="false" presentation:display-date-time="true" />
    </style:style>
    <style:style style:name="dp2" style:family="drawing-page">
      <style:drawing-page-properties presentation:display-header="true" presentation:display-footer="true" presentation:display-page-number="false" presentation:display-date-time="true" />
    </style:style>
    <style:style style:name="gr1" style:family="graphic" style:parent-style-name="standard">
      <style:graphic-properties draw:stroke="none" svg:stroke-color="#000000" draw:fill="none" draw:fill-color="#ffffff" draw:textarea-horizontal-align="left" draw:auto-grow-height="true" draw:auto-grow-width="true" fo:min-height="0cm" fo:min-width="0cm" />
    </style:style>
    <style:style style:name="gr2" style:family="graphic">
      <style:graphic-properties style:protect="size" />
    </style:style>
    <style:style style:name="gr3" style:family="graphic" style:parent-style-name="standard">
      <style:graphic-properties draw:stroke="none" draw:fill="none" draw:textarea-horizontal-align="center" draw:textarea-vertical-align="middle" draw:color-mode="standard" draw:luminance="0%" draw:contrast="0%" draw:gamma="100%" draw:red="0%" draw:green="0%" draw:blue="0%" fo:clip="rect(0cm, 0cm, 0cm, 0cm)" draw:image-opacity="100%" style:mirror="none" />
    </style:style>
    <style:style style:name="pr1" style:family="presentation" style:parent-style-name="Default-subtitle">
      <style:graphic-properties svg:stroke-color="#000000" draw:fill-color="#ffffff" fo:min-height="17.935cm" />
    </style:style>
    <style:style style:name="pr2" style:family="presentation" style:parent-style-name="Default-notes">
      <style:graphic-properties draw:fill-color="#ffffff" draw:auto-grow-height="true" fo:min-height="12.572cm" />
    </style:style>
    <style:style style:name="pr3" style:family="presentation" style:parent-style-name="Default-title">
      <style:graphic-properties svg:stroke-color="#000000" fo:min-height="3.506cm" />
    </style:style>
    <style:style style:name="pr4" style:family="presentation" style:parent-style-name="Default-outline1">
      <style:graphic-properties svg:stroke-color="#000000" fo:min-height="13.859cm" />
    </style:style>
    <style:style style:name="pr5" style:family="presentation" style:parent-style-name="Default-notes">
      <style:graphic-properties draw:fill-color="#ffffff" fo:min-height="12.572cm" />
    </style:style>
    <style:style style:name="pr6" style:family="presentation" style:parent-style-name="Default-title">
      <style:graphic-properties fo:min-height="3.506cm" />
    </style:style>
    <style:style style:name="pr7" style:family="presentation" style:parent-style-name="Default-outline1">
      <style:graphic-properties fo:min-height="13.859cm" />
    </style:style>
    <style:style style:name="P1" style:family="paragraph">
      <style:text-properties fo:color="#ffffff" />
    </style:style>
    <style:style style:name="P2" style:family="paragraph">
      <style:paragraph-properties fo:text-align="center" />
    </style:style>
    <style:style style:name="T1" style:family="text">
      <style:text-properties fo:color="#ffffff" />
    </style:style>
    <text:list-style style:name="L1">
      <text:list-level-style-bullet text:level="1" text:bullet-char="-">
        <style:list-level-properties />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="2" text:bullet-char="-">
        <style:list-level-properties text:space-before="0.6cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="3" text:bullet-char="-">
        <style:list-level-properties text:space-before="1.2cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="4" text:bullet-char="-">
        <style:list-level-properties text:space-before="1.8cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="5" text:bullet-char="-">
        <style:list-level-properties text:space-before="2.4cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="6" text:bullet-char="-">
        <style:list-level-properties text:space-before="3cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="7" text:bullet-char="-">
        <style:list-level-properties text:space-before="3.6cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="8" text:bullet-char="-">
        <style:list-level-properties text:space-before="4.2cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="9" text:bullet-char="-">
        <style:list-level-properties text:space-before="4.8cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="10" text:bullet-char="-">
        <style:list-level-properties text:space-before="5.4cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
    </text:list-style>
    <text:list-style style:name="L2">
      <text:list-level-style-bullet text:level="1" text:bullet-char="-">
        <style:list-level-properties text:space-before="0.3cm" text:min-label-width="0.9cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="2" text:bullet-char="-">
        <style:list-level-properties text:space-before="1.5cm" text:min-label-width="0.9cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="3" text:bullet-char="-">
        <style:list-level-properties text:space-before="2.8cm" text:min-label-width="0.8cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="75%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="4" text:bullet-char="-">
        <style:list-level-properties text:space-before="4.2cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="5" text:bullet-char="-">
        <style:list-level-properties text:space-before="5.4cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="75%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="6" text:bullet-char="-">
        <style:list-level-properties text:space-before="6.6cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="7" text:bullet-char="-">
        <style:list-level-properties text:space-before="7.8cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="8" text:bullet-char="-">
        <style:list-level-properties text:space-before="9cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="9" text:bullet-char="-">
        <style:list-level-properties text:space-before="10.2cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
      <text:list-level-style-bullet text:level="10" text:bullet-char="-">
        <style:list-level-properties text:space-before="11.4cm" text:min-label-width="0.6cm" />
        <style:text-properties fo:font-family="StarSymbol" style:use-window-font-color="true" fo:font-size="45%" />
      </text:list-level-style-bullet>
    </text:list-style>
  </office:automatic-styles>
</xsl:template>

</xsl:stylesheet>
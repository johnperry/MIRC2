<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet version="1.1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="suppressHome">no</xsl:param>
<xsl:param name="homeURL"/>
<xsl:param name="nextURL"/>
<xsl:param name="prevURL"/>
<xsl:param name="randomize"/>

<xsl:template match="/Results">
 <html>
  <head>
   <title>MIRC Case Navigator</title>
   <xsl:call-template name="style"/>
   <xsl:call-template name="script"/>
  </head>
  <body scroll="no">
   <xsl:call-template name="navdiv"/>
   <iframe id="MIRCinnerframeID" name="MIRCinnerframe">-</iframe>
  </body>
 </html>
</xsl:template>

<xsl:template name="style">
 <style>
  body {margin:0; padding:0}
  iframe {border:1px #aaccdd solid; width:100%; height:25%}
  .navdiv {width:100%; padding:0; background-color:#c6d8f9;}
  .b {width:90}
  .bq {width:90}
  .name {font-weight:bold; font-family:sans-serif; vertical-align:baseline; font-size:small; color:#004E96;}
  .buttons {font-weight:bold; font-family:sans-serif; vertical-align:baseline; font-size:larger;}
  .casenumbers {align:right;}
 </style>
</xsl:template>

<xsl:template name="navdiv">
 <div id="navdiv" class="navdiv">
  <table width="100%">
   <tr valign="center">

    <td align="left" class="buttons">

    	<xsl:if test="$prevURL">
			<img src="/icons/go-first.png"
				 style="margin-right:7px"
				 title="Previous query results page"
				 onclick="window.open('{$prevURL}','_self')"/>
		</xsl:if>

		<img src="/icons/go-previous.png"
			 style="margin-right:7px"
			 title="Previous case"
			 onclick="load(-1)"/>

		<xsl:if test="not($suppressHome='yes')">
			<img src="/icons/go-home.png"
				 style="margin-right:7px"
				 title="Return to the home page"
				 onclick="window.open('{$homeURL}','_self')"/>
		</xsl:if>

		<img src="/icons/go-next.png"
			 style="margin-right:7px"
			 title="Next case"
			 onclick="load(+1)"/>

    	<xsl:if test="$nextURL">
			<img src="/icons/go-last.png"
				 style="margin-right:10px"
				 title="Next query results page"
				 onclick="window.open('{$nextURL}','_self')"/>
		</xsl:if>

    </td>

    <td align="right" class="casenumbers">
		<span class="name">MIRC case navigator:&#160;&#160;</span>
		<span id="casenumberdisplay" class="name">Case 1 of 43</span>
    </td>

   </tr>
  </table>
 </div>
</xsl:template>

<xsl:template name="script">
 <script>
	var caselist = new Array(<xsl:for-each select="//MIRCdocument/@docref">
		"<xsl:value-of select="."/>"<xsl:if test="position() != last()">,</xsl:if>
	</xsl:for-each>);
	<xsl:if test="$randomize = 'yes'">randomizeList = true;
	</xsl:if>
	<xsl:if test="not($randomize = 'yes')">randomizeList = false;
	</xsl:if>
 </script>
 <xsl:text> </xsl:text>
 <script src="/query/CaseNavigatorResult.js">
 </script>
</xsl:template>

</xsl:stylesheet>

<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:param name="rebuildInProgress">no</xsl:param>
<xsl:param name="ssparams"/>

<xsl:template match="/mirc">
	<xsl:variable name="siteurl" select="@siteurl"/>
	<html>
		<head>
			<title>Storage Service Admin</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/ssadmin/StorageServiceAdmin.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/ssadmin/StorageServiceAdmin.js">;</script>
		</head>
		<body>
			<div class="closebox">
				<img src="/icons/home.png"
					 onclick="window.open('/query','_self');"
					 title="Return to the home page"/>
				<br/>
				<img src="/icons/save.png"
					 onclick="save();"
					 title="Save"/>
			</div>

			<h1>Storage Service Admin</h1>
			<form id="formID" action="" method="POST" accept-charset="UTF-8">
			<xsl:variable name="localLibraries" select="Libraries/Library[@local='yes']"/>
			<xsl:variable name="count" select="count($localLibraries)"/>

			<center>
				<xsl:for-each select="$localLibraries">
				<xsl:sort select="title"/>
				<xsl:variable name="id" select="@id"/>
				<xsl:variable name="numdocs" select="$ssparams/ssparams/ss[@id=$id]/@size"/>

				<p class="note">
					The table below controls the library with the context "<b><xsl:value-of select="@id"/></b>".
				</p>

				<table border="1">
					<tr>
						<td>URL base</td>
						<td><xsl:value-of select="$siteurl"/>/storage/<xsl:value-of select="@id"/></td>
					</tr>
					<tr>
						<td>Documents directory</td>
						<td><xsl:value-of select="$ssparams/ssparams/ss[@id=$id]/@dir"/></td>
					</tr>
					<tr>
						<td>Indexed documents</td>
						<td><xsl:value-of select="$numdocs"/></td>
					</tr>
					<tr>
						<td>Title</td>
						<td><input class="text" type="text" name="{@id}-title" value="{title}"/></td>
					</tr>
					<tr>
						<td>Tag line</td>
						<td><input class="text" type="text" name="{@id}-tagline" value="{tagline}"/></td>
					</tr>
					<tr>
						<td>Deleted documents timeout (days, default=0)</td>
						<td><input class="text" type="text" name="{@id}-timeout" value="{@timeout}"/></td>
					</tr>
					<tr>
						<td>Maximum upload size (MB, default=75)</td>
						<td><input class="text" type="text" name="{@id}-maxsize" value="{@maxsize}"/></td>
					</tr>
					<tr>
						<td>JPEG quality (0-100, default=-1)</td>
						<td><input class="text" type="text" name="{@id}-jpegquality" value="{@jpegquality}"/></td>
					</tr>
					<tr>
						<td>Automatic indexing enabled</td>
						<td>
							<input type="checkbox" name="{@id}-autoindex" value="autoindex">
								<xsl:if test="@autoindex='yes'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td>Author Service enabled</td>
						<td>
							<input type="checkbox" name="{@id}-authenb" value="yes">
								<xsl:if test="@authenb='yes'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td>Submit Service enabled</td>
						<td>
							<input type="checkbox" name="{@id}-subenb" value="yes">
								<xsl:if test="@subenb='yes'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td>Zip Service enabled</td>
						<td>
							<input type="checkbox" name="{@id}-zipenb" value="yes">
								<xsl:if test="@zipenb='yes'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td>DICOM Service enabled</td>
						<td>
							<input type="checkbox" name="{@id}-dcmenb" value="yes">
								<xsl:if test="@dcmenb='yes'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td>TCE Service enabled</td>
						<td>
							<input type="checkbox" name="{@id}-tceenb" value="yes">
								<xsl:if test="@tceenb='yes'"><xsl:attribute name="checked">checked</xsl:attribute></xsl:if>
							</input>
						</td>
					</tr>
					<tr>
						<td class="buttons" colspan="2">
							<xsl:if test="not($numdocs=0)">
								<input class="button" type="button" value="List the index" onclick="window.open('/ssadmin/list/{@id}','_self');"/>
							</xsl:if>
							<xsl:if test="not($rebuildInProgress='yes') and not($count=1)">
								<xsl:if test="not($numdocs=0)">&#160;&#160;&#160;&#160;</xsl:if>
								<input class="button" type="button" value="Remove this Storage Service" onclick="window.open('/ssadmin/remove/{@id}','_self');"/>
							</xsl:if>
						</td>
					</tr>
				</table>
				</xsl:for-each>

				<p class="center">
					<input class="button" type="button" value="Create New Storage Service" onclick="window.open('/ssadmin/new','_self');"/>
					<xsl:if test="not($rebuildInProgress='yes')">
						<br/>
						<input class="button" type="button" value="Rebuild All Indexes" onclick="window.open('/ssadmin/rebuild','_self');"/>
					</xsl:if>
				</p>
			</center>

			</form>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
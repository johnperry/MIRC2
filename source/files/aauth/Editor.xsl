<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.1">

<xsl:param name="prefs"/>
<xsl:param name="ssid"/>
<xsl:param name="dirpath"/>
<xsl:param name="authpath"/>
<xsl:param name="date"/>
<xsl:param name="mode"/>
<xsl:param name="icons"/>
<xsl:param name="options"/>
<xsl:param name="species"/>
<xsl:param name="version">Z1</xsl:param>
<xsl:param name="activetab">1</xsl:param>

<xsl:param name="draft" select="/MIRCdocument/@temp"/>

<xsl:template match="*|@*">
  <xsl:copy>
    <xsl:apply-templates select="*|@*|text()" />
  </xsl:copy>
</xsl:template>

<xsl:template match="text()">
  <xsl:variable name="text">
  	<xsl:value-of select="translate(.,'&#xA;',' ')"/>
  </xsl:variable>
  <xsl:choose>
  	<xsl:when test="string-length($text) != 0">
  		<xsl:copy-of select="$text"/>
  	</xsl:when>
  	<xsl:otherwise>
	    <xsl:text> </xsl:text>
	</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="/MIRCdocument">
	<html>
		<head>
			<title>MIRC Advanced Author Service</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"> </link>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSTree.css"> </link>
			<link rel="Stylesheet" type="text/css" media="all" href="/aauth/Editor.css"> </link>
			<xsl:if test="$mode='vet'">
				<xsl:call-template name="vet-breed-list"/>
			</xsl:if>
			<xsl:call-template name="vars"/>
			<script src="/JSUtil.js"> </script>
			<script src="/JSAJAX.js"> </script>
			<script src="/JSPopup.js"> </script>
			<script src="/JSTree.js"> </script>
			<script src="/aauth/Editor.js"> </script>
			<script src="/aauth/svg-editor.js"> </script>
		</head>
		<body>
			<div id="mainEditorDiv" class="mainEditorDiv">
				<xsl:call-template name="toolbar"/>
				<xsl:call-template name="tabs"/>
				<xsl:call-template name="editor"/>
				<xsl:call-template name="palette"/>
				<xsl:call-template name="empty-patient"/>
				<xsl:call-template name="empty-phi-study"/>
				<xsl:call-template name="author-service-form"/>
			</div>
			<div id="svgEditorDiv" class="svgEditorDiv">
				<xsl:call-template name="svg-toolbar"/>
				<div id="svgDiv"/>
			</div>
		</body>
	</html>
</xsl:template>

<xsl:template name="vars">
<xsl:variable name="as-mode">
	<xsl:choose>
		<xsl:when test="@as-mode">
			<xsl:value-of select="@as-mode"/>
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>false</xsl:text>
		</xsl:otherwise>
	</xsl:choose>
</xsl:variable>
<script>
	var ssid = "<xsl:value-of select="$ssid"/>";
	var dirpath = "<xsl:value-of select="$dirpath"/>";
	var mode = "<xsl:value-of select="$mode"/>";
	var show = <xsl:value-of select="$as-mode"/>;
	var firstTabAttribute = "<xsl:value-of select="@first-tab"/>";
	var dsPHI = <xsl:value-of select="count(//insert-dataset[@phi='yes'])"/>;
	var dsNoPHI = <xsl:value-of select="count(//insert-dataset[not(@phi='yes')])"/>;
	<xsl:text>var insertElementsPresent = </xsl:text>
	<xsl:choose>
		<xsl:when test="//insert-image">
			<xsl:text>true;</xsl:text>
		</xsl:when>
		<xsl:otherwise>
			<xsl:text>false;</xsl:text>
		</xsl:otherwise>
	</xsl:choose>
</script>
</xsl:template>

<xsl:template match="center">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template name="toolbar">
	<div id="toolbar" class="toolbar">
		<span class="group">
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Show hidden elements</xsl:with-param>
				<xsl:with-param name="onclick">showhideClicked(event);</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/show.gif</xsl:with-param>
				<xsl:with-param name="id">showhide-button</xsl:with-param>
			</xsl:call-template>
		</span>
		<xsl:text>&#160;&#160;</xsl:text>
		<span class="group">
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Save the document on the MIRC server</xsl:with-param>
				<xsl:with-param name="onclick">saveClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/save.gif</xsl:with-param>
				<xsl:with-param name="id">save-button</xsl:with-param>
			</xsl:call-template>
			<xsl:if test="not($draft='yes')">
				<xsl:call-template name="tool">
					<xsl:with-param name="title">Save the document and preview it in a separate window</xsl:with-param>
					<xsl:with-param name="onclick">previewClicked();</xsl:with-param>
					<xsl:with-param name="src">/aauth/buttons/preview.gif</xsl:with-param>
					<xsl:with-param name="id">preview-button</xsl:with-param>
				</xsl:call-template>
			</xsl:if>
		</span>

		<span class="toolLabel">Sections:&#160;</span>
		<span class="group">
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert a new section</xsl:with-param>
				<xsl:with-param name="onclick">sectionInsertClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertsection.gif</xsl:with-param>
				<xsl:with-param name="id">insertsection-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Remove the currently displayed section</xsl:with-param>
				<xsl:with-param name="onclick">sectionRemoveClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/cut.gif</xsl:with-param>
				<xsl:with-param name="id">removesection-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Move the current section to the left</xsl:with-param>
				<xsl:with-param name="onclick">sectionPromoteClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/left.gif</xsl:with-param>
				<xsl:with-param name="id">promotesection-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Move the current section to the right</xsl:with-param>
				<xsl:with-param name="onclick">sectionDemoteClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/right.gif</xsl:with-param>
				<xsl:with-param name="id">demotesection-button</xsl:with-param>
			</xsl:call-template>
		</span>

		<span class="toolLabel">Items:&#160;</span>
		<span class="group">
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert a paragraph</xsl:with-param>
				<xsl:with-param name="onclick">objectInsertParaClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertparagraph.gif</xsl:with-param>
				<xsl:with-param name="id">insertparagraph-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert a caption</xsl:with-param>
				<xsl:with-param name="onclick">objectInsertCaptionClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertcaption.gif</xsl:with-param>
				<xsl:with-param name="id">insertcaption-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert the selected images from the file cabinet</xsl:with-param>
				<xsl:with-param name="onclick">objectInsertImagesClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertimage.gif</xsl:with-param>
				<xsl:with-param name="id">insertimage-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert a patient</xsl:with-param>
				<xsl:with-param name="onclick">objectInsertPatientClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertpatient.gif</xsl:with-param>
				<xsl:with-param name="id">insertpatient-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert an external web page</xsl:with-param>
				<xsl:with-param name="onclick">objectInsertIFrameClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertiframe.gif</xsl:with-param>
				<xsl:with-param name="id">insertiframe-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Insert a quiz</xsl:with-param>
				<xsl:with-param name="onclick">objectInsertQuizClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/insertquiz.gif</xsl:with-param>
				<xsl:with-param name="id">insertquiz-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Remove the selected items</xsl:with-param>
				<xsl:with-param name="onclick">objectRemoveClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/cut.gif</xsl:with-param>
				<xsl:with-param name="id">removeobject-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Move the selected items up</xsl:with-param>
				<xsl:with-param name="onclick">objectPromoteClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/up.gif</xsl:with-param>
				<xsl:with-param name="id">promoteobject-button</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Move the selected items down</xsl:with-param>
				<xsl:with-param name="onclick">objectDemoteClicked();</xsl:with-param>
				<xsl:with-param name="src">/aauth/buttons/down.gif</xsl:with-param>
				<xsl:with-param name="id">demoteobject-button</xsl:with-param>
			</xsl:call-template>
		</span>
	</div>
</xsl:template>

<xsl:template name="tool">
	<xsl:param name="title"/>
	<xsl:param name="onclick"/>
	<xsl:param name="src"/>
	<xsl:param name="id"/>
	<span class="toolbutton" onclick="{$onclick}" title="{$title}">
		<img id="{$id}" src="{$src}"/>
	</span>
<xsl:text>
</xsl:text>
</xsl:template>

<xsl:template name="tabs">
	<div id="tabs" class="tabs">
		<input
			type="button"
			class="deselectedTab"
			value="Document"
			onclick="tabClicked(event);"
			hideable="true">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="Title"
			onclick="tabClicked(event);">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="Authors"
			onclick="tabClicked(event);">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="Abstract"
			onclick="tabClicked(event);">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="Keywords"
			onclick="tabClicked(event);">
		</input>

		<xsl:for-each select="section | image-section">
			<input type="button" class="deselectedTab" onclick="tabClicked(event);">
				<xsl:attribute name="value">
					<xsl:choose>
						<xsl:when test="not(string-length(normalize-space(@heading)) = 0)">
							<xsl:value-of select="@heading"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if test="name(.)='section'">
								<xsl:text>Section Heading</xsl:text>
							</xsl:if>
							<xsl:if test="name(.)='image-section'">
								<xsl:text>Image Section</xsl:text>
							</xsl:if>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
			</input>
		</xsl:for-each>

		<input
			type="button"
			class="deselectedTab"
			value="References"
			onclick="tabClicked(event);">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="Indexed Content"
			onclick="tabClicked(event);"
			hideable="true">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="PHI"
			onclick="tabClicked(event);"
			hideable="true">
		</input>
		<input
			type="button"
			class="deselectedTab"
			value="Permissions"
			onclick="tabClicked(event);"
			hideable="true">
		</input>
		<hr/>
	</div>
</xsl:template>

<xsl:template name="editor">
	<div id="editor" class="editor">
		<xsl:call-template name="document-description"/>
		<xsl:call-template name="title"/>
		<xsl:call-template name="authors"/>
		<xsl:call-template name="abstract"/>
		<xsl:call-template name="keywords"/>

		<xsl:for-each select="section | image-section">
			<xsl:if test="name(.)='section'">
				<xsl:call-template name="section"/>
			</xsl:if>
			<xsl:if test="name(.)='image-section'">
				<xsl:call-template name="image-section"/>
			</xsl:if>
		</xsl:for-each>

		<xsl:call-template name="references"/>
		<xsl:call-template name="index-elements"/>
		<xsl:call-template name="phi"/>
		<xsl:call-template name="permissions"/>
	</div>
</xsl:template>

<xsl:template name="palette">
	<div id="palette" class="palette">
		<div id="cabinetdiv" class="cabinetdiv">
			<input type="button" class="cabinetbutton" value="Cabinet" onclick="cabinetClicked();"></input>
		</div>
		<div id="filesdiv" class="filesdiv">
		</div>
	</div>
</xsl:template>

<xsl:template name="document-description">
	<div class="sectionPage" type="document-description">
		<h1>Document</h1>
		<center>
			<p class="p3">
				Specify the type of document you are creating.
				To create a normal document, for example a teaching file case, click
				the first radio button and select a display format.
			</p>
			<p class="p3">
				Documents that are created only to provide a reference to a file are
				called index cards. To create an index card, click
				the second radio button below and enter the URL of the referenced file
				in the field to its right.
			</p>
			<p class="p3" style="margin-bottom:20">
				Enter as much other information as you want in this section and other sections.
				To save your work, click the Save button at the left side of the tool bar.
				You can change anything you like at any time if the result
				is not what you want.
			</p>
			<p class="p3">
				To request that this document be made publicly visible, check the box at the bottom of the page.
			</p>

			<table border="1">
				<tr>
					<td rowspan="3">
						<input type="radio" name="typebutton" id="normaltype" value="normal">
							<xsl:if test="not(@docref)">
								<xsl:attribute name="checked">true</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Display format:</td>
					<td>
						<select id="display" style="width:300"
							onchange="setForDisplayMode();">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space(@display)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/display"/>
								<xsl:with-param name="use-value-attribute">yes</xsl:with-param>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Background color:</td>
					<td>
						<select id="bg" style="width:300">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space(@background)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/background"/>
								<xsl:with-param name="use-value-attribute">yes</xsl:with-param>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>First tab:</td>
					<td>
						<select id="first-tab" style="width:300">
						</select>
					</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="typebutton" id="indexcardtype" value="indexcard">
							<xsl:if test="@docref">
								<xsl:attribute name="checked">true</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Index card:</td>
					<td>
						<input type="text" id="docref" style="width:283">
							<xsl:attribute name="value">
								<xsl:value-of select="@docref"/>
							</xsl:attribute>
						</input>
						<img onclick="setDocref(event);"
							style="vertical-align:bottom; margin-bottom:3"
							title="Insert a reference to the selected object in the file cabinet"
							src="/aauth/buttons/left-small.gif" />
					</td>
				</tr>
			</table>
			<br/>
			<table>
				<tr>
					<td>Document type:</td>
					<td>
						<select id="doctype" style="width:300">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space(document-type)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/document-type"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Category:</td>
					<td>
						<select id="category" style="width:300">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space(category)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/category"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Level:</td>
					<td>
						<select id="level" style="width:300">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space(level)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/level"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Language:</td>
					<td>
						<select id="lang" style="width:300">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space(language)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/language"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Copyright:</td>
					<td>
						<textarea id="copyright" style="width:300;height:100">
							<xsl:value-of select="normalize-space(rights)"/>
						</textarea>
					</td>
				</tr>
				<tr><td>&#160;</td></tr>
				<tr>
					<td>Publication date:</td>
					<td id="pubdate">
						<xsl:choose>
							<xsl:when test="string-length(normalize-space($date)) = 0">
								<xsl:value-of select="normalize-space(publication-date)"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="$date"/>
							</xsl:otherwise>
						</xsl:choose>
					</td>
				</tr>
				<tr>
					<td>Creator:</td>
					<td id="creator">
						<xsl:text>MIRC Author Service - version </xsl:text>
						<xsl:value-of select="$version"/>
					</td>
				</tr>
				<tr>
					<td colspan="2">
						Check this box to request publication of this document:
						<input type="checkbox" id="pubreq" name="pubreq">
							<xsl:if test="@pubreq='yes'">
								<xsl:attribute name="checked">true</xsl:attribute>
							</xsl:if>
						</input>
					</td>
				</tr>
			</table>
		</center>
	</div>
</xsl:template>

<xsl:template name="title">
	<xsl:variable name="norm-title" select="normalize-space(title)"/>
	<xsl:variable name="norm-alttitle" select="normalize-space(alternative-title)"/>
	<div class="sectionPage" type="title">
		<h1>Title</h1>
		<p class="p1">
			Enter the title of the document as you want it displayed for normal viewing.
		</p>
		<textarea class="title">
			<xsl:choose>
				<xsl:when test="($draft='yes') and ($norm-alttitle!='')">
					<xsl:value-of select="$norm-alttitle"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$norm-title"/>
				</xsl:otherwise>
			</xsl:choose>
		</textarea>
		<br/><br/><br/>
		<p class="p1" hideable="true">
			Enter the title of the document as you want it displayed when the user
			requests that the document be presented as an unknown case. If you do not
			want this feature, you can leave this field blank.
		</p>
		<textarea class="title" hideable="true">
			<xsl:value-of select="$norm-alttitle"/>
		</textarea>
	</div>
</xsl:template>

<xsl:template name="authors">
	<div class="sectionPage" type="authors">
		<h1>Authors</h1>
		<p class="p1">
			Enter the names and other information of the
			document's authors. Create additional authors by clicking the button at
			the bottom of the page. To remove an author, remove the text in the author's
			name field.
		</p>
		<xsl:variable name="authors" select="author[ (name!='') and not(contains(name, '(draft)')) ]"/>
		<xsl:for-each select="$authors">
			<xsl:call-template name="author">
				<xsl:with-param name="author-name" select="name"/>
				<xsl:with-param name="author-affiliation" select="affiliation"/>
				<xsl:with-param name="author-contact" select="contact"/>
			</xsl:call-template>
		</xsl:for-each>
		<xsl:if test="not($authors) or ($draft='yes')">
			<xsl:call-template name="author">
				<xsl:with-param name="author-name" select="$prefs/User/@name"/>
				<xsl:with-param name="author-affiliation" select="$prefs/User/@affiliation"/>
				<xsl:with-param name="author-contact" select="$prefs/User/@contact"/>
			</xsl:call-template>
		</xsl:if>
		<p class="p4">
			<input type="button" value="Add a new author" onclick="cloneParagraph(event);"></input>
		</p>
	</div>
</xsl:template>

<xsl:template name="author">
	<xsl:param name="author-name"/>
	<xsl:param name="author-affiliation"/>
	<xsl:param name="author-contact"/>
	<p class="p2">
		Author's name:
		<br/>
		<input type="text" class="author">
			<xsl:attribute name="value">
				<xsl:value-of select="normalize-space($author-name)"/>
			</xsl:attribute>
		</input>
		<br/>
		Author's affiliation:
		<br/>
		<input type="text" class="author">
			<xsl:attribute name="value">
				<xsl:value-of select="normalize-space($author-affiliation)"/>
			</xsl:attribute>
		</input>
		<br/>
		Author's contact information (phone, email, etc.):
		<br/>
		<input type="text" class="author">
			<xsl:attribute name="value">
				<xsl:value-of select="normalize-space($author-contact)"/>
			</xsl:attribute>
		</input>
	</p>
</xsl:template>

<xsl:template name="abstract">
	<div class="sectionPage" type="abstract">
		<h1>Abstract</h1>
		<p class="p1">
			Enter the abstract of the document as you want it displayed for normal viewing.
			Try to keep the abstract under 200 words.
		</p>
		<textarea class="abstract">
			<xsl:for-each select="abstract/p">
				<xsl:apply-templates select="*|text()"/>
				<xsl:text>&#xA;&#xA;</xsl:text>
			</xsl:for-each>
		</textarea>
		<br/><br/><br/>
		<p class="p1" hideable="true">
			Enter the abstract of the document as you want it displayed when the user
			requests that the document be presented as an unknown case. If you do not
			want this feature, you can leave this field blank.
		</p>
		<textarea class="abstract" hideable="true">
			<xsl:for-each select="alternative-abstract/p">
				<xsl:apply-templates select="*|text()"/>
				<xsl:text>&#xA;&#xA;</xsl:text>
			</xsl:for-each>
		</textarea>
	</div>
</xsl:template>

<xsl:template name="keywords">
	<div class="sectionPage" type="keywords">
		<h1>Keywords</h1>
		<p class="p1">
			Enter the keywords by which you want the document to be indexed.
			Separate the keywords by spaces.
		</p>
		<textarea class="abstract">
			<xsl:value-of select="normalize-space(keywords)"/>
		</textarea>
	</div>
</xsl:template>

<xsl:template name="section">
	<div class="sectionPage" type="document-section">
		<xsl:if test="@heading">
			<h1><xsl:value-of select="@heading"/></h1>
		</xsl:if>
		<xsl:if test="not(@heading)">
			<h1>Section Heading</h1>
		</xsl:if>
		<p class="p1">
			Use the Items toolbar icons to insert, remove, and rearrange content items.
		</p>
		<p class="p4" hideable="true">
			<table class="techtable">
				<tr>
					<td>Heading:</td>
					<td>
						<input type="text" class="w300" onchange="headingChanged(event);">
							<xsl:attribute name="value">
								<xsl:if test="@heading">
									<xsl:value-of select="@heading"/>
								</xsl:if>
								<xsl:if test="not(@heading)">
									<xsl:text>Image Section</xsl:text>
								</xsl:if>
							</xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td>Visible:</td>
					<td>
						<select class="w300">
							<option value="yes">
								<xsl:if test="not(@visible) or @visible='yes'">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:text>Yes</xsl:text>
							</option>
							<option value="no">
								<xsl:if test="@visible='no'">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:text>No</xsl:text>
							</option>
							<option value="owner">
								<xsl:if test="@visible='owner'">
									<xsl:attribute name="selected">true</xsl:attribute>
								</xsl:if>
								<xsl:text>Owners only</xsl:text>
							</option>
						</select>
					</td>
				</tr>
				<tr>
					<td>After (YYYYMMDD):</td>
					<td>
						<input type="text" class="w300">
							<xsl:attribute name="value">
								<xsl:value-of select="@after"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td>Maximum image width:</td>
					<td>
						<input type="text" style="width:300">
							<xsl:attribute name="value">
								<xsl:value-of select="@image-width"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td>Minimum image width:</td>
					<td>
						<input type="text" style="width:300">
							<xsl:attribute name="value">
								<xsl:value-of select="@min-width"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
			</table>
		</p>
		<div>
			<xsl:apply-templates/>
		</div>
	</div>
</xsl:template>

<xsl:template name="image-section">
	<div class="sectionPage" type="image-section">
		<xsl:if test="@heading">
			<h1><xsl:value-of select="@heading"/></h1>
		</xsl:if>
		<xsl:if test="not(@heading)">
			<h1>Image Section</h1>
		</xsl:if>
		<p class="p1">
			This section can be included in documents displayed in either the
			Tab format or the MIRC Standard Teaching File (MSTF) format. In Page
			format documents, this section is ignored.
		</p>
		<p class="p4" hideable="true">
			<table class="techtable">
				<tr>
					<td>Heading:</td>
					<td>
						<input type="text" class="w300" onchange="headingChanged(event);">
							<xsl:attribute name="value">
								<xsl:if test="@heading">
									<xsl:value-of select="@heading"/>
								</xsl:if>
								<xsl:if test="not(@heading)">
									<xsl:text>Image Section</xsl:text>
								</xsl:if>
							</xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td>Pane width:</td>
					<td>
						<input type="text" class="w300">
							<xsl:attribute name="value">
								<xsl:if test="@image-pane-width">
									<xsl:value-of select="@image-pane-width"/>
								</xsl:if>
								<xsl:if test="not(@image-pane-width)">
									<xsl:text>700</xsl:text>
								</xsl:if>
							</xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td>Minimum image width:</td>
					<td>
						<input type="text" style="width:300">
							<xsl:attribute name="value">
								<xsl:value-of select="@min-width"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
				<tr>
					<td>Suppress icons:</td>
					<td>
						<input type="checkbox" value="no">
							<xsl:if test="@icons='no'">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
				</tr>
				<tr>
					<td>Include insertion point:</td>
					<td>
						<input type="checkbox" value="yes">
							<xsl:if test="insert-megasave">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
				</tr>
			</table>
		</p>
		<div>
			<p class="p4">
				<xsl:for-each select="image">
					<xsl:call-template name="img">
						<xsl:with-param name="image" select="."/>
						<xsl:with-param name="href"/>
						<xsl:with-param name="onclick">sectionImageClicked(event);</xsl:with-param>
						<xsl:with-param name="ondblclick">sectionImageDblClicked(event);</xsl:with-param>
					</xsl:call-template>
					<xsl:text> </xsl:text>
				</xsl:for-each>
			</p>
		</div>
		<p class="p4">
			<input type="button"
				value="Insert the selected images from the file cabinet"
				onclick="objectInsertImagesClicked();">
			</input>
		</p>
		<!--<xsl:apply-templates select="insert-image | insert-megasave"/>-->
	</div>
</xsl:template>

<xsl:template name="references">
	<div class="sectionPage" type="references">
		<h1>References</h1>
		<p class="p1">
			This section allows you to enter references. Enter one reference in each
			field. To insert additional references, click the button at the bottom
			of the page. To remove a reference, remove all the text in its field.
		</p>
		<xsl:for-each select="references/reference">
			<xsl:call-template name="reference">
				<xsl:with-param name="ref" select="."/>
			</xsl:call-template>
		</xsl:for-each>
		<xsl:if test="not(references/reference)">
			<xsl:call-template name="reference">
				<xsl:with-param name="ref" select="empty-note-list"/>
			</xsl:call-template>
		</xsl:if>
		<p class="p4">
			<input type="button" value="Add a new reference" onclick="cloneParagraph(event);">
			</input>
		</p>
	</div>
</xsl:template>

<xsl:template name="reference">
	<xsl:param name="ref"/>
	<p>
		<textarea class="reference">
			<xsl:apply-templates select="$ref/* | $ref/text()"/>
		</textarea>
	</p>
</xsl:template>

<xsl:template name="permissions">
	<div class="sectionPage" type="permissions">
		<h1>Permissions</h1>
		<p class="p1">
			This section allows you to control who can read, update (modify), or export the document.
		</p>
		<p class="p1">
			Owners have all privileges with respect to a document, including the privilege
			to delete it from the server. You can assign additional owners here by including
			their usernames, separated by spaces or commas.
		</p>
		<p class="p4">
			<table class="pmtable">
				<xsl:variable name="owner-field" select="normalize-space(authorization/owner)"/>
				<tr>
					<td class="pmlabel">Owner:</td>
					<td>
						<input type="text" style="width:340">
							<xsl:attribute name="value">
								<xsl:choose>
									<xsl:when test="string-length($owner-field) = 0">
										<xsl:value-of select="$prefs/User/@username"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="$owner-field"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:attribute>
						</input>
					</td>
				</tr>
			</table>
		</p>
		<p class="p1">
			Select who can view this document.
			If you wish to specify individual users or groups of users (defined by
			their roles), you can
			click the third radio button and list the usernames and/or roles in the text field.
			Enter usernames in square brackets like this: "role1 role2 [userA] [userB]".
		</p>
		<p class="p4">
			<table class="pmtable">
				<xsl:variable name="read-field" select="normalize-space(authorization/read)"/>
				<tr>
					<td rowspan="3" class="pmlabel">Read:</td>
					<td>
						<input type="radio" name="read" value="public">
							<xsl:if test="contains($read-field,'*')">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Public (all users, including those without accounts)</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="read" value="private">
							<xsl:if test="string-length($read-field)=0">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Private (owners only)</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="read" value="specify">
							<xsl:if test="not(contains($read-field,'*')) and not(string-length($read-field)=0)">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>
						<input type="text" style="width:310">
							<xsl:attribute name="value">
								<xsl:value-of select="$read-field"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
			</table>
		</p>
		<p class="p1">
			Select who can edit or update this document.
		</p>
		<p class="p4">
			<table class="pmtable">
				<xsl:variable name="update-field" select="normalize-space(authorization/update)"/>
				<tr>
					<td rowspan="3" class="pmlabel">Update:</td>
					<td>
						<input type="radio" name="update" value="public">
							<xsl:if test="contains($update-field,'*')">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Public (all users who have the author role)</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="update" value="private">
							<xsl:if test="string-length($update-field)=0">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Private (owners only)</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="update" value="specify">
							<xsl:if test="not(contains($update-field,'*')) and not(string-length($update-field)=0)">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>
						<input type="text" style="width:310">
							<xsl:attribute name="value">
								<xsl:value-of select="$update-field"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
			</table>
		</p>
		<p class="p1">
			Select who can export this document, including all the images and files it references.
		</p>
		<p class="p4">
			<table class="pmtable">
				<xsl:variable name="export-field" select="normalize-space(authorization/export)"/>
				<tr>
					<td rowspan="3" class="pmlabel">Export:</td>
					<td>
						<input type="radio" name="export" value="public">
							<xsl:if test="contains($export-field,'*')">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Public (all users, including those without accounts)</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="export" value="private">
							<xsl:if test="string-length($export-field)=0">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>Private (owners only)</td>
				</tr>
				<tr>
					<td>
						<input type="radio" name="export" value="specify">
							<xsl:if test="not(contains($export-field,'*')) and not(string-length($export-field)=0)">
								<xsl:attribute name="checked">
									<xsl:text>true</xsl:text>
								</xsl:attribute>
							</xsl:if>
						</input>
					</td>
					<td>
						<input type="text" style="width:310">
							<xsl:attribute name="value">
								<xsl:value-of select="$export-field"/>
							</xsl:attribute>
						</input>
					</td>
				</tr>
			</table>
		</p>
	</div>
</xsl:template>

<xsl:template name="index-elements">
	<div class="sectionPage" type="index-elements">
		<h1>Indexed Content</h1>
		<p class="p1">
			Enter any information that you want to be indexed but not displayed
			in the document. This information is included in the index of the document's
			contents, and you can use it to find the document through a MIRC query. To
			make this section most effective, include only the terms by which you would
			expect to find this document.
		</p>
		<h2>History</h2>
		<textarea class="content"><xsl:value-of select="history"/></textarea>
		<h2>Findings</h2>
		<textarea class="content"><xsl:value-of select="findings"/></textarea>
		<h2>Diagnosis</h2>
		<textarea class="content"><xsl:value-of select="diagnosis"/></textarea>
		<h2>Differential Diagnosis</h2>
		<textarea class="content"><xsl:value-of select="differential-diagnosis"/></textarea>
		<h2>Discussion</h2>
		<textarea class="content"><xsl:value-of select="discussion"/></textarea>
		<h2>Anatomy</h2>
		<textarea class="content"><xsl:value-of select="anatomy"/></textarea>
		<h2>Pathology</h2>
		<textarea class="content"><xsl:value-of select="pathology"/></textarea>
		<h2>Organ System</h2>
		<textarea class="content"><xsl:value-of select="organ-system"/></textarea>
		<h2>Modalities</h2>
		<textarea class="content"><xsl:value-of select="modality"/></textarea>
		<h2>Clinical Codes</h2>
		<textarea class="content"><xsl:value-of select="code"/></textarea>
		<div>
			<xsl:for-each select="patient">
				<xsl:call-template name="patient">
					<xsl:with-param name="pt" select="."/>
				</xsl:call-template>
			</xsl:for-each>
		</div>
	</div>
</xsl:template>

<xsl:template name="empty-patient">
	<div class="hide" id="empty-patient">
		<xsl:call-template name="patient">
			<xsl:with-param name="pt" select="empty-node-list"/>
		</xsl:call-template>
	</div>
</xsl:template>

<xsl:template name="empty-phi-study">
	<div class="hide" id="empty-phi-study">
		<xsl:call-template name="phi-study">
			<xsl:with-param name="study" select="empty-node-list"/>
		</xsl:call-template>
	</div>
</xsl:template>

<xsl:template name="phi">
	<div class="sectionPage" type="phi">
		<h1>Protected Health Information</h1>
		<p class="p1a">
			When MIRC servers distribute documents containing Protected Health
			Information (PHI, including the patient's name, ID, study ID, etc.),
			they automatically log the access in accordance with HIPAA rules.
			It is the author's responsibility to indicate in this section that the
			document contains PHI. If this document contains PHI, either in the text
			of the document or in any images referenced by the document, enter the
			study identifier, patient ID, and patient name to be logged when the
			document is accessed.
		</p>
		<p class="p1">
			It is also the author's responsibility to ensure that the document is
			accessible only to authorized personnel. Use the Permissions section to
			control access to the document.
		</p>
		<div>
			<xsl:choose>
				<xsl:when test="phi">
					<xsl:for-each select="phi/study">
						<xsl:call-template name="phi-study">
							<xsl:with-param name="study" select="."/>
						</xsl:call-template>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="phi-study">
						<xsl:with-param name="study" select="empty-node-list"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</div>
		<p class="p4">
			<input type="button" value="Insert a study block"
				onclick="objectInsertPHIStudyClicked();">
			</input>
		</p>
	</div>
</xsl:template>

<xsl:template name="phi-study">
	<xsl:param name="study"/>
	<p class="p4" item-type="phi-study">
		<table class="pttable">
			<tr>
				<td>Study Instance UID:</td>
				<td>
					<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($study/si-uid)"/>
						</xsl:attribute>
					</input>
				</td>
			</tr>
			<tr>
				<td>Patient ID:</td>
				<td>
					<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($study/pt-id)"/>
						</xsl:attribute>
					</input>
				</td>
			</tr>
			<tr>
				<td>Patient Name:</td>
				<td>
					<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($study/pt-name)"/>
						</xsl:attribute>
					</input>
				</td>
			</tr>
		</table>
	</p>
</xsl:template>

<xsl:template name="patient">
	<xsl:param name="pt"/>
	<p class="p4" item-type="patient">
		<table class="pttable">
			<tr>
				<td>Name:</td>
				<td>
					<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-name)"/>
						</xsl:attribute>
					</input>
				</td>
			</tr>
			<tr>
				<td>ID:</td>
				<td>
					<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-id)"/>
						</xsl:attribute>
					</input>
				</td>
			</tr>
			<tr>
				<td>MRN:</td>
				<td>
					<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-mrn)"/>
						</xsl:attribute>
					</input>
				</td>
			</tr>
			<tr>
				<td>Age:</td>
				<td>
					<input type="text" style="width:30" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-age/years)"/>
						</xsl:attribute>
					</input> years;
					<input type="text" style="width:30" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-age/months)"/>
						</xsl:attribute>
					</input> months;
					<input type="text" style="width:30" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-age/weeks)"/>
						</xsl:attribute>
					</input> weeks;
					<input type="text" style="width:30" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-age/days)"/>
						</xsl:attribute>
					</input> days
				</td>
			</tr>
			<xsl:if test="$mode='rad'">
				<tr>
					<td>Sex:</td>
					<td>
						<select style="width:310" onclick="setCurrentObject(event);">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space($pt/pt-sex)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/rad-pt-sex"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Race:</td>
					<td>
						<input type="text" style="width:310" onclick="setCurrentObject(event);">
						<xsl:attribute name="value">
							<xsl:value-of select="normalize-space($pt/pt-race)"/>
						</xsl:attribute>
						</input>
					</td>
				</tr>
			</xsl:if>
			<xsl:if test="$mode='vet'">
				<tr>
					<td>Sex:</td>
					<td>
						<select style="width:310" onclick="setCurrentObject(event);">
							<xsl:call-template name="make-options">
								<xsl:with-param name="elem-value" select="normalize-space($pt/pt-sex)"/>
								<xsl:with-param name="options" select="$options/enumerated-values/vet-pt-sex"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Species:</td>
					<td>
						<select style="width:310" onchange="setBreedList(event);" onclick="setCurrentObject(event);">
							<xsl:call-template name="make-species-options">
								<xsl:with-param name="species-value" select="normalize-space($pt/pt-species)"/>
								<xsl:with-param name="options" select="$species/vet"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
				<tr>
					<td>Breed:</td>
					<td>
						<select style="width:310" onclick="setCurrentObject(event);">
							<xsl:call-template name="make-breed-options">
								<xsl:with-param name="species-value" select="normalize-space($pt/pt-species)"/>
								<xsl:with-param name="breed-value" select="normalize-space($pt/pt-breed)"/>
								<xsl:with-param name="options" select="$species/vet"/>
							</xsl:call-template>
						</select>
					</td>
				</tr>
			</xsl:if>
			<tr>
				<td/>
				<td>
					<input type="checkbox">
						<xsl:if test="$pt/@visible='phi-restricted'">
							<xsl:attribute name="checked">
								<xsl:text>true</xsl:text>
							</xsl:attribute>
						</xsl:if>
						Suppress PHI for all users but the owner
					</input>
				</td>
			</tr>
		</table>
	</p>
</xsl:template>

<xsl:template match="quiz">
	<p class="p4" item-type="quiz">
		<table class="quiz">
			<xsl:apply-templates select="quiz-context | quizcontext"/>
			<xsl:if test="not(quiz-context)">
				<xsl:call-template name="quiz-context"/>
			</xsl:if>
			<xsl:apply-templates select="question"/>
			<tr>
				<td class="qbutton" colspan="2">
					<button class="quiz" onclick="insertQuestion(event);">Insert another question</button>
				</td>
			</tr>
		</table>
	</p>
</xsl:template>

<xsl:template match="quiz-context | quizcontext">
	<xsl:call-template name="quiz-context">
		<xsl:with-param name="qc" select="."/>
	</xsl:call-template>
</xsl:template>

<xsl:template name="quiz-context">
	<xsl:param name="qc"/>
	<tr>
		<td class="context" colspan="2">
			Enter the quiz context. If no context is necessary, leave this field blank.
			<br/>
			<textarea class="context" onclick="setCurrentObject(event);">
				<xsl:if test="$qc">
					<xsl:apply-templates select="$qc/* | $qc/text()"/>
				</xsl:if>
			</textarea>
		</td>
	</tr>
</xsl:template>

<xsl:template match="question">
	<xsl:call-template name="question-body">
		<xsl:with-param name="qb" select="question-body | questonbody"/>
	</xsl:call-template>
	<xsl:apply-templates select="answer"/>
	<tr>
		<td class="abutton" colspan="2">
			<button class="quizans" onclick="insertAnswer(event);">Insert another answer</button>
		</td>
	</tr>
</xsl:template>

<xsl:template name="question-body">
	<xsl:param name="qb"/>
	<tr>
		<td class="question" colspan="2">
			Enter a question.<br/>
			<textarea class="question" onclick="setCurrentObject(event);">
				<xsl:apply-templates select="$qb/* | $qb/text()"/>
			</textarea>
		</td>
	</tr>
</xsl:template>

<xsl:template match="answer">
	<tr>
		<td class="ans" rowspan="2">Ans:</td>
		<td class="ansa">
			Enter a possible answer.<br/>
			<textarea class="answer" onclick="setCurrentObject(event);">
				<xsl:apply-templates
					select="answer-body/* | answer-body/text() | answerbody/* | answerbody/text()"/>
			</textarea>
		</td>
	</tr>
	<tr>
		<td class="ansr">
			Enter the response to this answer.<br/>
			<textarea class="response" onclick="setCurrentObject(event);">
				<xsl:apply-templates select="response/* | response/text()"/>
			</textarea>
		</td>
	</tr>
</xsl:template>

<xsl:template name="textarea-content">
	<xsl:param name="node"/>
	<xsl:param name="class"/>
	<xsl:param name="min-rows" select="2"/>
	<xsl:param name="max-rows" select="20"/>
	<xsl:param name="row-height" select="15"/>
	<xsl:variable name="calc-rows">
		<xsl:call-template name="count-lines">
			<xsl:with-param name="node" select="$node"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="count-rows">
		<xsl:call-template name="count-lines">
			<xsl:with-param name="node" select="$node"/>
		</xsl:call-template>
	</xsl:variable>
	<xsl:variable name="calc-rows" select="string-length($node) div 40"/>
	<xsl:variable name="max-desired-rows">
		<xsl:choose>
			<xsl:when test="($count-rows &gt;= $min-rows) and ($count-rows &gt;= $calc-rows)">
				<xsl:value-of select="$count-rows"/>
			</xsl:when>
			<xsl:when test="($calc-rows &gt;= $min-rows) and ($calc-rows &gt;= $count-rows)">
				<xsl:value-of select="$max-rows"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$min-rows"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="rows">
		<xsl:choose>
			<xsl:when test="$max-desired-rows &lt; $max-rows">
				<xsl:value-of select="$max-desired-rows"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$max-rows"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<textarea onclick="setCurrentObject(event);">
		<xsl:attribute name="class">
			<xsl:value-of select="$class"/>
		</xsl:attribute>
		<xsl:attribute name="style">
			<xsl:text>height:</xsl:text>
			<xsl:value-of select="$row-height*$rows"/>
		</xsl:attribute>
		<xsl:apply-templates select="$node/* | $node/text()"/>
	</textarea>
</xsl:template>

<xsl:template name="count-lines">
	<xsl:param name="node"/>
	<xsl:choose>
		<xsl:when test="$node">
			<xsl:variable name="rest">
				<xsl:call-template name="count-lines">
					<xsl:with-param name="node" select="substring-after($node,'&#xA;')"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:value-of select="$rest + 1"/>
		</xsl:when>
		<xsl:otherwise>0</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="text-caption">
	<p class="p5" item-type="text-caption">
		<table class="captiontable">
			<tr>
				<td class="checkbox">
					<input type="checkbox" onclick="setCurrentObject(event);"
							name="break" value="yes">
						<xsl:if test="@display='always'">
							<xsl:attribute name="checked">
								<xsl:text>true</xsl:text>
							</xsl:attribute>
						</xsl:if>
					</input>
				</td>
				<td>Force a break, even if blank</td>
			</tr>
			<tr>
				<td class="checkbox">
					<input type="checkbox" onclick="setCurrentObject(event);"
							name="showhide" value="yes">
						<xsl:if test="@show-button='yes'">
							<xsl:attribute name="checked">
								<xsl:text>true</xsl:text>
							</xsl:attribute>
						</xsl:if>
					</input>
				</td>
				<td>Include Show/Hide buttons</td>
			</tr>
			<tr>
				<td class="checkbox">
					<input type="checkbox" onclick="setCurrentObject(event);"
							name="jump" value="yes">
						<xsl:if test="@jump-buttons='yes'">
							<xsl:attribute name="checked">
								<xsl:text>true</xsl:text>
							</xsl:attribute>
						</xsl:if>
					</input>
				</td>
				<td>Include Jump buttons</td>
			</tr>
			<tr>
				<td class="checkbox"></td>
				<td>
					<xsl:call-template name="textarea-content">
						<xsl:with-param name="node" select="."/>
						<xsl:with-param name="class">sectionP</xsl:with-param>
					</xsl:call-template>
				</td>
			</tr>
		</table>
	</p>
</xsl:template>

<xsl:template match="p | text">
	<xsl:choose>
		<xsl:when test=".//patient">
			<xsl:apply-templates select=".//patient"/>
		</xsl:when>
		<xsl:otherwise>
			<p class="p5" item-type="p">
				<xsl:call-template name="textarea-content">
					<xsl:with-param name="node" select="."/>
					<xsl:with-param name="class">sectionP</xsl:with-param>
					<xsl:with-param name="min-rows">10</xsl:with-param>
				</xsl:call-template>
			</p>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="ol">
	<xsl:apply-templates select="li">
		<xsl:with-param name="type">#</xsl:with-param>
	</xsl:apply-templates>
</xsl:template>

<xsl:template match="ul">
	<xsl:apply-templates select="li">
		<xsl:with-param name="type">-</xsl:with-param>
	</xsl:apply-templates>
</xsl:template>

<xsl:template match="li">
	<xsl:param name="type">-</xsl:param>
	<xsl:text>&#xA;</xsl:text>
	<xsl:value-of select="$type"/>
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="patient">
	<xsl:call-template name="patient">
		<xsl:with-param name="pt" select="."/>
	</xsl:call-template>
</xsl:template>

<xsl:template match="term">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template name="lookup-icon-name">
	<xsl:param name="src"/>
	<xsl:variable name="list" select="$icons//file[@name = $src]"/>
	<xsl:choose>
		<xsl:when test="count($list) != 0">
			<xsl:value-of select="$list[position()=1]/@icon"/>
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="$src"/>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="iframe">
	<xsl:variable name="url">
		<xsl:choose>
			<xsl:when test="src"><xsl:value-of select="src"/></xsl:when>
			<xsl:otherwise><xsl:value-of select="@src"/></xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<p class="p5" item-type="iframe">
		<table class="techtable">
			<tr><td colspan="2" style="text-align:center;">External Web Page Frame</td></tr>
			<tr>
				<td>External Page URL:</td>
				<td><input type="text" class="w300" value="{$url}" onclick="setCurrentObject(event);"></input></td>
			</tr>
			<tr>
				<td>Frame width:</td>
				<td><input type="text" class="w300" value="{@width}" onclick="setCurrentObject(event);"></input></td>
			</tr>
			<tr>
				<td>Frame height:</td>
				<td><input type="text" class="w300" value="{@height}" onclick="setCurrentObject(event);"></input></td>
			</tr>
			<tr>
				<td>Enable scrolling:</td>
				<td>
					<select class="w300" onclick="setCurrentObject(event);">
						<option value="no">
							<xsl:if test="@scrolling='no'">
								<xsl:attribute name="selected">true</xsl:attribute>
							</xsl:if>
							no
						</option>
						<option value="yes">
							<xsl:if test="@scrolling='yes'">
								<xsl:attribute name="selected">true</xsl:attribute>
							</xsl:if>
							yes
						</option>
					</select>
				</td>
			</tr>
		</table>
	</p>
</xsl:template>

<xsl:template match="image">
	<xsl:param name="ref"></xsl:param>
	<xsl:if test="@href and not(@src)">
		<xsl:apply-templates select="image">
			<xsl:with-param name="ref" select="@href"/>
		</xsl:apply-templates>
	</xsl:if>
	<xsl:if test="@src">
		<p class="p5" item-type="image">
			<xsl:call-template name="img">
				<xsl:with-param name="image" select="."/>
				<xsl:with-param name="ref" select="$ref"/>
				<xsl:with-param name="onclick">setCurrentObject(event);</xsl:with-param>
			</xsl:call-template>
		</p>
	</xsl:if>
</xsl:template>

<xsl:template name="img">
	<xsl:param name="image"/>
	<xsl:param name="ref"/>
	<xsl:param name="onclick"/>
	<xsl:param name="ondblclick"/>
	<img class="fileImg">
		<xsl:if test="string-length(normalize-space($ref)) != 0">
			<xsl:attribute name="ref">
				<xsl:value-of select="$ref"/>
			</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="onclick">
			<xsl:value-of select="$onclick"/>
		</xsl:attribute>
		<xsl:if test="string-length(normalize-space($ondblclick)) != 0">
			<xsl:attribute name="ondblclick">
				<xsl:value-of select="$ondblclick"/>
			</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="title">
			<xsl:value-of select="$image/@src"/>
		</xsl:attribute>
		<xsl:choose>
			<xsl:when test="starts-with($image/@src,'http://') or starts-with($image/@src,'/')">
				<xsl:copy-of select="$image/@src"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="srcname">
					<xsl:call-template name="lookup-icon-name">
						<xsl:with-param name="src" select="$image/@src"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:attribute name="src">
					<xsl:value-of select="$dirpath"/>
					<xsl:value-of select="$srcname"/>
				</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="$image/@width">
			<xsl:attribute name="imagewidth">
				<xsl:value-of select="$image/@width"/>
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="$image/@height">
			<xsl:attribute name="imageheight">
				<xsl:value-of select="$image/@height"/>
			</xsl:attribute>
		</xsl:if>
		<xsl:attribute name="base-image">
			<xsl:value-of select="$image/@src"/>
		</xsl:attribute>
		<xsl:attribute name="icon">
			<xsl:value-of select="$image/alternative-image/@src[../@role='icon']"/>
		</xsl:attribute>
		<xsl:attribute name="ansvgsrc">
			<xsl:value-of select=
				"$image/alternative-image/@src
					[../@role='annotation' and
						(../@type='svg' or
							(../@type='' and contains(.,'.svg')))]"
			/>
		</xsl:attribute>
		<xsl:attribute name="animagesrc">
			<xsl:value-of select=
				"$image/alternative-image/@src
					[../@role='annotation' and
						(../@type='image' or
							(../@type='' and not(contains(.,'.svg'))))]"
			/>
		</xsl:attribute>
		<xsl:attribute name="original-dimensions">
			<xsl:value-of select="$image/alternative-image/@src[../@role='original-dimensions']"/>
		</xsl:attribute>
		<xsl:attribute name="original-format">
			<xsl:value-of select="$image/alternative-image/@src[../@role='original-format']"/>
		</xsl:attribute>
		<xsl:attribute name="format">
			<xsl:value-of select="$image/format"/>
		</xsl:attribute>
		<xsl:attribute name="compression">
			<xsl:value-of select="$image/compression"/>
		</xsl:attribute>
		<xsl:attribute name="modality">
			<xsl:value-of select="$image/modality"/>
		</xsl:attribute>
		<xsl:attribute name="alwayscaption">
			<xsl:value-of select="image-caption[not(@display) or @display='always']"/>
		</xsl:attribute>
		<xsl:attribute name="clickcaption">
			<xsl:value-of select="image-caption[@display='click']"/>
		</xsl:attribute>
		<xsl:if test="order-by">
			<xsl:attribute name="orderby-study">
				<xsl:value-of select="order-by/@study"/>
			</xsl:attribute>
			<xsl:attribute name="orderby-series">
				<xsl:value-of select="order-by/@series"/>
			</xsl:attribute>
			<xsl:attribute name="orderby-acquisition">
				<xsl:value-of select="order-by/@acquisition"/>
			</xsl:attribute>
			<xsl:attribute name="orderby-instance">
				<xsl:value-of select="order-by/@instance"/>
			</xsl:attribute>
			<xsl:attribute name="orderby-date">
				<xsl:value-of select="order-by/@date"/>
			</xsl:attribute>
		</xsl:if>
	</img>
</xsl:template>

<xsl:template name="make-options">
	<xsl:param name="elem-value"/>
	<xsl:param name="options"/>
	<xsl:param name="use-value-attribute">no</xsl:param>
	<xsl:variable name="elem-value-lc"
			select="translate(normalize-space($elem-value),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
	<xsl:for-each select="$options/option">
		<xsl:variable name="option-lc"
			select="translate(normalize-space(@value),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
		<option>
			<xsl:attribute name="value">
				<xsl:choose>
					<xsl:when test="$use-value-attribute = 'no'">
						<xsl:value-of select="."/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="@value"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:if test="$elem-value-lc = $option-lc">
				<xsl:attribute name="selected">true</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="."/>
		</option>
	</xsl:for-each>
</xsl:template>

<xsl:template name="make-species-options">
	<xsl:param name="species-value"/>
	<xsl:param name="options"/>
	<xsl:variable name="species-value-lc"
			select="translate(normalize-space($species-value),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
	<option value=""/>
	<xsl:for-each select="$options/species">
		<xsl:variable name="option-lc"
			select="translate(normalize-space(@name),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
		<option>
			<xsl:attribute name="value">
				<xsl:value-of select="@name"/>
			</xsl:attribute>
			<xsl:if test="$species-value-lc = $option-lc">
				<xsl:attribute name="selected">true</xsl:attribute>
			</xsl:if>
			<xsl:value-of select="@name"/>
		</option>
	</xsl:for-each>
</xsl:template>

<xsl:template name="make-breed-options">
	<xsl:param name="species-value"/>
	<xsl:param name="breed-value"/>
	<xsl:param name="options"/>
	<xsl:variable name="species-value-lc"
			select="translate(normalize-space($species-value),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
	<xsl:variable name="breed-value-lc"
			select="translate(normalize-space($breed-value),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
	<xsl:for-each select="$options/species">
		<xsl:variable name="species-entry"
			select="translate(normalize-space(@name),
								'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
								'abcdefghijklmnopqrstuvwxyz')"/>
		<xsl:if test="$species-entry = $species-value-lc">
			<xsl:for-each select="breed">
				<option value=""/>
				<xsl:variable name="option-lc"
					select="translate(normalize-space(@name),
										'ABCDEFGHIJKLMNOPQRSTUVWXYZ&quot;|',
										'abcdefghijklmnopqrstuvwxyz')"/>
				<option>
					<xsl:attribute name="value">
						<xsl:value-of select="@name"/>
					</xsl:attribute>
					<xsl:if test="contains($option-lc,$breed-value-lc)">
						<xsl:attribute name="selected">true</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="@name"/>
				</option>
			</xsl:for-each>
		</xsl:if>
	</xsl:for-each>
</xsl:template>

<xsl:template name="vet-breed-list">
	<script>
		breedList = new Array(
			new Array(""),
			<xsl:for-each select="$species/vet/species">
				new Array(
					<xsl:text>""</xsl:text>
					<xsl:for-each select="breed">
						<xsl:text>,"</xsl:text>
						<xsl:value-of select="@name"/>
						<xsl:text>"</xsl:text>
					</xsl:for-each>
				)<xsl:if test="position()!=last()">,</xsl:if>
			</xsl:for-each>
		);
	</script>
</xsl:template>

<xsl:template name="author-service-form">
	<form id="author-service-form" method="post" action="{$authpath}" accept-charset="UTF-8">
		<input type="hidden" id="doctext" name="doctext" value=""/>
		<input type="hidden" id="preview" name="preview" value=""/>
		<input type="hidden" id="activetab" name="activetab" value="{$activetab}"/>
	</form>
</xsl:template>

<xsl:template name="svg-toolbar">
	<div id="svgToolPalette" class="svgToolPalette">
		<p class="svgToolP">
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Quit without saving changes</xsl:with-param>
				<xsl:with-param name="onclick">swapEditorDivs();</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/quit.gif</xsl:with-param>
				<xsl:with-param name="id">svgQuit</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Save changes and return to the main editor</xsl:with-param>
				<xsl:with-param name="onclick">svgSave();</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/save.gif</xsl:with-param>
				<xsl:with-param name="id">saveSVG</xsl:with-param>
			</xsl:call-template>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>

			<xsl:call-template name="tool">
				<xsl:with-param name="title">Delete</xsl:with-param>
				<xsl:with-param name="onclick">svgCut();</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/cut.gif</xsl:with-param>
				<xsl:with-param name="id">cutSVG</xsl:with-param>
			</xsl:call-template>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>

			<xsl:call-template name="tool">
				<xsl:with-param name="title">Select</xsl:with-param>
				<xsl:with-param name="onclick">setType('select');</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/arrow.gif</xsl:with-param>
				<xsl:with-param name="id">svgSelect</xsl:with-param>
			</xsl:call-template>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>

			<xsl:call-template name="tool">
				<xsl:with-param name="title">Arrow</xsl:with-param>
				<xsl:with-param name="onclick">setType('arrow');</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/arrow.gif</xsl:with-param>
				<xsl:with-param name="id">svgArrow</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Larger arrowhead</xsl:with-param>
				<xsl:with-param name="onclick">setArrowSize(+1);</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/arrowplus.gif</xsl:with-param>
				<xsl:with-param name="id">svgArrowPlus</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Smaller arrowhead</xsl:with-param>
				<xsl:with-param name="onclick">setArrowSize(-1);</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/arrowminus.gif</xsl:with-param>
				<xsl:with-param name="id">svgArrowMinus</xsl:with-param>
			</xsl:call-template>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>

			<xsl:call-template name="tool">
				<xsl:with-param name="title">Line</xsl:with-param>
				<xsl:with-param name="onclick">setType('line');</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/line-pressed.gif</xsl:with-param>
				<xsl:with-param name="id">svgLine</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Thicker line</xsl:with-param>
				<xsl:with-param name="onclick">setStrokeWidth(+1);</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/lineplus.gif</xsl:with-param>
				<xsl:with-param name="id">svgLinePlus</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Thinner line</xsl:with-param>
				<xsl:with-param name="onclick">setStrokeWidth(-1);</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/lineminus.gif</xsl:with-param>
				<xsl:with-param name="id">svgLineMinus</xsl:with-param>
			</xsl:call-template>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>

			<xsl:call-template name="tool">
				<xsl:with-param name="title">Circle</xsl:with-param>
				<xsl:with-param name="onclick">setType('circle');</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/circle.gif</xsl:with-param>
				<xsl:with-param name="id">svgCircle</xsl:with-param>
			</xsl:call-template>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>

			<xsl:call-template name="tool">
				<xsl:with-param name="title">Text</xsl:with-param>
				<xsl:with-param name="onclick">setType('text');</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/text.gif</xsl:with-param>
				<xsl:with-param name="id">svgText</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Larger font</xsl:with-param>
				<xsl:with-param name="onclick">setFontSize(+4);</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/textplus.gif</xsl:with-param>
				<xsl:with-param name="id">svgTextPlus</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Smaller font</xsl:with-param>
				<xsl:with-param name="onclick">setFontSize(-4);</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/textminus.gif</xsl:with-param>
				<xsl:with-param name="id">svgTextMinus</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Bold text</xsl:with-param>
				<xsl:with-param name="onclick">setFontWeight();</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/bold.gif</xsl:with-param>
				<xsl:with-param name="id">svgbold</xsl:with-param>
			</xsl:call-template>
			<xsl:call-template name="tool">
				<xsl:with-param name="title">Italic text</xsl:with-param>
				<xsl:with-param name="onclick">setFontStyle();</xsl:with-param>
				<xsl:with-param name="src">/aauth/svg-buttons/italic.gif</xsl:with-param>
				<xsl:with-param name="id">svgItalic</xsl:with-param>
			</xsl:call-template>
			<select id="svgFontSelector" onchange="setFont();" onmouseup="setFont();">
				<option value="serif" style="font:serif">Serif</option>
				<option value="sans-serif" style="font:sans-serif">Sans-Serif</option>
				<option value="monospace" style="font:monospace">Monospace</option>
			</select>

			<img src="/aauth/svg-buttons/spacer.gif" style="width:10"/>


<input type="button" class="svgColorButton" style="background:red" onclick="setStroke('red');"></input><input type="button" class="svgColorButton" style="background:green" onclick="setStroke('green');"></input><input type="button" class="svgColorButton" style="background:blue" onclick="setStroke('blue');"></input><input type="button" class="svgColorButton" style="background:yellow" onclick="setStroke('yellow');"></input><input type="button" class="svgColorButton" style="background:skyblue" onclick="setStroke('skyblue');"></input><input type="button" class="svgColorButton" style="background:aqua" onclick="setStroke('aqua');"></input><input type="button" class="svgColorButton" style="background:white" onclick="setStroke('white');"></input><input type="button" class="svgColorButton" style="background:black" onclick="setStroke('black');"></input>
		</p>
	</div>
</xsl:template>

</xsl:stylesheet>

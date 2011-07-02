<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="xml" encoding="utf-8" omit-xml-declaration="yes" />

<xsl:template match="/mirc">
	<html>
		<head>
			<title>Query Service Admin</title>
			<link rel="Stylesheet" type="text/css" media="all" href="/JSPopup.css"></link>
			<link rel="Stylesheet" type="text/css" media="all" href="/qsadmin/QueryServiceAdmin.css"></link>
			<script language="JavaScript" type="text/javascript" src="/JSUtil.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/JSPopup.js">;</script>
			<script language="JavaScript" type="text/javascript" src="/qsadmin/QueryServiceAdmin.js">;</script>
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

			<h1>Query Service Admin</h1>
			<form id="formID" action="" method="POST" accept-charset="UTF-8">

			<p class="note">
				The table below controls the primary configuration parameters
				of the system. Changes in the items marked with an asterisk
				require the system to be restarted to become effective.
			</p>

			<center>
				<table border="1">
					<tr>
						<td>System mode:</td>
						<td>
							<input type="radio" name="mode" value="rad">
								<xsl:if test="@mode='rad' or @mode=''">
									<xsl:attribute name="checked"/>
								</xsl:if>
								Radiology
							</input>
							<br/>
							<input type="radio" name="mode" value="vet">
								<xsl:if test="@mode='vet'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								Veterinary Medicine
							</input>
						</td>
					</tr>
					<tr>
						<td>Site name:</td>
						<td><input class="text" type="text" name="sitename" value="{@sitename}"/></td>
					</tr>
					<tr>
						<td>Masthead file name:</td>
						<td><input class="text" type="text" name="masthead" value="{@masthead}"/></td>
					</tr>
					<tr>
						<td>Default query page user interface:</td>
						<td>
							<input type="radio" name="UI" value="classic">
								<xsl:if test="@UI='classic'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								Classic
							</input>
							<br/>
							<input type="radio" name="UI" value="integrated">
								<xsl:if test="not(@UI='classic')">
									<xsl:attribute name="checked"/>
								</xsl:if>
								Integrated
							</input>
						</td>
					</tr>
					<tr>
						<td>Show site name in masthead:</td>
						<td>
							<input type="radio" name="showsitename" value="yes">
								<xsl:if test="@showsitename='yes'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								yes
							</input>
							<br/>
							<input type="radio" name="showsitename" value="no">
								<xsl:if test="@showsitename='no'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								no
							</input>
						</td>
					</tr>
					<tr>
						<td>Show Patient ID fields:</td>
						<td>
							<input type="radio" name="showptids" value="yes">
								<xsl:if test="not(@showptids='no')">
									<xsl:attribute name="checked"/>
								</xsl:if>
								yes
							</input>
							<br/>
							<input type="radio" name="showptids" value="no">
								<xsl:if test="@showptids='no'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								no
							</input>
						</td>
					</tr>
					<tr>
						<td>Site URL:</td>
						<td><input class="text" type="text" name="siteurl" value="{@siteurl}"/></td>
					</tr>
					<tr>
						<td>Address type:</td>
						<td>
							<input type="radio" name="addresstype" value="dynamic">
								<xsl:if test="not(@addresstype='static')">
									<xsl:attribute name="checked"/>
								</xsl:if>
								dynamic
							</input>
							<br/>
							<input type="radio" name="addresstype" value="static">
								<xsl:if test="@addresstype='static'">
									<xsl:attribute name="checked"/>
								</xsl:if>
								static
							</input>
						</td>
					</tr>
					<tr>
						<td>Query timeout (in seconds, default=10) :</td>
						<td><input class="text" type="text" name="timeout" value="{@timeout}"/></td>
					</tr>
					<tr>
						<td>Disclaimer URL (blank to disable):</td>
						<td><input class="text" type="text" name="disclaimerurl" value="{@disclaimerurl}"/></td>
					</tr>
					<tr>
						<td>Site-defined roles (separated by commas):</td>
						<td><input class="text" type="text" name="roles" value="{@roles}"/></td>
					</tr>
					<tr>
						<td>Remove Case of the Day:</td>
						<td>
							<input type="checkbox" name="removecod" value="yes"></input>
						</td>
					</tr>
				</table>
			</center>

			<p class="note">
				The table below controls the list of libraries. To enable a library,
				check its checkbox in the list.
				To delete a library from the list, erase its name or its URL in the table.
				To add a new library, fill in its parameters in the blank fields in the last row.
				NOTE: You cannot create, delete. or change the URLs of local libraries; you can
				only change their names. Local libraries must be created or deleted on the Storage Service
				Admin page.
			</p>

			<center>
				<table border="1">
					<tr>
						<th class="checkbox">Enable</th>
						<th>Storage Service Name</th>
						<th>Storage Service URL</th>
					</tr>
					<xsl:for-each select="Libraries/Library">
						<xsl:variable name="n"><xsl:number/></xsl:variable>
						<tr>
							<td class="checkbox">
								<input type="checkbox" name="enb{$n}" value="yes">
									<xsl:if test="not(@enabled) or @enabled='yes' or @enabled=''">
										<xsl:attribute name="checked"/>
									</xsl:if>
								</input>
							</td>
							<td>
								<input class="svrtext" name="name{$n}" type="text" value="{normalize-space(title)}"/>
							</td>
							<td>
								<xsl:if test="@local='yes'">
									<xsl:value-of select="@address"/>
									<input name="adrs{$n}" type="hidden" value="{@address}"/>
								</xsl:if>
								<xsl:if test="not(@local='yes')">
									<input class="svrtext" name="adrs{$n}" type="text" value="{@address}"/>
									<input name="prevadrs{$n}" type="hidden" value="{@address}"/>
								</xsl:if>
							</td>
						</tr>
					</xsl:for-each>
					<tr>
						<td class="checkbox">
							<input type="checkbox" name="enb0" value="yes"/>
						</td>
						<td><input class="svrtext" type="text" name="name0" value=""/></td>
						<td><input class="svrtext" type="text" name="adrs0" value=""/></td>
					</tr>
				</table>
			</center>
			</form>

		</body>
	</html>
</xsl:template>

</xsl:stylesheet>
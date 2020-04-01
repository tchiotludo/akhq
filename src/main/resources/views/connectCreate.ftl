<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="pluginDefinition" type="java.util.Optional<org.akhq.models.ConnectPlugin>" -->
<#-- @ftlvariable name="definition" type="org.akhq.models.ConnectDefinition" -->

<#import "includes/template.ftl" as template>
<#import "includes/connect.ftl" as connectTemplate>

<@template.header (definition?has_content)?then("Update", "Create") + " a definition", "connect" />

<@connectTemplate.form pluginDefinition definition! />

<@template.footer/>

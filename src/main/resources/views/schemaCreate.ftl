<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="config" type="org.akhq.models.Schema.Config" -->
<#-- @ftlvariable name="schema" type="org.akhq.models.Schema" -->

<#import "includes/template.ftl" as template>
<#import "includes/schema.ftl" as schemaTemplate>

<@template.header (config?has_content)?then("Update", "Create") + " a schema", "schema" />

<@schemaTemplate.form config schema! />

<@template.footer/>

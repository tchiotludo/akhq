<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="config" type="org.kafkahq.models.Schema.Config" -->
<#-- @ftlvariable name="schema" type="org.kafkahq.models.Schema" -->

<#import "includes/template.ftl" as template>
<#import "includes/schema.ftl" as schemaTemplate>

<@template.header (config?has_content)?then("Update", "Create") + " a schema", "schema" />

<@schemaTemplate.form config schema! />

<@template.footer/>
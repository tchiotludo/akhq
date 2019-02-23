<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="config" type="org.kafkahq.models.Schema.Config" -->

<#import "includes/template.ftl" as template>
<#import "includes/schema.ftl" as schemaTemplate>

<@template.header "Create a schema", "schema" />

<@schemaTemplate.form config />

<@template.footer/>
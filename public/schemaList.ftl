<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="schemas" type="java.util.List<org.kafkahq.models.Schema>" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "/includes/template.ftl" as template>
<#import "/includes/schema.ftl" as schemaTemplate>

<@template.header "Schema Registry", "schema" />

<@schemaTemplate.table schemas false />

<@template.bottom>
    <a href="${basePath}/${clusterId}/schema/create" type="submit" class="btn btn-primary">Create a subject</a>
</@template.bottom>

<@template.footer/>
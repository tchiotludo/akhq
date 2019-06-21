<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="schemas" type="java.util.List<org.kafkahq.models.Schema>" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->

<#import "includes/template.ftl" as template>
<#import "includes/schema.ftl" as schemaTemplate>

<@template.header "Schema Registry", "schema" />

<#include "blocks/navbar-search.ftl" />

<@schemaTemplate.table schemas false />

<#if roles?seq_contains("registry/insert") == true>
    <@template.bottom>
        <a href="${basePath}/${clusterId}/schema/create" class="btn btn-primary">Create a subject</a>
    </@template.bottom>
</#if>

<@template.footer/>
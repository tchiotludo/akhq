<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="schema" type="org.akhq.models.Schema" -->
<#-- @ftlvariable name="config" type="org.akhq.models.Schema.Config" -->
<#-- @ftlvariable name="versions" type="java.util.List<org.akhq.models.Schema>" -->

<#import "includes/template.ftl" as template>
<#import "includes/schema.ftl" as schemaTemplate>

<@template.header "Schema: " + schema.getSubject(), "schema" />

<div class="tabs-container">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "update")?then("active", "")}"
               href="${basePath}/${clusterId}/schema/${schema.getSubject()}"
               role="tab">Update</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "version")?then("active", "")}"
               href="${basePath}/${clusterId}/schema/${schema.getSubject()}/version"
               role="tab">Versions <span class="badge badge-secondary">${versions?size}</span></a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "update">
            <div class="tab-pane active" role="tabpanel">
                <@schemaTemplate.form config schema />
            </div>
        </#if>

        <#if tab == "version">
            <div class="tab-pane active" role="tabpanel">
                <@schemaTemplate.table versions true />
            </div>
        </#if>
    </div>
</div>

<@template.footer/>

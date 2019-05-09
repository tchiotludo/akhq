<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="cluster" type="org.kafkahq.models.Cluster" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="message" type="java.lang.String" -->
<#-- @ftlvariable name="stacktrace" type="java.lang.String" -->

<#import "../includes/template.ftl" as template>

<@template.header "500 Internal server error" />

<div class="mb-5">
    <h3 class="logo"><img src="${basePath}/static/img/logo.svg" alt=""/><sup><strong>HQ</strong></sup></h3>
</div>

<code>${message!}</code><br /><br />

<pre><code>${stacktrace}</code></pre>

<@template.footer/>
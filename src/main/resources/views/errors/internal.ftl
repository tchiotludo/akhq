<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="cluster" type="org.kafkahq.models.Cluster" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="message" type="java.lang.String" -->
<#-- @ftlvariable name="stacktrace" type="java.lang.String" -->

<#import "../includes/template.ftl" as template>

<@template.header "500 Internal server error", "node" />

<code>${message}</code><br /><br />

<pre><code>${stacktrace}</code></pre>

<@template.footer/>
<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="cluster" type="org.kafkahq.models.Cluster" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "../includes/template.ftl" as template>

<@template.header "404 Not Found", "node" />

<p>
    The page you were looking for doesn't exist.
</p>

<p>
    You may have mistyped the address or the page doesn't exist.
</p>

<@template.footer/>
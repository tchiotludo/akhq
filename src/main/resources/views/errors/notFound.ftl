<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="cluster" type="org.kafkahq.models.Cluster" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "../includes/template.ftl" as template>

<@template.header "404 Not Found" />

<div class="jumbotron" style="max-width: 360px; margin: 0 auto">
    <div class="mb-5">
        <h3 class="logo"><img src="${basePath}/static/img/logo.svg" alt=""/><sup><strong>HQ</strong></sup></h3>
    </div>

    <p>
        The page you were looking for doesn't exist.
    </p>

    <p>
        You may have mistyped the address or the page doesn't exist.
    </p>

    <div class="text-right mt-5">
        <a href="/" class="btn btn-primary btn-lg">Back to home</a>
    </div>
</div>

<@template.footer/>
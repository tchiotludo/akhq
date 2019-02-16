<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="node" type="org.kafkahq.models.Node" -->
<#-- @ftlvariable name="configs" type="java.util.ArrayList<org.kafkahq.models.Config>" -->
<#-- @ftlvariable name="logs" type="java.util.ArrayList<org.kafkahq.models.LogDir>" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>
<#import "includes/log.ftl" as logTemplate>

<@template.header "Node: " + node.getId()?c, "node" />

<div class="tabs-container">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "configs")?then("active", "")}"
               href="${basePath}/${clusterId}/node/${node.getId()?c}"
               role="tab">Configs</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "logs")?then("active", "")}"
               href="${basePath}/${clusterId}/node/${node.getId()?c}/logs"
               role="tab">Logs</a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "configs">
        <div class="tab-pane active" role="tabpanel">
            <#include "blocks/configs.ftl" />
        </div>
        </#if>

        <#if tab == "logs">
        <div class="tab-pane active" role="tabpanel">
        <@logTemplate.table logs />
        </div>
        </#if>
    </div>
</div>


<@template.footer/>


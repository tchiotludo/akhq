<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="topic" type="org.kafkahq.models.Topic" -->
<#-- @ftlvariable name="configs" type="java.util.ArrayList<org.kafkahq.models.Config>" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->
<#-- @ftlvariable name="acls" type="java.util.ArrayList<org.kafkahq.models.AccessControlList>" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>
<#import "includes/log.ftl" as logTemplate>

<@template.header "Topic: " + topic.getName(), "topic" />

<div class="tabs-container">
    <ul class="nav nav-tabs" role="tablist">
        <#if roles?seq_contains("topic/data/read")>
            <li class="nav-item">
                <a class="nav-link ${(tab == "data")?then("active", "")}"
                   href="${basePath}/${clusterId}/topic/${topic.getName()}"
                   role="tab">Data</a>
            </li>
        </#if>
        <li class="nav-item">
            <a class="nav-link ${(tab == "partitions")?then("active", "")}"
               href="${basePath}/${clusterId}/topic/${topic.getName()}/partitions"
               role="tab">Partitions</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "groups")?then("active", "")}"
               href="${basePath}/${clusterId}/topic/${topic.getName()}/groups"
               role="tab">Consumer Groups</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "configs")?then("active", "")}"
               href="${basePath}/${clusterId}/topic/${topic.getName()}/configs"
               role="tab">Configs</a>
        </li>
        <#if roles?seq_contains("acls") == true>
            <li class="nav-item">
                <a class="nav-link ${(tab == "acls")?then("active", "")}"
                   href="${basePath}/${clusterId}/topic/${topic.getName()}/acls"
                   role="tab">ACLS</a>
            </li>
        </#if>
        <li class="nav-item">
            <a class="nav-link ${(tab == "logs")?then("active", "")}"
               href="${basePath}/${clusterId}/topic/${topic.getName()}/logs"
               role="tab">Logs</a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "data">
        <div class="tab-pane active" role="tabpanel">
            <#include "blocks/topic/data.ftl" />
        </div>
        </#if>

        <#if tab == "partitions">
        <div class="tab-pane active" role="tabpanel">
            <#include "blocks/topic/partitions.ftl" />
        </div>
        </#if>

        <#if tab == "groups">
        <div class="tab-pane active" role="tabpanel">
            <@groupTemplate.table topic.getConsumerGroups() />
        </div>
        </#if>

        <#if tab == "configs">
        <div class="tab-pane active" role="tabpanel">
            <#include "blocks/configs.ftl" />
        </div>
        </#if>

        <#if tab == "acls" && roles?seq_contains("acls") == true>
            <div class="tab-pane active" role="tabpanel">
                <#assign resourceType="topic"/>
                <#include "blocks/resourceTypeAcls.ftl" />
            </div>
        </#if>

        <#if tab == "logs">
        <div class="tab-pane active" role="tabpanel">
            <@logTemplate.table topic.getLogDir() />
        </div>
        </#if>
    </div>
</div>

<#if tab != "configs" && roles?seq_contains("topic/data/insert")>
    <@template.bottom>
        <a href="${basePath}/${clusterId}/tail/?topics=${topic.getName()}" class="btn btn-secondary mr-2">
            <i class="fa fa-fw fa-level-down" aria-hidden="true"></i> Live Tail
        </a>

        <a href="${basePath}/${clusterId}/topic/${topic.getName()}/produce" class="btn btn-primary">
            <i class="fa fa-plus" aria-hidden="true"></i> Produce to topic
        </a>
    </@template.bottom>
</#if>

<@template.footer/>


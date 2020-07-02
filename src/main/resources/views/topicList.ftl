<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="topics" type="java.util.ArrayList<org.akhq.models.Topic>" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->
<#-- @ftlvariable name="skipConsumerGroups" type="java.lang.Boolean" -->

<#assign canDelete=roles?seq_contains("topic/delete")>
    
<#import "includes/template.ftl" as template>
<#import "includes/functions.ftl" as functions>

<@template.header "Topics", "topic" />

<#include "blocks/navbar-search.ftl" />

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th colspan="3">Topics</th>
                <th colspan="1">Partitions</th>
                <th colspan="2">Replications</th>
                <#if skipConsumerGroups == false>
                    <th>Consumers Groups</th>
                </#if>
                <th colspan="3" class="khq-row-action"></th>
            </tr>
        </thead>
        <thead class="thead-dark">
            <tr>
                <th class="text-nowrap">Name</th>
                <th class="text-nowrap">Count</th>
                <th class="text-nowrap">Size</th>
                <th class="text-nowrap">Total</th>
                <!--
                <th class="text-nowrap">Available</th>
                <th class="text-nowrap">Under replicated</th>
                -->
                <th class="text-nowrap">Factor</th>
                <th class="text-nowrap">In Sync</th>
                <#if skipConsumerGroups == false>
                    <th class="text-nowrap">Consumer Groups</th>
                </#if>
                <th class="khq-row-action"></th>
                <th class="khq-row-action"></th>
                <#if canDelete == true>
                    <th class="khq-row-action"></th>
                </#if>
            </tr>
        </thead>
        <tbody>
                <#if topics?size == 0>
                    <tr>
                        <td colspan="9">
                            <div class="alert alert-info mb-0" role="alert">
                                No topic available
                            </div>
                        </td>
                    </tr>
                </#if>
                <#list topics as topic>
                    <tr>
                        <td>${topic.getName()}</td>
                        <td>
                            <span class="text-nowrap">
                                ≈ ${topic.getSize()}
                            </span>
                        </td>
                        <td>
                            <#if topic.getLogDirSize().isEmpty() >
                                n/a
                            <#else>
                                ${functions.filesize(topic.getLogDirSize().get())}
                            </#if>
                        </td>
                        <td>${topic.getPartitions()?size}</td>
                        <td>${topic.getReplicaCount()}</td>
                        <td><span class="${(topic.getReplicaCount() > topic.getInSyncReplicaCount())?then("text-warning", "")}">${topic.getInSyncReplicaCount()}</span></td>
                        <#if skipConsumerGroups == false>
                            <td>
                                <#list topic.getConsumerGroups() as group>
                                    <#assign active = group.isActiveTopic(topic.getName()) >
                                    <a href="${basePath}/${clusterId}/group/${group.getId()}" class="btn btn-sm mb-1 btn-${active?then("success", "warning")} ">
                                        ${group.getId()}
                                        <span class="badge badge-light">
                                            Lag: ${group.getOffsetLag(topic.getName())}
                                        </span>
                                    </a><br/>
                                </#list>
                            </td>
                        </#if>
                        <td class="khq-row-action khq-row-action-main">
                            <a href="${basePath}/${clusterId}/topic/${topic.getName()}${roles?seq_contains("topic/data/read")?then("", "/partitions")}" ><i class="fa fa-search"></i></a>
                        </td>
                        <td class="khq-row-action">
                            <a href="${basePath}/${clusterId}/topic/${topic.getName()}/configs" ><i class="fa fa-gear"></i></a>
                        </td>
                        <#if canDelete == true>
                            <td class="khq-row-action">
                                <#if topic.isInternalTopic() == false>
                                    <a
                                        href="${basePath}/${clusterId}/topic/${topic.getName()}/delete"
                                        data-confirm="Do you want to delete topic: <code>${topic.getName()}</code> ?"
                                    ><i class="fa fa-trash"></i></a>
                                </#if>
                            </td>
                        </#if>
                    </tr>
                </#list>
        </tbody>
    </table>
</div>

<#include "blocks/navbar-pagination.ftl" />

<#if roles?seq_contains("topic/insert") == true>
    <@template.bottom>
        <a href="${basePath}/${clusterId}/topic/create" class="btn btn-primary">Create a topic</a>
    </@template.bottom>
</#if>

<@template.footer/>

<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->

<#import "node.ftl" as nodeTemplate>
<#assign canDelete=roles?seq_contains("group/delete")>

<#macro table groups>
    <#-- @ftlvariable name="groups" type="java.util.List<org.akhq.models.ConsumerGroup>" -->
    <div class="table-responsive">
        <table class="table table-bordered table-striped table-hover mb-0">
            <thead class="thead-dark">
                <tr>
                    <th>Id</th>
                    <th>State</th>
                    <th>Coordinator</th>
                    <th>Members</th>
                    <th>Topics</th>
                    <th class="khq-row-action"></th>
                    <#if canDelete == true>
                        <th class="khq-row-action"></th>
                    </#if>
                </tr>
            </thead>
            <tbody>
                    <#if groups?size == 0>
                        <tr>
                            <td colspan="${(canDelete == true)?then("7", "6")}">
                                <div class="alert alert-info mb-0" role="alert">
                                    No consumer group available
                                </div>
                            </td>
                        </tr>
                    </#if>
                    <#list groups as group>
                        <tr>
                            <td>${group.getId()}</td>
                            <td><@state group.getState() /></td>
                            <td><@nodeTemplate.badge group.getCoordinator()/></td>
                            <td>${group.getMembers()?size}</td>
                            <td>
                                <#list group.getTopics() as topic>
                                    <a href="${basePath}/${clusterId}/topic/${topic}" class="btn btn-dark btn-sm mb-1">
                                        ${topic}
                                        <span class="badge badge-light">Lag: ${group.getOffsetLag(topic)}</span>
                                    </a>
                                </#list>
                            </td>
                            <td class="khq-row-action khq-row-action-main">
                                <a href="${basePath}/${clusterId}/group/${group.getId()}" ><i class="fa fa-search"></i></a>
                            </td>
                            <#if canDelete == true>
                                <td class="khq-row-action">
                                    <a
                                        href="${basePath}/${clusterId}/group/${group.getId()}/delete"
                                        data-confirm="Do you want to delete consumer group: <code>${group.getId()}</code> ?"
                                    ><i class="fa fa-trash"></i></a>
                                </td>
                            </#if>
                        </tr>
                    </#list>
            </tbody>
        </table>
    </div>
</#macro>

<#macro state state>
    <#-- @ftlvariable name="state" type="org.apache.kafka.common.ConsumerGroupState" -->
    <#if state.toString() == "Stable">
        <#assign class="success">
    <#elseif state.toString() == "Dead">
        <#assign class="danger">
    <#elseif state.toString() == "Empty">
        <#assign class="warning">
    <#else>
        <#assign class="info">
    </#if>
    <span class="badge badge-${class}">${state.toString()}</span>
</#macro>

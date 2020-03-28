<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="acl" type="org.akhq.models.AccessControl" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>
<#import "includes/log.ftl" as logTemplate>

<@template.header "Principal : " + acl.getPrincipal(), "acls" />

<div class="tabs-container">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "topic")?then("active", "")}"
               href="${basePath}/${clusterId}/acls/${acl.getEncodedPrincipal()}/"
               role="tab">Topics</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "group")?then("active", "")}"
               href="${basePath}/${clusterId}/acls/${acl.getEncodedPrincipal()}/group"
               role="tab">Groups</a>
        </li>
    </ul>
</div>

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
        <tr>
            <th>${tab?cap_first}</th>
            <th>Host</th>
            <th>Permissions</th>
        </tr>
        </thead>
        <tbody>
        <#assign aclCounter=0>
        <#if acl.getPermissions()[tab]?? >
            <#assign topicAcls=acl.getPermissions()[tab]>


            <#assign key_list = topicAcls?keys/>
            <#assign value_list = topicAcls?values/>
            <#list key_list as key>
                <#assign aclCounter++>
                <#assign seq_index = key_list?seq_index_of(key) />
                <#assign key_value = value_list[seq_index]/>
                <tr>
                    <td>${key.getResource()}</td>
                    <td>${key.getHost()}</td>
                    <td>
                        <h5>
                            <#list key_value as acl >
                                <span class="badge badge-secondary">${acl}</span>
                            </#list>
                        </h5>
                    </td>
                </tr>
            </#list>
        </#if>
        <#if aclCounter == 0 >
            <tr>
                <td colspan="3">
                    <div class="alert alert-warning mb-0" role="alert">
                        No ACL found, or the "authorizer.class.name" parameter is not configured on the cluster.
                    </div>
                </td>
            </tr>
        </#if>
        </tbody>
    </table>
</div>

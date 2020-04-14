<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="acl" type="org.akhq.models.AccessControl" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>
<#import "includes/log.ftl" as logTemplate>

<@template.header "Acl: " + acl.getPrincipal(), "acls" />

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
            <th>Permission</th>
        </tr>
        </thead>
        <tbody>
        <#assign aclCounter=0>
        <#if acl.findByRessourceType(tab)?? >
            <#assign currents=acl.findByRessourceType(tab)>

            <#list currents as current>
                <#assign aclCounter++>

                <tr>
                    <td>${current.getResource().getName()}</td>
                    <td>${current.getHost()}</td>
                    <td>
                        <span class="badge badge-secondary">${current.getOperation().getPermissionType()}</span>
                        ${current.getOperation().getOperation()}
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

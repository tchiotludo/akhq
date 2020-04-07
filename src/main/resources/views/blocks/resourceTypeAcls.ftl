<#ftl output_format="HTML">

<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="resourceType" type="java.lang.String" -->
<#-- @ftlvariable name="acls" type="java.util.List<org.akhq.models.AccessControl>" -->


<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
        <tr>
            <th>${resourceType?cap_first}</th>
            <th>Host</th>
            <th>Permissions</th>
        </tr>
        </thead>
        <tbody>
        <#assign aclCounter=0>
        <#list acls as acl>
            <#assign topicAcls=acl.findByRessourceType(resourceType?lower_case)>
            <#list topicAcls as current>
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
        </#list>

        <#if aclCounter == 0 >
            <tr>
                <td colspan="3">
                    <div class="alert alert-warning mb-0" role="alert">
                        No ACLS found, or the "authorizer.class.name" parameter is not configured on the cluster.
                    </div>
                </td>
            </tr>
        </#if>
        </tbody>
    </table>
</div>

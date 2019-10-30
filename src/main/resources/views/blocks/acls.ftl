<#ftl output_format="HTML">

<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="resourceType" type="java.lang.String" -->
<#-- @ftlvariable name="users" type="java.util.List<Users>" -->


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
        <#list users as user>
            <#assign topicAcls=user.getAcls()[resourceType?lower_case]>
            <#assign key_list = topicAcls?keys/>
            <#assign value_list = topicAcls?values/>
            <#list key_list as key>
                <#assign aclCounter++>
                <#assign seq_index = key_list?seq_index_of(key) />
                <#assign key_value = value_list[seq_index]/>
                <tr>
                    <td>${user.getName()}</td>
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
        </#list>

        <#if aclCounter == 0 >
            <tr>
                <td colspan="3">
                    No ACLS found, or the "authorizer.class.name" parameter is not configured on the cluster.
                </td>
            </tr>
        </#if>
        </tbody>
    </table>
</div>

<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="group" type="org.akhq.models.ConsumerGroup" -->

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th>ClientId</th>
                <th>Id</th>
                <th>Host</th>
                <th>Assigments</th>
            </tr>
        </thead>
        <tbody>
        <#if group.getMembers()?size == 0>
            <tr>
                <td colspan="5">
                    <div class="alert alert-info mb-0" role="alert">
                        No members available
                    </div>
                </td>
            </tr>
        </#if>
        <#list group.getMembers() as member>
            <tr>
                <td>${member.getClientId()}</td>
                <td>${member.getId()}</td>
                <td>${member.getHost()}</td>
                <td>
                    <#if member.getAssignments()?size == 0>
                        <div class="alert alert-info mb-0" role="alert">
                            No assigments available
                        </div>
                    </#if>
                    <#list member.getGroupedAssignments() as assignment>
                        <span class="badge badge-info">${assignment.getTopic()}
                            <#list assignment.getPartitions() as partition>
                                <span class="badge badge-dark">${partition}</span>
                            </#list>
                        </span>
                    </#list>
                </td>
            </tr>
        </#list>
        </tbody>
    </table>
</div>

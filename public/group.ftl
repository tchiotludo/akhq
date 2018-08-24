<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="group" type="org.kafkahq.models.ConsumerGroup" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->

<#import "/includes/template.ftl" as template>

<@template.header "Consumer Group: " + group.getId(), "group" />

<div class="tabs-container invisible">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "topics")?then("active", "")}"
               href="/${clusterId}/group/${group.getId()}/topics"
               role="tab">Topics</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "members")?then("active", "")}"
               href="/${clusterId}/group/${group.getId()}/members"
               role="tab">Members</a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "topics">
        <div class="tab-pane show active" role="tabpanel">
            <div class="table-responsive">
                <table class="table table-bordered table-striped table-hover mb-0">
                    <thead class="thead-dark">
                        <tr>
                            <th>Name</th>
                            <th>Partition</th>
                            <th>Metadata</th>
                            <th>Offset</th>
                            <th>Lag</th>
                        </tr>
                    </thead>
                    <tbody>
                    <#if group.getOffsets()?size == 0>
                        <tr>
                            <td colspan="5">
                                <div class="alert alert-info mb-0" role="alert">
                                    No topic available
                                </div>
                            </td>
                        </tr>
                    </#if>
                    <#list group.getOffsets() as offset>
                        <tr>
                            <td><a href="/${clusterId}/topic/${offset.getTopic()}">${offset.getTopic()}</a></td>
                            <td>${offset.getPartition()}</td>
                            <td>
                                <#if offset.getMetadata().isPresent() && offset.getMetadata().get() != "">
                                    ${offset.getMetadata().get()}
                                <#else>
                                    -
                                </#if>
                            </td>
                            <td>
                                <#if offset.getOffset().isPresent()>
                                    ${offset.getOffset().get()}
                                <#else>
                                    -
                                </#if>
                            </td>
                            <td>
                                <#if offset.getOffsetLag().isPresent()>
                                    ${offset.getOffsetLag().get()}
                                <#else>
                                    -
                                </#if>
                            </td>
                        </tr>
                    </#list>
                    </tbody>
                </table>
            </div>
        </div>
        </#if>

        <#if tab == "members">
        <div class="tab-pane active" role="tabpanel">
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
        </div>
        </#if>
    </div>
</div>


<@template.footer/>


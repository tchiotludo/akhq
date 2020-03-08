<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="group" type="org.akhq.models.ConsumerGroup" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th>Name</th>
                <th>Partition</th>
                <th>Member</th>
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
                <td><a href="${basePath}/${clusterId}/topic/${offset.getTopic()}">${offset.getTopic()}</a></td>
                <td>${offset.getPartition()}</td>
                <td>
                    <#if offset.getMember().isPresent()>
                        ${offset.getMember().get().getHost()}
                        <a class="text-secondary" data-toggle="tooltip" title="${offset.getMember().get().getId()?replace('<[^>]+>','','r')}">
                            <i class="fa fa-question-circle" aria-hidden="true"></i>
                        </a>
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

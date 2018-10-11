<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="group" type="org.kafkahq.models.ConsumerGroup" -->

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
<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "node.ftl" as nodeTemplate>
<#import "functions.ftl" as functions>

<#macro table logs>
    <#-- @ftlvariable name="logs" type="java.util.List<org.kafkahq.models.LogDir>" -->
    <div class="table-responsive">
        <table class="table table-bordered table-striped table-hover mb-0">
            <thead class="thead-dark">
                <tr>
                    <th>Broker</th>
                    <th>Topic</th>
                    <th>Partition</th>
                    <th>Size</th>
                    <th>OffsetLag</th>
                </tr>
            </thead>
            <tbody>
                    <#if logs?size == 0>
                        <tr>
                            <td colspan="6">
                                <div class="alert alert-warning mb-0" role="alert">
                                    Missing ACL to list logs
                                </div>
                            </td>
                        </tr>
                    </#if>
                    <#list logs as log>
                        <tr>
                            <td>${log.getBrokerId()}</td>
                            <td>${log.getTopic()}</td>
                            <td>${log.getPartition()}</td>
                            <td>${functions.filesize(log.getSize())}</td>
                            <td>${log.getOffsetLag()}</td>
                        </tr>
                    </#list>
            </tbody>
        </table>
    </div>
</#macro>
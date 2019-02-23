<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="cluster" type="org.kafkahq.models.Cluster" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "includes/template.ftl" as template>
<#import "includes/node.ftl" as nodeTemplate>

<@template.header "Nodes", "node" />

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th class="text-nowrap">Id</th>
                <th class="text-nowrap">Host</th>
                <th class="text-nowrap">Rack</th>
                <th colspan="1" class="khq-row-action"></th>
            </tr>
        </thead>
        <tbody>
                <#if cluster.getNodes()?size == 0>
                    <tr>
                        <td colspan="5">
                            <div class="alert alert-info mb-0" role="alert">
                                No topic available
                            </div>
                        </td>
                    </tr>
                </#if>
                <#list cluster.getNodes() as node>
                    <tr>
                        <td><@nodeTemplate.badge node/></td>
                        <td>${node.getHost()}:${node.getPort()?c}</td>
                        <td>${node.getRack()!}</td>
                        <td class="khq-row-action khq-row-action-main">
                            <a href="${basePath}/${clusterId}/node/${node.getId()?c}" ><i class="fa fa-search"></i></a>
                        </td>
                    </tr>
                </#list>
        </tbody>
    </table>
</div>

<@template.footer/>
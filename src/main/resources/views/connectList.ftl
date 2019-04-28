<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->
<#-- @ftlvariable name="connects" type="java.util.List<org.kafkahq.models.ConnectDefinition>" -->

<#import "includes/template.ftl" as template>

<@template.header "Connect", "connect" />

<#assign canDelete=roles?seq_contains("connect/delete")>

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th class="text-nowrap">Name</th>
                <th class="text-nowrap">Type</th>
                <th class="text-nowrap">Tasks</th>
                <th class="khq-row-action"></th>
                <#if canDelete == true>
                    <th class="khq-row-action"></th>
                </#if>
            </tr>
        </thead>
        <tbody>
            <#if connects?size == 0>
                <tr>
                    <td colspan="${(canDelete == true)?then("5", "4")}">
                        <div class="alert alert-info mb-0" role="alert">
                            No connectors available
                        </div>
                    </td>
                </tr>
            </#if>
            <#list connects as connect>
                <tr>
                    <td>${connect.getName()}</td>
                    <td>
                        <#if connect.getType() == "source">
                            <i class="fa fa-forward" aria-hidden="true"></i>
                        <#else>
                            <i class="fa fa-backward" aria-hidden="true"></i>
                        </#if>
                        ${connect.getShortClassName()}
                    </td>
                    <td>
                        <#list connect.getTasks() as task>
                            <#assign class = (task.getState() == "RUNNING")?then("success", (task.getState() == "FAILED")?then("danger", "warning"))>
                            <span class="btn btn-sm mb-1 btn-${class} ">
                                ${task.getWorkerId()} (${task.getId()})
                                <span class="badge badge-light">
                                    ${task.getState()}
                                </span>
                            </span><br/>
                        </#list>
                    </td>
                    <td class="khq-row-action khq-row-action-main">
                        <a href="${basePath}/${clusterId}/connect/${connect.getName()}" ><i class="fa fa-search"></i></a>
                    </td>
                    <#if canDelete == true>
                        <td class="khq-row-action">
                            <a
                                    href="${basePath}/${clusterId}/connect/${connect.getName()}/delete"
                                    data-confirm="Do you want to delete definition: <code>${connect.getName()}</code> ?"
                            ><i class="fa fa-trash"></i></a>
                        </td>
                    </#if>
                </tr>

                <tr>
                    <td colspan="5">
                        <button type="button" class="close d-none" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                        <pre class="mb-0 khq-data-highlight"><code>${connect.getConfigsAsJson()}</code></pre>
                    </td>
                </tr>
            </#list>
        </tbody>
    </table>
</div>

<#if roles?seq_contains("connect/insert") == true>
    <@template.bottom>
        <a href="${basePath}/${clusterId}/connect/create" type="submit" class="btn btn-primary">Create a defintion</a>
    </@template.bottom>
</#if>

<@template.footer/>
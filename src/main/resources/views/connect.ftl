<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="connectId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->
<#-- @ftlvariable name="pluginDefinition" type="java.util.Optional<org.akhq.models.ConnectPlugin>" -->
<#-- @ftlvariable name="definition" type="org.akhq.models.ConnectDefinition" -->

<#import "includes/template.ftl" as template>
<#import "includes/connect.ftl" as connectTemplate>

<@template.header "Connect: " + definition.getName(), "connect" />

<#assign canStateChange=roles?seq_contains("connect/state/update")>

<div class="tabs-container">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "tasks")?then("active", "")}"
               href="${basePath}/${clusterId}/connect/${connectId}/${definition.getName()}"
               role="tab">Tasks</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "configs")?then("active", "")}"
               href="${basePath}/${clusterId}/connect/${connectId}/${definition.getName()}/configs"
               role="tab">Configs</span></a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "tasks">
            <div class="tab-pane active" role="tabpanel">
                <div class="table-responsive">
                    <table class="table table-bordered table-striped table-hover mb-0">
                        <thead class="thead-dark">
                            <tr>
                                <th class="text-nowrap">Id</th>
                                <th class="text-nowrap">Worker</th>
                                <th class="text-nowrap">State</th>
                                <#if canStateChange == true>
                                    <th colspan="1" class="khq-row-action"></th>
                                </#if>
                            </tr>
                        </thead>
                        <tbody>
                            <#if definition.getTasks()?size == 0>
                                <tr>
                                    <td colspan="5">
                                        <div class="alert alert-info mb-0" role="alert">
                                            No task available
                                        </div>
                                    </td>
                                </tr>
                            </#if>
                            <#list definition.getTasks() as task>
                                <#assign class = (task.getState() == "RUNNING")?then("success", (task.getState() == "FAILED")?then("danger", "warning"))>
                                <tr>
                                    <td>${task.getId()}</td>
                                    <td>${task.getWorkerId()}</td>
                                    <td>
                                        <span class="btn btn-sm mb-1 btn-${class}" title="${task.getState()}">
                                            ${task.getState()}
                                        </span>
                                    </td>
                                    <#if canStateChange == true>
                                        <td class="khq-row-action">
                                            <a
                                                    href="${basePath}/${clusterId}/connect/${connectId}/${definition.getName()}/tasks/${task.getId()}/restart" title="Restart"
                                                    data-confirm="Do you want to restart task: <code>${task.getId()} from ${task.getConnector()} </code> ?"
                                            >
                                                <i class="fa fa-play" aria-hidden="true"></i>
                                            </a>
                                        </td>
                                    </#if>
                                </tr>
                                <#if task.getTrace()??>
                                  <tr>
                                    <td colspan="5">
                                      <button type="button" class="close d-none" aria-label="Close">
                                        <span aria-hidden="true">&times;</span>
                                      </button>
                                      <pre class="mb-0 khq-data-highlight"><code>${task.getTrace()}</code></pre>
                                    </td>
                                  </tr>
                                </#if>
                            </#list>
                        </tbody>
                    </table>
                </div>
            </div>

            <#if canStateChange == true>
                <@template.bottom>
                    <#if definition.isPaused()>
                        <a
                                href="${basePath}/${clusterId}/connect/${connectId}/${definition.getName()}/resume" class="btn btn-primary mr-2"
                                data-confirm="Do you want to resume definition: <code>${definition.getName()} </code> ?"
                        >
                            <i class="fa fa-forward" aria-hidden="true"></i> Resume Definition
                        </a>
                    <#else>
                        <a
                                href="${basePath}/${clusterId}/connect/${connectId}/${definition.getName()}/pause" type="pause" class="btn btn-primary mr-2"
                                data-confirm="Do you want to pause definition: <code>${definition.getName()} </code> ?"
                        >
                            <i class="fa fa-pause" aria-hidden="true"></i> Pause Definition
                        </a>

                        <a
                                href="${basePath}/${clusterId}/connect/${connectId}/${definition.getName()}/restart" class="btn btn-primary mr-2"
                                data-confirm="Do you want to restart definition: <code>${definition.getName()} </code> ?"
                        >
                            <i class="fa fa-play" aria-hidden="true"></i> Restart Definition
                        </a>
                    </#if>
                </@template.bottom>
            </#if>

        </#if>

        <#if tab == "configs">
            <@connectTemplate.form pluginDefinition definition />
        </#if>
    </div>
</div>

<@template.footer/>

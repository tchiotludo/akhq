<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="topic" type="org.kafkahq.models.Topic" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="datas" type="java.util.List<org.apache.kafka.clients.consumer.ConsumerRecord<java.lang.String, java.lang.String>>" -->

<#import "/includes/template.ftl" as template>
<#import "/includes/node.ftl" as nodeTemplate>
<#import "/includes/group.ftl" as groupTemplate>
<#import "/includes/functions.ftl" as functions>
<#import "/includes/topic.ftl" as topicTemplate>

<@template.header "Topic: " + topic.getName(), "topic" />

<div class="tabs-container invisible">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "data")?then("active", "")}"
               href="/${clusterId}/topic/${topic.getName()}"
               role="tab">Data</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "partitions")?then("active", "")}"
               href="/${clusterId}/topic/${topic.getName()}/partitions"
               role="tab">Partitions</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "groups")?then("active", "")}"
               href="/${clusterId}/topic/${topic.getName()}/groups"
               role="tab">Consumer Groups</a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "data">
        <div class="tab-pane active" role="tabpanel">
            <@topicTemplate.data datas />
        </div>
        </#if>

        <#if tab == "partitions">
        <div class="tab-pane active" role="tabpanel">
            <table class="table table-bordered table-striped table-hover mb-0">
                <thead class="thead-dark">
                    <tr>
                        <th>Id</th>
                        <th>Leader</th>
                        <th>Replicas</th>
                        <th>Offsets</th>
                        <th>Size</th>
                        <th class="row-action"></th>
                    </tr>
                </thead>
                <tbody>
                <#list topic.getPartitions() as partition>
                    <tr>
                        <td>${partition.getId()}</td>
                        <td><@nodeTemplate.badge partition.getLeader()/></td>
                        <td>
                            <#list partition.getNodes() as replica>
                                <@nodeTemplate.badge replica/>
                            </#list>
                        </td>
                        <td>
                            ${partition.getFirstOffset()}
                            ⤑
                            ${partition.getLastOffset()}
                        </td>
                        <td>
                            ${partition.getLastOffset()-partition.getFirstOffset()}
                            -
                            ${functions.filesize(partition.getLogDir().getSize())}
                        </td>
                        <td class="row-action main-row-action">
                            <a href="/${clusterId}/topic/${topic.getName()}/partitions/${partition.getId()}" ><i class="fa fa-search"></i></a>
                        </td>
                    </tr>
                </#list>
                </tbody>
            </table>
        </div>
        </#if>

        <#if tab == "groups">
        <div class="tab-pane active" role="tabpanel">
            <@groupTemplate.table topic.getConsumerGroups() />
        </div>
        </#if>
    </div>
</div>


<@template.footer/>


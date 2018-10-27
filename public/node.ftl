<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="node" type="org.kafkahq.models.Node" -->
<#-- @ftlvariable name="configs" type="java.util.ArrayList<org.kafkahq.models.Config>" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->

<#import "/includes/template.ftl" as template>
<#import "/includes/group.ftl" as groupTemplate>

<@template.header "Node: " + node.getId()?c, "node" />

<div class="tabs-container invisible">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "configs")?then("active", "")}"
               href="/${clusterId}/node/${node.getId()?c}/configs"
               role="tab">Configs</a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "configs">
        <div class="tab-pane active" role="tabpanel">
            <#include "/blocks/configs.ftl" />
        </div>
        </#if>
    </div>
</div>


<@template.footer/>


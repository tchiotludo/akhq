<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="group" type="org.kafkahq.models.ConsumerGroup" -->
<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#import "includes/template.ftl" as template>

<@template.header "Consumer Group: " + group.getId(), "group" />

<div class="tabs-container">
    <ul class="nav nav-tabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${(tab == "topics")?then("active", "")}"
               href="${basePath}/${clusterId}/group/${group.getId()}/topics"
               role="tab">Topics</a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${(tab == "members")?then("active", "")}"
               href="${basePath}/${clusterId}/group/${group.getId()}/members"
               role="tab">Members</a>
        </li>
    </ul>

    <div class="tab-content">
        <#if tab == "topics">
        <div class="tab-pane show active" role="tabpanel">
            <#include "blocks/group/topic.ftl" />
        </div>
        </#if>

        <#if tab == "members">
        <div class="tab-pane active" role="tabpanel">
            <#include "blocks/group/members.ftl" />
        </div>
        </#if>
    </div>
</div>

<@template.bottom>
    <a href="${basePath}/${clusterId}/group/${group.getId()}/offsets" type="submit" class="btn btn-primary">Update offsets</a>
</@template.bottom>

<@template.footer/>


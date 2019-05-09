<#ftl output_format="HTML">

<#-- @ftlvariable name="groups" type="java.util.ArrayList<org.kafkahq.models.ConsumerGroup>" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>

<@template.header "Consumer Groups", "group" />

<#include "blocks/navbar-search.ftl" />

<@groupTemplate.table groups />

<@template.footer/>


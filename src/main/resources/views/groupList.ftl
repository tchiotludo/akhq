<#ftl output_format="HTML" encoding="UTF-8">

<#-- @ftlvariable name="groups" type="java.util.ArrayList<org.akhq.models.ConsumerGroup>" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>

<@template.header "Consumer Groups", "group" />

<#include "blocks/navbar-search.ftl" />

<@groupTemplate.table groups />

<#include "blocks/navbar-pagination.ftl" />

<@template.footer/>


<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="users" type="java.util.List<org.kafkahq.models.User>" -->
<#-- @ftlvariable name="search" type="java.util.Optional<java.lang.String>" -->

<#import "includes/template.ftl" as template>
<#import "includes/group.ftl" as groupTemplate>
<#import "includes/log.ftl" as logTemplate>

<@template.header "Users", "user" />

<nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-nav">
    <button class="navbar-toggler"
            type="button"
            data-toggle="collapse"
            data-target="#navbar-search"
            aria-controls="navbar-search"
            aria-expanded="false"
            aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <div class="collapse navbar-collapse" id="navbar-search">
        <form class="form-inline mr-auto khq-form-get" method="get">
            <input class="form-control"
                   name="search"
                   placeholder="Search"
                   autocomplete="off"
                   type="text"
                    <#if search.isPresent()>
                        value="${search.get()}"
                    </#if> />
            <button class="btn btn-primary" type="submit">
                <span class="d-md-none">Search </span><i class="fa fa-search"></i>
            </button>
        </form>
    </div>
</nav>

<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
        <tr>
            <th colspan="2">User</th>
        </tr>
        </thead>
        <tbody>
        <#list users as user>
            <tr>
                <td>${user.getName()}</td>
                <td class="khq-row-action khq-row-action-main">
                    <a href="${basePath}/${clusterId}/user/${user.getEncodedName()}" ><i class="fa fa-search"></i></a>
                </td>
            </tr>
        </#list>
        <#if users?size == 0 >
            <tr>
                <td colspan="2">
                    No user found, or the "authorizer.class.name" parameter is not configured on the cluster.
                </td>
            </tr>
        </#if>
        </tbody>
    </table>
</div>

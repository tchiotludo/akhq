<#ftl output_format="HTML">

<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="clusters" type="java.util.List<java.lang.String>" -->
<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="toast" type="java.lang.String" -->
<#-- @ftlvariable name="registryEnabled" type="java.lang.Boolean" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->
<#-- @ftlvariable name="username" type="java.lang.String" -->
<#-- @ftlvariable name="loginEnabled" type="java.lang.Boolean" -->

<#macro header title tab="">
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8" />
        <title>${title} | KafkaHQ</title>
        <meta name="turbolinks-cache-control" content="no-cache" />
        <link rel="shortcut icon"
              type="image/png"
              href="${basePath}/static/img/icon.png" />
        <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Open+Sans+Condensed:300,700|Open+Sans:400,700" />
        <link rel="stylesheet" href="${basePath}/static/css/vendor.css" />
        <link rel="stylesheet" href="${basePath}/static/css/main.css" />
        <script type="text/javascript" src="${basePath}/static/js/vendor.js"></script>
        <script type="text/javascript" src="${basePath}/static/js/main.js"></script>
    <#nested>
    </head>
    <body>
        <#if toast??>
            <div class="khq-toast"></div>
            <script type="application/json">${toast?no_esc}</script>
        </#if>
        <div class="wrapper">
            <#if tab != "">
            <nav id="khq-sidebar">
                <div class="sidebar-header">
                    <a href="${basePath}/">
                        <h3 class="logo"><img src="${basePath}/static/img/logo.svg" alt=""/><sup><strong>HQ</strong></sup></h3>
                    </a>
                </div>

                <#if clusterId??>
                <ul class="list-unstyled components">
                    <li class="${(tab == "cluster")?then("active", "")}">
                        <a href="#clusters"
                            data-toggle="collapse"
                            aria-expanded="false"
                            class="dropdown-toggle"><i
                            class="fa fa-fw fa fa-database"
                            aria-hidden="true"></i> Clusters <span class="badge badge-success">${clusterId}</span></a>
                        <ul class="collapse list-unstyled" id="clusters">
                            <#list clusters as cluster>
                            <li>
                                <a href="${basePath}/${cluster}" class="${(cluster == clusterId)?then("active", "")}">${cluster}</a>
                            </li>
                            </#list>
                        </ul>
                    </li>
                    <#if roles?seq_contains("node") == true>
                        <li class="${(tab == "node")?then("active", "")}">
                            <a href="${basePath}/${clusterId}/node"><i class="fa fa-fw fa-laptop" aria-hidden="true"></i> Nodes</a>
                        </li>
                    </#if>
                    <#if roles?seq_contains("group") == true>
                        <li class="${(tab == "topic")?then("active", "")}">
                            <a href="${basePath}/${clusterId}/topic"><i class="fa fa-fw fa-list" aria-hidden="true"></i> Topics</a>
                        </li>
                    </#if>
                    <#if roles?seq_contains("group") == true>
                        <li class="${(tab == "group")?then("active", "")}">
                            <a href="${basePath}/${clusterId}/group"><i class="fa fa-fw fa-object-group" aria-hidden="true"></i> Consumer Groups</a>
                        </li>
                    </#if>
                    <#if registryEnabled?? && registryEnabled == true && roles?seq_contains("registry") == true>
                        <li class="${(tab == "schema")?then("active", "")}">
                            <a href="${basePath}/${clusterId}/schema"><i class="fa fa-fw fa-cogs" aria-hidden="true"></i> Schema Registry</a>
                        </li>
                    </#if>
                </ul>
                </#if>
                <#if loginEnabled>
                    <div class="sidebar-log">
                        <#if username??>
                            <a href="${basePath}/logout" data-turbolinks="false">
                                <i class="fa fa-fw fa-sign-out" aria-hidden="true"></i>
                                ${username} (Logout)
                            </a>
                        <#else>
                            <a href="${basePath}/login">
                                <i class="fa fa-fw fa-sign-in" aria-hidden="true"></i>
                                Login
                            </a>
                        </#if>
                        </a>
                    </div>
                </#if>
            </nav>
            </#if>
            <div id="content" class="${(tab == "")?then("no-side-bar", "")}">
                <#if tab != "">
                    <div class="title">
                        <h1>
                            <button type="button" id="khq-sidebar-collapse" class="btn btn-dark d-md-none">
                                <i class="fa fa-bars"></i>
                            </button>

                            ${title}
                        </h1>
                    </div>
                </#if>

                <main>
</#macro>
<#macro bottom>
                    <aside>
                        <#nested>
                    </aside>
</#macro>
<#macro footer>
                </main>
            </div>
        </div>

        <#nested>
        </body>
    </html>
</#macro>

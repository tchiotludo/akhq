<#-- @ftlvariable name="tab" type="java.lang.String" -->
<#-- @ftlvariable name="clusters" type="java.util.List<java.lang.String>" -->
<#-- @ftlvariable name="clusterId" type="java.lang.String" -->

<#macro header title tab>
    <!DOCTYPE html>
    <html>
    <head>
        <title>${title} | KafkaHQ</title>
        <meta name="turbolinks-cache-control" content="no-cache" />
        <link rel="shortcut icon"
              type="image/png"
              href="/img/icon.png" />
        <link rel="stylesheet"
              href="https://fonts.googleapis.com/css?family=Open+Sans+Condensed:300,700|Open+Sans:400,700" />
        ${liveReload?no_esc}
        ${vendor_styles?no_esc}
        ${main_styles?no_esc}
        ${vendor_scripts?no_esc}
        ${main_scripts?no_esc}
    <#nested>
    </head>
    <body>
        <div class="wrapper">
            <nav id="sidebar">
                <div class="sidebar-header">
                    <h3><img src="/static/img/logo.svg"/><sup><strong>HQ</strong></sup></h3>
                </div>

                <ul class="list-unstyled components">
                    <li class="${(tab == "cluster")?then("active", "")}">
                        <a href="#clusters"
                            data-toggle="collapse"
                            aria-expanded="false"
                            class="dropdown-toggle"><i
                            class="fa fa-fw fa fa-object-group"
                            aria-hidden="true"></i> Clusters <span class="badge badge-success">${clusterId}</span></a>
                        <ul class="collapse list-unstyled" id="clusters">
                            <#list clusters as cluster>
                            <li>
                                <a href="/${cluster}" class="${(cluster == clusterId)?then("active", "")}">${cluster}</a>
                            </li>
                            </#list>
                        </ul>
                    </li>
                    <li class="${(tab == "topic")?then("active", "")}">
                        <a href="/${clusterId}/topic"><i class="fa fa-fw fa-list" aria-hidden="true"></i> Topics</a>
                    </li>
                    <li class="${(tab == "group")?then("active", "")}">
                        <a href="/${clusterId}/group"><i class="fa fa-fw fa-object-group" aria-hidden="true"></i> Consumer Groups</a>
                    </li>
                </ul>
            </nav>

            <div id="content">
                <div class="title">
                    <h1>
                        <button type="button" id="sidebar-collapse" class="btn btn-dark d-md-none">
                            <i class="fa fa-bars"></i>
                        </button>

                        ${title}
                    </h1>
                </div>

                <main>
</#macro>

<#macro footer>
                </main>
            </div>
        </div>

        <#nested>
        </body>
    </html>
</#macro>

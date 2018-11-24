<#-- @ftlvariable name="datas" type="java.util.List<org.kafkahq.models.Record<java.lang.String, java.lang.String>>" -->
<#-- @ftlvariable name="navbar" type="java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Object>>" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto data-filter">
    <button class="navbar-toggler"
            type="button"
            data-toggle="collapse"
            data-target="#topic-data"
            aria-controls="topic-data"
            aria-expanded="false"
            aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <nav>
        <#include "/blocks/topic/pagination.ftl" />
    </nav>

    <div class="collapse navbar-collapse" id="topic-data">
        <ul class="navbar-nav mr-auto">
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Sort:</strong> (${navbar["sort"]['current'].orElse('')?lower_case?cap_first})
                </a>
                <div class="dropdown-menu">
                    <#list navbar["sort"]['values'] as k, v >
                        <a class="dropdown-item" href="${basePath}${k}">
                            <i class="fa fa-fw fa-sort-numeric-desc" aria-hidden="true"></i> ${v?lower_case?cap_first}
                        </a>
                    </#list>
                </div>
            </li>

            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Partition:</strong> (${navbar["partition"]['current'].orElse('All')})
                </a>
                <div class="dropdown-menu">
                    <#list navbar["partition"]['values'] as k, v >
                        <a class="dropdown-item" href="${basePath}${k}">${v}</a>
                    </#list>
                </div>
            </li>

            <!--
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Start Timestamp:</strong>
                </a>
                <div class="dropdown-menu">
                    <input class="form-control" type="datetime-local" placeholder="Search" aria-label="Search">
                </div>
            </li>
            -->
        </ul>
    </div>
</nav>
<div class="table-responsive">
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th>Key</th>
                <th>Date</th>
                <th>Partition</th>
                <th>Offset</th>
                <th>Headers</th>
            </tr>
        </thead>
        <tbody>
            <#if datas?size == 0>
                <tr>
                    <td colspan="5">
                        <div class="alert alert-info mb-0" role="alert">
                            No data available
                        </div>
                    </td>
                </tr>
            </#if>
            <#assign i=0>
            <#list datas as data>
                <#assign i++>
                <tr>
                    <td><code>${data.getKey()!'null'}</code></td>
                    <td>${data.getTimestamp()?number_to_datetime?string.medium_short}</td>
                    <td class="text-right">${data.getPartition()}</td>
                    <td class="text-right">${data.getOffset()}</td>
                    <td class="text-right">
                        <#if data.getHeaders()?size != 0>
                            <a href="#" data-toggle="collapse" role="button" aria-expanded="false" data-target=".headers-${i}">${data.getHeaders()?size}</a>
                        <#else>
                            ${data.getHeaders()?size}
                        </#if>
                    </td>
                </tr>
                <tr>
                    <td colspan="5">
                        <#if data.getHeaders()?size != 0>
                            <table class="table table-sm collapse headers-${i}">
                                <#list data.getHeaders() as key, value>
                                    <tr>
                                        <th>${key}</th>
                                        <td><pre class="mb-0">${value}</pre></td>
                                    </tr>
                                </#list>
                            </table>
                        </#if>
                        <pre class="mb-0"><code>${data.getValue()!'null'}</code></pre>
                    </td>
                </tr>
            </#list>
        </tbody>
    </table>
</div>
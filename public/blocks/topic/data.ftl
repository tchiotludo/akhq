<#-- @ftlvariable name="datas" type="java.util.List<org.apache.kafka.clients.consumer.ConsumerRecord<java.lang.String, java.lang.String>>" -->
<#-- @ftlvariable name="navbar" type="java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Object>>" -->

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
                        <a class="dropdown-item" href="${k}">
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
                        <a class="dropdown-item" href="${k}">${v}</a>
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
    <#list datas as data>
        <tr>
            <td><code>${data.key()!'null'}</code></td>
            <td>${data.timestamp()?number_to_datetime?string.medium_short}</td>
            <td>${data.partition()}</td>
            <td>${data.offset()}</td>
        </tr>
        <tr>
            <td colspan="4">
                <pre class="mb-0"><code>${data.value()!'null'}</code></pre>
            </td>
        </tr>
    </#list>
        </tbody>
    </table>
</div>
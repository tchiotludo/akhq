<#ftl output_format="HTML">

<#-- @ftlvariable name="datas" type="java.util.List<org.akhq.models.Record<java.lang.Byte[], java.lang.String>>" -->
<#-- @ftlvariable name="navbar" type="java.util.Map<java.lang.String, java.util.Map<java.lang.String, java.lang.Object>>" -->
<#-- @ftlvariable name="partitions" type="int" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="topic" type="org.akhq.models.Topic" -->
<#-- @ftlvariable name="canDeleteRecords" type="java.lang.Boolean" -->

<nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
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
        <#include "../pagination.ftl" />
    </nav>

    <div class="collapse navbar-collapse" id="topic-data">
        <ul class="navbar-nav mr-auto">
            <#if navbar["search"]["current"].isPresent() == false>
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Sort:</strong> (${navbar["sort"]["current"].orElse("")?lower_case?cap_first})
                </a>
                <div class="dropdown-menu">
                    <#list navbar["sort"]["values"] as k, v >
                        <a class="dropdown-item" href="${k}">
                            <i class="fa fa-fw fa-sort-numeric-desc" aria-hidden="true"></i> ${v?lower_case?cap_first}
                        </a>
                    </#list>
                </div>
            </li>
            </#if>

            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Partition:</strong> (${navbar["partition"]["current"].orElse("All")})
                </a>
                <div class="dropdown-menu">
                    <#list navbar["partition"]["values"] as k, v >
                        <a class="dropdown-item" href="${k}">${v}</a>
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
                    <strong>Timestamp:</strong>
                    <#if navbar["timestamp"]["current"].isPresent()>(${navbar["timestamp"]["current"].get()?number_to_datetime?string.medium_short})</#if>
                </a>
                <div class="dropdown-menu khq-data-datetime">
                    <div class="input-group mb-2">
                        <input class="form-control"
                               name="timestamp"
                               type="text"
                               <#if navbar["timestamp"]["current"].isPresent()>
                               value="${navbar["timestamp"]["current"].get()?number_to_datetime?string.iso}"
                               </#if> />
                        <div class="input-group-append">
                            <button class="btn btn-primary" type="button">OK</button>
                        </div>
                    </div>
                    <div class="datetime-container"></div>
                </div>
            </li>
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Search:</strong>
                    <#if navbar["search"]["current"].isPresent()>(${navbar["search"]["current"].get()})</#if>
                </a>
                <div class="dropdown-menu khq-search-navbar">
                    <div class="input-group">
                        <input class="form-control"
                               name="search"
                               type="text"
                                <#if navbar["search"]["current"].isPresent()>
                                    value="${navbar["search"]["current"].get()}"
                                </#if> />
                        <div class="input-group-append">
                            <button class="btn btn-primary" type="button">OK</button>
                        </div>
                    </div>
                </div>
            </li>
            <li class="nav-item dropdown">
                <a class="nav-link dropdown-toggle"
                   href="#"
                   role="button"
                   data-toggle="dropdown"
                   aria-haspopup="true"
                   aria-expanded="false">
                    <strong>Offsets:</strong>
                </a>
                <div class="dropdown-menu khq-offset-navbar">
                    <div class="input-group">
                        <table>
                            <#list 0..partitions-1 as partition>
                                <tr>
                                    <td class="offset-navbar-partition-label offset-navbar-partition-td">Partition ${partition} : </td>
                                    <td class="offset-navbar-partition-td">
                                        <input class="form-control"
                                               type="number"
                                               min="0"
                                           name="${partition}"
                                           type="text"
                                            <#if navbar["offset"][partition?string]??>
                                                value="${navbar["offset"][partition?string]}"
                                            </#if>
                                        />
                                    </td>
                                </tr>
                            </#list>

                        </table>
                        <div class="input-group-append">
                            <button class="btn btn-primary" type="button">OK</button>
                        </div>
                    </div>
                </div>
            </li>
        </ul>
    </div>
</nav>
<div class="table-responsive <#if navbar["search"]["current"].isPresent()>khq-search-sse</#if>">
    <#if navbar["search"]["current"].isPresent()>
    <div class="progress-container">
        <div class="progress">
            <div class="progress-bar" role="progressbar" style="width: 0;" aria-valuemin="0" aria-valuemax="100"></div>
        </div>
        <button type="button" class="btn btn btn-outline-info btn-sm disabled">Cancel</button>
    </div>
    </#if>
    <table class="table table-bordered table-striped table-hover mb-0">
        <thead class="thead-dark">
            <tr>
                <th>Key</th>
                <th>Date</th>
                <th>Partition</th>
                <th>Offset</th>
                <th>Headers</th>
                <th>Schema</th>
                <#if canDeleteRecords == true >
                <th class="khq-row-action"></th>
                </#if>
            </tr>
        </thead>
        <tbody>
            <#if datas?size == 0 && !navbar["search"]["current"].isPresent()>
                <tr>
                    <td colspan="${canDeleteRecords?then("7", "6")}">
                        <div class="alert alert-info mb-0" role="alert">
                            No data available
                        </div>
                    </td>
                </tr>
            </#if>

            <#include "dataBody.ftl" />
        </tbody>
    </table>
</div>

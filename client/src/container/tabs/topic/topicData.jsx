import React, {Component} from 'react';
import {Link} from "react-router-dom";

// Adaptation of data.ftl

class TopicData extends Component {
    state = {
        partitions: [],
        sortBy: 'Oldest',
        sortOptions: [
            'Oldest',
            'Newest'
        ]
    };

    renderSortOptions() {
        const {sortOptions} = this.state;

        let renderedOptions = [];
        for (let option of sortOptions) {
            renderedOptions.push(
                <div>
                    <Link key={option} className="dropdown-item" to="#">
                        <i className="fa fa-fw fa-sort-numeric-desc" aria-hidden="true"/> options
                    </Link>
                </div>
            );
        }
        return renderedOptions;
    };

    render() {
        const {partitions, sortBy} = this.state;

        return (
            <React.Fragment>
                <nav
                    className="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
                    <button className="navbar-toggler"
                            type="button"
                            data-toggle="collapse"
                            data-target="#topic-data"
                            aria-controls="topic-data"
                            aria-expanded="false"
                            aria-label="Toggle navigation">
                        <span className="navbar-toggler-icon"/>
                    </button>

                    <nav>
                        {/*<#include "../pagination.ftl" />*/}
                    </nav>

                    <div className="collapse navbar-collapse" id="topic-data">
                        <ul className="navbar-nav mr-auto">
                            <li className="nav-item dropdown">
                                <Link className="nav-link dropdown-toggle"
                                      to="#"
                                      data-toggle="dropdown"
                                      aria-haspopup="true"
                                      aria-expanded="false">
                                    <strong>Sort:</strong> ({sortBy})
                                </Link>
                                <div className="dropdown-menu">
                                    {this.renderSortOptions()};
                                    {/*<#list navbar["sort"]["values"] as k, v >
                        <a className="dropdown-item" href="${k}">
                            <i className="fa fa-fw fa-sort-numeric-desc" aria-hidden="true"></i> ${v?lower_case?cap_first}
                        </a>
                    </#list>*/}
                                </div>
                            </li>

                            <li className="nav-item dropdown">
                                <Link className="nav-link dropdown-toggle"
                                      to="#"
                                      role="button"
                                      data-toggle="dropdown"
                                      aria-haspopup="true"
                                      aria-expanded="false">
                                    <strong>Partition:</strong> {/*(${navbar["partition"]["current"].orElse("All")})*/}
                                </Link>
                                <div className="dropdown-menu">
                                    {/*<#list navbar["partition"]["values"] as k, v >
                        <Link className="dropdown-item" href="${k}">${v}</Link>
                    </#list>*/}
                                </div>
                            </li>
                            <li className="nav-item dropdown">
                                <Link className="nav-link dropdown-toggle"
                                      to="#"
                                      role="button"
                                      data-toggle="dropdown"
                                      aria-haspopup="true"
                                      aria-expanded="false">
                                    <strong>Timestamp:</strong>
                                    {/*<#if navbar["timestamp"]["current"].isPresent()>(${navbar["timestamp"]["current"].get()?number_to_datetime?string.medium_short})</#if>*/}
                                </Link>
                                <div className="dropdown-menu khq-data-datetime">
                                    <div className="input-group mb-2">
                                        <input className="form-control"
                                               name="timestamp"
                                               type="text"
                                        />
                                        {/*<#if navbar["timestamp"]["current"].isPresent()>
                               value="${navbar["timestamp"]["current"].get()?number_to_datetime?string.iso}"
                               </#if> */}
                                        <div className="input-group-append">
                                            <button className="btn btn-primary" type="button">OK</button>
                                        </div>
                                    </div>
                                    <div className="datetime-container"/>
                                </div>
                            </li>
                            <li className="nav-item dropdown">
                                <Link className="nav-link dropdown-toggle"
                                      to="#"
                                      role="button"
                                      data-toggle="dropdown"
                                      aria-haspopup="true"
                                      aria-expanded="false">
                                    <strong>Search:</strong>
                                    {/*<#if navbar["search"]["current"].isPresent()>(${navbar["search"]["current"].get()})</#if>*/}
                                </Link>
                                <div className="dropdown-menu khq-search-navbar">
                                    <div className="input-group">
                                        <input className="form-control"
                                               name="search"
                                               type="text"
                                        />
                                        {/*<#if navbar["search"]["current"].isPresent()>
                                    value="${navbar["search"]["current"].get()}"
                                </#if> */}
                                        <div className="input-group-append">
                                            <button className="btn btn-primary" type="button">OK</button>
                                        </div>
                                    </div>
                                </div>
                            </li>
                            <li className="nav-item dropdown">
                                <Link className="nav-link dropdown-toggle"
                                      to="#"
                                      role="button"
                                      data-toggle="dropdown"
                                      aria-haspopup="true"
                                      aria-expanded="false">
                                    <strong>Offsets:</strong>
                                </Link>
                                <div className="dropdown-menu khq-offset-navbar">
                                    <div className="input-group">
                                        <table>
                                            {/*<#list 0..partitions-1 as partition>
                                <tr>
                                    <td className="offset-navbar-partition-label offset-navbar-partition-td">Partition ${partition} : </td>
                                    <td className="offset-navbar-partition-td">
                                        <input className="form-control"
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
                            </#list>*/}

                                        </table>
                                        <div className="input-group-append">
                                            <button className="btn btn-primary" type="button">OK</button>
                                        </div>
                                    </div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </nav>
                {/*<div className="table-responsive <#if navbar["search"]["current"].isPresent()>khq-search-sse</#if>">
                                        <#if navbar["search"]["current"].isPresent()>*/}
                <div className="table-responsive">
                    {/*<div className="progress-container">*/}
                    {/*    <div className="progress">*/}
                    {/*        <div className="progress-bar" role="progressbar"*/}
                    {/*             style={{width: "0", ariaValueMin: "0", ariaValueMax: "100"}}/>*/}
                    {/*    </div>*/}
                    {/*    <button type="button" className="btn btn btn-outline-info btn-sm disabled">Cancel</button>*/}
                    {/*</div>*/}
                    {/*</#if>*/}
                    <table className="table table-bordered table-striped table-hover mb-0">
                        <thead className="thead-dark">
                        <tr>
                            <th>Key</th>
                            <th>Date</th>
                            <th>Partition</th>
                            <th>Offset</th>
                            <th>Headers</th>
                            <th>Schema</th>
                            {/*<#if canDeleteRecords == true >
                <th className="khq-row-action"></th>
            </#if>*/}
                        </tr>
                        </thead>
                        <tbody>
                        {/*#if datas?size == 0 && !navbar["search"]["current"].isPresent()*/}
                        <tr>
                            {/*td colspan="${canDeleteRecords?then("7", "6")}"*/}
                            <td colSpan="7">
                                <div className="alert alert-info mb-0" role="alert">
                                    No data available
                                </div>
                            </td>
                        </tr>
                        {/*</#if>*/}

                        {/*<#include "dataBody.ftl" />*/}
                        </tbody>
                    </table>
                </div>
            </React.Fragment>
        );
    }
}

export default TopicData;
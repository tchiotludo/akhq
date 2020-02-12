import React from 'react';
import Pagination from "./pagination";

function SearchBar({pagination, topicListView}) {
    return (
        <nav className="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
            <button className="navbar-toggler"
                    type="button"
                    data-toggle="collapse"
                    data-target="#navbar-search"
                    aria-controls="navbar-search"
                    aria-expanded="false"
                    aria-label="Toggle navigation">
                <span className="navbar-toggler-icon"/>
            </button>


            {pagination ? <nav><Pagination/></nav> : <div></div>}

            <div className="collapse navbar-collapse" id="navbar-search">
                <form className="form-inline mr-auto khq-form-get" method="get">
                    <input className="form-control"
                           name="search"
                           placeholder="Search"
                           autoComplete="off"
                           type="text"/>
                    {/*<#if search.isPresent()>
            value="${search.get()}"*/}
                    {/*</#if> />
        <#if topicListView??>
        <select name="show" className="khq-select" data-style="btn-white">
        <option ${(topicListView.toString() == "ALL")?then("selected", "")} value="ALL">Show all topics</option>
        <option ${(topicListView.toString() == "HIDE_INTERNAL")?then("selected", "")} value="HIDE_INTERNAL">Hide internal topics</option>
        <option ${(topicListView.toString() == "HIDE_INTERNAL_STREAM")?then("selected", "")} value="HIDE_INTERNAL_STREAM">Hide internal & stream topics</option>
        <option ${(topicListView.toString() == "HIDE_STREAM")?then("selected", "")} value="HIDE_STREAM">Hide stream topics</option>
    </select>
    </#if>*/}
                    {
                        topicListView &&
                        <select name="show" className="khq-select form-control" data-style="btn-white">
                            {/*<option ${(topicListView.toString() == "ALL") ? then("selected", "")}*/}
                            <option value="ALL">Show all topics
                            </option>
                            {/*<option ${(topicListView.toString() == "HIDE_INTERNAL") ? then("selected", "")}*/}
                            <option value="HIDE_INTERNAL">Hide internal topics
                            </option>
                            {/*<option ${(topicListView.toString() == "HIDE_INTERNAL_STREAM") ? then("selected", "")}*/}
                            <option value="HIDE_INTERNAL_STREAM">Hide internal & stream topics
                            </option>
                            {/*<option ${(topicListView.toString() == "HIDE_STREAM") ? then("selected", "")}*/}
                            <option value="HIDE_STREAM">Hide stream topics
                            </option>
                        </select>
                    }


                    <button className="btn btn-primary" type="submit">
                        <span className="d-md-none">Search </span><i className="fa fa-search"/>
                    </button>
                </form>
            </div>
        </nav>
    );
}

export default SearchBar;
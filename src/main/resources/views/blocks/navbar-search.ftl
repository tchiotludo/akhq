<#ftl output_format="HTML">

<#-- @ftlvariable name="search" type="java.util.Optional<java.lang.String>" -->
<#-- @ftlvariable name="topicListView" type="org.kafkahq.repositories.TopicRepository.TopicListView" -->

<nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter khq-sticky khq-nav">
    <button class="navbar-toggler"
            type="button"
            data-toggle="collapse"
            data-target="#navbar-search"
            aria-controls="navbar-search"
            aria-expanded="false"
            aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
    </button>

    <#if pagination??>
        <nav>
            <#include "pagination.ftl" />
        </nav>
    </#if>
    
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
            <#if topicListView??>
                <select name="show" class="khq-select" data-style="btn-white">
                    <option ${(topicListView.toString() == "ALL")?then("selected", "")} value="ALL">Show all topics</option>
                    <option ${(topicListView.toString() == "HIDE_INTERNAL")?then("selected", "")} value="HIDE_INTERNAL">Hide internal topics</option>
                    <option ${(topicListView.toString() == "HIDE_INTERNAL_STREAM")?then("selected", "")} value="HIDE_INTERNAL_STREAM">Hide internal & stream topics</option>
                    <option ${(topicListView.toString() == "HIDE_STREAM")?then("selected", "")} value="HIDE_STREAM">Hide stream topics</option>
                </select>
            </#if>

            <button class="btn btn-primary" type="submit">
                <span class="d-md-none">Search </span><i class="fa fa-search"></i>
            </button>
        </form>
    </div>
</nav>
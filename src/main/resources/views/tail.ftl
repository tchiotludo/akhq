<#ftl output_format="HTML">

<#-- @ftlvariable name="search" type="java.util.Optional<java.lang.String>" -->
<#-- @ftlvariable name="topicsList" type="java.util.List<java.lang.String>" -->
<#-- @ftlvariable name="topics" type="java.util.List<java.lang.String>" -->
<#-- @ftlvariable name="size" type="java.lang.Integer" -->

<#import "includes/template.ftl" as template>
<#import "includes/functions.ftl" as functions>

<@template.header "Live Tail", "tail" />

<div class="khq-tail-sse" data-max-rows="${size}">
    <nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-sticky khq-nav">
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
                        </#if>
                />
                <select
                        name="topics"
                        class="khq-select"
                        multiple
                        data-selected-text-format="count"
                        data-style="btn-white"
                        data-actions-box="true"
                        data-live-search="true"
                        title="Topics"
                >
                    <#list topicsList as topic>
                        <option value="${topic}" ${(topics?seq_contains(topic) == true)?then("selected", "")}>${topic}</option>
                    </#list>
                </select>
                <select
                        name="size"
                        class="khq-select"
                        data-style="btn-white"
                        title="Max records"
                >
                    <#list [50, 100, 250, 500, 1000, 2500] as current>
                        <option value="${current}" title="Max records: ${current}" ${(size == current)?then("selected", "")}>${current}</option>
                    </#list>
                </select>

                <button class="btn btn-primary" type="submit">
                    <span class="d-md-none">Search </span><i class="fa fa-search"></i>
                </button>
                <#if topics?size  != 0>
                <div class="btn-group" role="group">
                    <button class="btn btn-secondary pause d-none">
                        <i class="fa fa-pause"></i><span> Pause</span>
                    </button>

                    <button class="btn btn-secondary resume d-none">
                        <i class="fa fa-play"></i> <span> Resume</span>
                    </button>

                    <button class="btn btn-secondary empty d-none">
                        <i class="fa fa-remove"></i> <span> Clear</span>
                    </button>
                </div>
                </#if>
            </form>
        </div>
    </nav>
    <#if topics?size != 0>
        <div class="table-responsive">
            <table class="table table-bordered table-striped table-hover mb-0">
                <thead class="thead-dark">
                    <tr>
                        <th>Topic</th>
                        <th>Key</th>
                        <th>Date</th>
                        <th>Partition</th>
                        <th>Offset</th>
                        <th>Headers</th>
                        <th>Schema</th>
                    </tr>
                </thead>
                <tbody>

                </tbody>
            </table>
        </div>
    </#if>
</div>


<@template.footer/>
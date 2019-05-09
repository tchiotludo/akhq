<#ftl output_format="HTML">

<#-- @ftlvariable name="search" type="java.util.Optional<java.lang.String>" -->

<nav class="navbar navbar-expand-lg navbar-light bg-light mr-auto khq-data-filter">
    <form class="form-inline khq-form-get" method="get">
        <div class="input-group">
            <input class="form-control"
                   name="search"
                   placeholder="Search"
                   autocomplete="off"
                   type="text"
                    <#if search.isPresent()>
                        value="${search.get()}"
                    </#if> />
            <div class="input-group-append">
                <button class="btn btn-primary" type="button">
                    <i class="fa fa-search"></i>
                </button>
            </div>
        </div>
    </form>
</nav>
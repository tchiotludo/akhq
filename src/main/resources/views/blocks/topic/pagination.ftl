<#ftl output_format="HTML">

<#-- @ftlvariable name="pagination" type="java.util.Map<java.lang.String, java.lang.String>" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->

<#assign size = pagination["size"] >
<#assign before = pagination["before"] >
<#assign after = pagination["after"] >

<ul class="pagination mb-0 ml-sm-2">
    <!--
    <li class="page-item before ${(before == "")?then('disabled', '')}">
        <a class="page-link" aria-label="Previous" ${(before != "")?then(' href="'?no_esc + basePath + before + '"'?no_esc, '')}>
            <span aria-hidden="true">&laquo;</span>
            <span class="sr-only">Previous</span>
        </a>
    </li>
    -->
    <li class="page-item info">
        <a class="page-link">≈ ${size}</a>
    </li>
    <li class="page-item after ${(after == "")?then('disabled', '')}">
        <a class="page-link" aria-label="Next" ${(after != "")?then(' href="'?no_esc + basePath + after + '"'?no_esc, '')}>
            <span aria-hidden="true">&raquo;</span>
            <span class="sr-only">Next</span>
        </a>
    </li>
</ul>

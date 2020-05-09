<#ftl output_format="HTML">

<#macro badge node type="info">
    <#-- @ftlvariable name="node" type="org.akhq.models.Node" -->

    <a title="${node.getHost()}" data-toggle="tooltip" data-placement="top" href="#">
        <span class="badge badge-${type}">${node.getId()?c}</span>
    </a>
</#macro>

<#ftl output_format="HTML">

<#macro badge node>
    <#-- @ftlvariable name="node" type="org.kafkahq.models.Node" -->

    <a title="${node.getHost()}" data-toggle="tooltip" data-placement="top" href="#">
        <span class="badge badge-info">${node.getId()?c}</span>
    </a>
</#macro>
<#ftl output_format="HTML">

<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="datas" type="java.util.List<org.akhq.models.Record<java.lang.Byte[], java.lang.Byte[]>>" -->
<#-- @ftlvariable name="topic" type="org.akhq.models.Topic" -->
<#-- @ftlvariable name="canDeleteRecords" type="java.lang.Boolean" -->
<#-- @ftlvariable name="roles" type="java.util.ArrayList<java.lang.String>" -->
<#-- @ftlvariable name="displayTopic" type="java.lang.Boolean" -->

<#assign canDelete = roles?seq_contains("topic/data/delete") && canDeleteRecords>
<#assign i=0>

<#list datas as data>
    <#assign i++>
    <tr class="reduce <#if !(data.getValue())??>deleted</#if>">
        <#if displayTopic?? && displayTopic == true>
            <td>${data.getTopic()}</td>
        </#if>
        <td><code class="key">${data.getKey()!"null"}</code></td>
        <td>${data.getDate()?datetime?string.xs_ms_nz?replace("T", " ")}</td>
        <td class="text-right">${data.getPartition()}</td>
        <td class="text-right">${data.getOffset()}</td>
        <td class="text-right">
            <#if data.getHeaders()?size != 0>
                <a href="#" data-toggle="collapse" role="button" aria-expanded="false" data-target=".headers-${i}">${data.getHeaders()?size}</a>
            <#else>
                ${data.getHeaders()?size}
            </#if>
        </td>
        <td class="text-right">
            <#if data.getKeySchemaId()??>
                <a href="${basePath}/${clusterId}/schema/id/${data.getKeySchemaId()?c}" class="badge badge-info">Key: ${data.getKeySchemaId()?c}</a>
            </#if>
            <#if data.getValueSchemaId()??>
                <a href="${basePath}/${clusterId}/schema/id/${data.getValueSchemaId()?c}" class="badge badge-info">Value: ${data.getValueSchemaId()?c}</a>
            </#if>
        </td>
        <#if canDelete == true >
            <td>
                <#if data.getKeyAsBase64()??>
                    <a
                            href="${basePath}/${clusterId}/topic/${topic.getName()}/deleteRecord?partition=${data.getPartition()}&key=${data.getKeyAsBase64()}"
                            data-confirm="Do you want to delete record <code>${data.getKey()!"null"} from topic ${topic.getName()}</code> ?"
                    ><i class="fa fa-trash"></i></a>
                </#if>
            </td>
        </#if>
    </tr>
    <tr<#if !(data.getValue())??> class="deleted"</#if>>
        <td colspan="${(canDelete == true || (displayTopic?? && displayTopic) == true)?then("7", "6")}">
            <button type="button" class="close d-none" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
            <#if data.getHeaders()?size != 0>
                <table class="table table-sm collapse headers-${i}">
                    <#list data.getHeaders() as key, value>
                        <tr>
                            <th>${key}</th>
                            <td><pre class="mb-0">${value!"null"}</pre></td>
                        </tr>
                    </#list>
                </table>
            </#if>
            <pre class="mb-0 khq-data-highlight"><code>${(data.getValue()?esc)!"null"}</code></pre>
        </td>
    </tr>
</#list>

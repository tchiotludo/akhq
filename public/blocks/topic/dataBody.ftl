<#-- @ftlvariable name="clusterId" type="java.lang.String" -->
<#-- @ftlvariable name="basePath" type="java.lang.String" -->
<#-- @ftlvariable name="datas" type="java.util.List<org.kafkahq.models.Record<java.lang.String, java.lang.String>>" -->
<#-- @ftlvariable name="topic" type="org.kafkahq.models.Topic" -->
<#-- @ftlvariable name="canDeleteRecords" type="java.lang.Boolean" -->

<#assign i=0>
<#list datas as data>
    <#assign i++>
    <tr class="reduce <#if !(data.getValue())??>deleted</#if>">
        <td><code class="key">${data.getKey()!"null"}</code></td>
        <td>${data.getTimestamp()?number_to_datetime?string.medium_short}</td>
        <td class="text-right">${data.getPartition()}</td>
        <td class="text-right">${data.getOffset()}</td>
        <td class="text-right">
            <#if data.getHeaders()?size != 0>
                <a href="#" data-toggle="collapse" role="button" aria-expanded="false" data-target=".headers-${i}">${data.getHeaders()?size}</a>
            <#else>
                ${data.getHeaders()?size}
            </#if>
        </td>
        <#if canDeleteRecords == true >
            <td>
                <#if data.getValue()??>
                    <a
                            href="${basePath}/${clusterId}/topic/${topic.getName()}/deleteRecord?partition=${data.getPartition()}&key=${data.getKey()}"
                            data-confirm="Do you want to delete record <br /><strong>${data.getKey()} from topic ${topic.getName()}</strong><br /><br /> ?"
                    ><i class="fa fa-trash"></i></a>
                </#if>
            </td>
        </#if>
    </tr>
    <tr<#if !(data.getValue())??> class="deleted"</#if>>
        <td colspan="${(canDeleteRecords == true)?then("6", "5")}">
            <button type="button" class="close d-none" aria-label="Close">
                <span aria-hidden="true">&times;</span>
            </button>
            <#if data.getHeaders()?size != 0>
                <table class="table table-sm collapse headers-${i}">
                    <#list data.getHeaders() as key, value>
                        <tr>
                            <th>${key}</th>
                            <td><pre class="mb-0">${value}</pre></td>
                        </tr>
                    </#list>
                </table>
            </#if>
            <pre class="mb-0 khq-data-highlight"><code>${data.getValue()!"null"}</code></pre>
        </td>
    </tr>
</#list>

<#ftl output_format="HTML">

<#function filesize num>
    <#assign order = num?round?c?length />
    <#assign thousands = ((order - 1) / 3)?floor />
    <#if (thousands < 0)><#assign thousands = 0 /></#if>
    <#assign siMap = [ {"factor": 1, "unit": " B"}, {"factor": 1024, "unit": " KiB"}, {"factor": 1024 * 1024, "unit": " MiB"}, {"factor": 1024 * 1024 * 1024, "unit":" GiB"}, {"factor": 1024 * 1024 * 1024 * 1024, "unit": " TiB"} ]/>
    <#assign siStr = (num / (siMap[thousands].factor))?string("0.#") + siMap[thousands].unit />
    <#return siStr />
</#function>


<#function filesize num>
    <#assign order = num?round?c?length />
    <#assign thousands = ((order - 1) / 3)?floor />
    <#if (thousands < 0)><#assign thousands = 0 /></#if>
    <#assign siMap = [ {"factor": 1, "unit": " b"}, {"factor": 1000, "unit": " ko"}, {"factor": 1000000, "unit": " mo"}, {"factor": 1000000000, "unit":" go"}, {"factor": 1000000000000, "unit": " to"} ]/>
    <#assign siStr = (num / (siMap[thousands].factor))?string("0.#") + siMap[thousands].unit />
    <#return siStr />
</#function>


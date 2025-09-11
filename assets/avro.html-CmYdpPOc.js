import{_ as o,c as t,d as a,b as i,e as s,a as r,w as l,r as c,o as p}from"./app-DtddHu5R.js";const d={};function u(m,e){const n=c("RouteLink");return p(),t("div",null,[e[3]||(e[3]=a("h1",{id:"avro-deserialization",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#avro-deserialization"},[a("span",null,"Avro deserialization")])],-1)),a("p",null,[e[1]||(e[1]=s("Avro messages using Schema registry are automatically decoded if the registry is configured (see ")),r(n,{to:"/docs/configuration/brokers.html"},{default:l(()=>e[0]||(e[0]=[s("Kafka cluster")])),_:1,__:[0]}),e[2]||(e[2]=s(")."))]),e[4]||(e[4]=i(`<p>You can also decode raw binary Avro messages, that is messages encoded directly with <a href="https://avro.apache.org/docs/current/api/java/org/apache/avro/io/DatumWriter.html" target="_blank" rel="noopener noreferrer">DatumWriter</a> without any header. You must provide a <code>schemas-folder</code> and mappings which associate a <code>topic-regex</code> and a schema file name. The schema can be specified either for message keys with <code>key-schema-file</code> and/or for values with <code>value-schema-file</code>.</p><p>Here is an example of configuration:</p><div class="language-text line-numbers-mode" data-highlighter="prismjs" data-ext="text"><pre><code class="language-text"><span class="line">akhq:</span>
<span class="line">  connections:</span>
<span class="line">    kafka:</span>
<span class="line">      properties:</span>
<span class="line">        # standard kafka properties</span>
<span class="line">      deserialization:</span>
<span class="line">        avro-raw:</span>
<span class="line">          schemas-folder: &quot;/app/avro_schemas&quot;</span>
<span class="line">          topics-mapping:</span>
<span class="line">            - topic-regex: &quot;album.*&quot;</span>
<span class="line">              value-schema-file: &quot;Album.avsc&quot;</span>
<span class="line">            - topic-regex: &quot;film.*&quot;</span>
<span class="line">              value-schema-file: &quot;Film.avsc&quot;</span>
<span class="line">            - topic-regex: &quot;test.*&quot;</span>
<span class="line">              key-schema-file: &quot;Key.avsc&quot;</span>
<span class="line">              value-schema-file: &quot;Value.avsc&quot;</span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"></div></div><p>Examples can be found in <a href="https://github.com/tchiotludo/akhq/tree/dev/src/main/java/org/akhq/utils" target="_blank" rel="noopener noreferrer">tests</a>.</p>`,4))])}const f=o(d,[["render",u]]),g=JSON.parse('{"path":"/docs/configuration/avro.html","title":"Avro deserialization","lang":"en-US","frontmatter":{},"git":{"updatedTime":1757575312000,"contributors":[{"name":"Alex Mihalenok","username":"","email":"aleksej.voroneckij@icloud.com","commits":1}],"changelog":[{"hash":"08b34a71c4fac055e493a4078bd4e5d75f628845","time":1757575312000,"email":"aleksej.voroneckij@icloud.com","author":"Alex Mihalenok","message":"feat(helm): add annotations option to secret too (#2476)"}]},"filePathRelative":"docs/configuration/avro.md"}');export{f as comp,g as data};

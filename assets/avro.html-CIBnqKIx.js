import{_ as o,c as t,d as a,b as r,e as s,a as i,w as l,r as c,o as p}from"./app--BFGRV3M.js";const m={};function d(u,e){const n=c("RouteLink");return p(),t("div",null,[e[3]||(e[3]=a("h1",{id:"avro-deserialization",tabindex:"-1"},[a("a",{class:"header-anchor",href:"#avro-deserialization"},[a("span",null,"Avro deserialization")])],-1)),a("p",null,[e[1]||(e[1]=s("Avro messages using Schema registry are automatically decoded if the registry is configured (see ")),i(n,{to:"/docs/configuration/brokers.html"},{default:l(()=>e[0]||(e[0]=[s("Kafka cluster")])),_:1,__:[0]}),e[2]||(e[2]=s(")."))]),e[4]||(e[4]=r(`<p>You can also decode raw binary Avro messages, that is messages encoded directly with <a href="https://avro.apache.org/docs/current/api/java/org/apache/avro/io/DatumWriter.html" target="_blank" rel="noopener noreferrer">DatumWriter</a> without any header. You must provide a <code>schemas-folder</code> and mappings which associate a <code>topic-regex</code> and a schema file name. The schema can be specified either for message keys with <code>key-schema-file</code> and/or for values with <code>value-schema-file</code>.</p><p>Here is an example of configuration:</p><div class="language-text line-numbers-mode" data-highlighter="prismjs" data-ext="text"><pre><code class="language-text"><span class="line">akhq:</span>
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
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"></div></div><p>Examples can be found in <a href="https://github.com/tchiotludo/akhq/tree/dev/src/main/java/org/akhq/utils" target="_blank" rel="noopener noreferrer">tests</a>.</p>`,4))])}const f=o(m,[["render",d]]),g=JSON.parse('{"path":"/docs/configuration/avro.html","title":"Avro deserialization","lang":"en-US","frontmatter":{},"git":{"updatedTime":1749561554000,"contributors":[{"name":"Christopher Poenaru","username":"","email":"kiambogo@gmail.com","commits":1},{"name":"Claude","username":"Claude","email":"noreply@anthropic.com","commits":1,"url":"https://github.com/Claude"}],"changelog":[{"hash":"28adebe3a94c587b147638fe9a60eaf0ab6b268b","time":1749561554000,"email":"kiambogo@gmail.com","author":"Christopher Poenaru","message":"feat(helm): add configurable targetPort support for sidecars","coAuthors":[{"name":"Claude","email":"noreply@anthropic.com"}]}]},"filePathRelative":"docs/configuration/avro.md"}');export{f as comp,g as data};

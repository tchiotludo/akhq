import{_ as t,c as o,d as e,b as i,e as s,a as r,w as l,r as c,o as p}from"./app-DB88e0CB.js";const d={};function u(m,a){const n=c("RouteLink");return p(),o("div",null,[a[3]||(a[3]=e("h1",{id:"avro-deserialization",tabindex:"-1"},[e("a",{class:"header-anchor",href:"#avro-deserialization"},[e("span",null,"Avro deserialization")])],-1)),e("p",null,[a[1]||(a[1]=s("Avro messages using Schema registry are automatically decoded if the registry is configured (see ")),r(n,{to:"/docs/configuration/brokers.html"},{default:l(()=>a[0]||(a[0]=[s("Kafka cluster")])),_:1,__:[0]}),a[2]||(a[2]=s(")."))]),a[4]||(a[4]=i(`<p>You can also decode raw binary Avro messages, that is messages encoded directly with <a href="https://avro.apache.org/docs/current/api/java/org/apache/avro/io/DatumWriter.html" target="_blank" rel="noopener noreferrer">DatumWriter</a> without any header. You must provide a <code>schemas-folder</code> and mappings which associate a <code>topic-regex</code> and a schema file name. The schema can be specified either for message keys with <code>key-schema-file</code> and/or for values with <code>value-schema-file</code>.</p><p>Here is an example of configuration:</p><div class="language-text line-numbers-mode" data-highlighter="prismjs" data-ext="text"><pre><code class="language-text"><span class="line">akhq:</span>
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
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"></div></div><p>Examples can be found in <a href="https://github.com/tchiotludo/akhq/tree/dev/src/main/java/org/akhq/utils" target="_blank" rel="noopener noreferrer">tests</a>.</p>`,4))])}const f=t(d,[["render",u]]),g=JSON.parse('{"path":"/docs/configuration/avro.html","title":"Avro deserialization","lang":"en-US","frontmatter":{},"git":{"updatedTime":1748466332000,"contributors":[{"name":"stiwa-maha","username":"stiwa-maha","email":"203819859+stiwa-maha@users.noreply.github.com","commits":1,"url":"https://github.com/stiwa-maha"}],"changelog":[{"hash":"74f8a0cd92b6f4ad2e3fe6494856365bbbe88771","time":1748466332000,"email":"203819859+stiwa-maha@users.noreply.github.com","author":"stiwa-maha","message":"fix(build): add graceful shutdown for docker"}]},"filePathRelative":"docs/configuration/avro.md"}');export{f as comp,g as data};

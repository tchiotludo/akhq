import{i as e,r as t,s as n,t as r}from"./app-D04v3DBQ.js";var i=JSON.parse(`{"path":"/docs/configuration/schema-registry/glue.html","title":"Glue schema registry","lang":"en-US","frontmatter":{},"git":{"updatedTime":1778661920000,"contributors":[{"name":"dependabot[bot]","username":"dependabot[bot]","email":"49699333+dependabot[bot]@users.noreply.github.com","commits":2,"url":"https://github.com/dependabot[bot]"}],"changelog":[{"hash":"cf19c19b3dfe2b1911d675a9334f15e33e45bc08","time":1778661920000,"email":"49699333+dependabot[bot]@users.noreply.github.com","author":"dependabot[bot]","message":"chore(deps): bump com.google.protobuf:protobuf-java-util (#2851)","coAuthors":[{"name":"dependabot[bot]","email":"49699333+dependabot[bot]@users.noreply.github.com"}]}]},"filePathRelative":"docs/configuration/schema-registry/glue.md"}`),a={name:`glue.md`};function o(r,i,a,o,s,c){return n(),t(`div`,null,[...i[0]||=[e(`<h1 id="glue-schema-registry" tabindex="-1"><a class="header-anchor" href="#glue-schema-registry"><span>Glue schema registry</span></a></h1><p>Currently ,glue schema registry support is limited to only de-serialisation of avro/protobuf/json serialized messages. It can be configured as below.</p><div class="language-yaml line-numbers-mode" data-highlighter="prismjs" data-ext="yml"><pre><code class="language-yaml"><span class="line">  <span class="token key atrule">akhq</span><span class="token punctuation">:</span></span>
<span class="line">    <span class="token key atrule">environment</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token key atrule">AKHQ_CONFIGURATION</span><span class="token punctuation">:</span> <span class="token punctuation">|</span><span class="token scalar string"></span>
<span class="line">        akhq:</span>
<span class="line">          connections:</span>
<span class="line">            docker-kafka-server:</span>
<span class="line">              properties:</span>
<span class="line">                bootstrap.servers: &quot;kafka:9092&quot;</span>
<span class="line">              schema-registry:</span>
<span class="line">                url: &quot;http://schema-registry:8085&quot;</span>
<span class="line">                type: &quot;glue&quot;</span>
<span class="line">                glueSchemaRegistryName: Name of schema Registry</span>
<span class="line">                awsRegion: aws region</span>
<span class="line">              connect:</span>
<span class="line">                - name: &quot;connect&quot;</span>
<span class="line">                  url: &quot;http://connect:8083&quot;</span></span>
<span class="line">    <span class="token key atrule">ports</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token punctuation">-</span> 8080<span class="token punctuation">:</span><span class="token number">8080</span></span>
<span class="line">    <span class="token key atrule">links</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token punctuation">-</span> kafka</span>
<span class="line">      <span class="token punctuation">-</span> repo</span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Please note that authentication is done using aws default credentials provider.</p><p>Url key is required to not break the flow.</p>`,5)]])}var s=r(a,[[`render`,o]]);export{i as _pageData,s as default};
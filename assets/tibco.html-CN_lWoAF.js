import{_ as s,c as a,b as t,o as e}from"./app-CAPi6cFF.js";const p={};function o(c,n){return e(),a("div",null,n[0]||(n[0]=[t(`<h1 id="tibco-schema-registry" tabindex="-1"><a class="header-anchor" href="#tibco-schema-registry"><span>TIBCO schema registry</span></a></h1><p>If you are using the TIBCO schema registry, you will also need to mount and use the TIBCO Avro client library and its dependencies. The akhq service in a docker compose file might look something like:</p><div class="language-yaml line-numbers-mode" data-highlighter="prismjs" data-ext="yml"><pre><code class="language-yaml"><span class="line">  <span class="token key atrule">akhq</span><span class="token punctuation">:</span></span>
<span class="line">    <span class="token comment"># build:</span></span>
<span class="line">    <span class="token comment">#   context: .</span></span>
<span class="line">    <span class="token key atrule">image</span><span class="token punctuation">:</span> tchiotludo/akhq</span>
<span class="line">    <span class="token key atrule">volumes</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token punctuation">-</span> /opt/tibco/akd/repo/1.2/lib/tibftl<span class="token punctuation">-</span>kafka<span class="token punctuation">-</span>avro<span class="token punctuation">-</span>1.2.0<span class="token punctuation">-</span>thin.jar<span class="token punctuation">:</span>/app/tibftl<span class="token punctuation">-</span>kafka<span class="token punctuation">-</span>avro<span class="token punctuation">-</span>1.2.0<span class="token punctuation">-</span>thin.jar</span>
<span class="line">      <span class="token punctuation">-</span> /opt/tibco/akd/repo/1.2/lib/deps<span class="token punctuation">:</span>/app/deps</span>
<span class="line">    <span class="token key atrule">environment</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token key atrule">AKHQ_CONFIGURATION</span><span class="token punctuation">:</span> <span class="token punctuation">|</span><span class="token scalar string"></span>
<span class="line">        akhq:</span>
<span class="line">          connections:</span>
<span class="line">            docker-kafka-server:</span>
<span class="line">              properties:</span>
<span class="line">                bootstrap.servers: &quot;kafka:9092&quot;</span>
<span class="line">              schema-registry:</span>
<span class="line">                type: &quot;tibco&quot;</span>
<span class="line">                url: &quot;http://repo:8081&quot;</span>
<span class="line">              connect:</span>
<span class="line">                - name: &quot;connect&quot;</span>
<span class="line">                  url: &quot;http://connect:8083&quot;</span></span>
<span class="line">      <span class="token key atrule">CLASSPATH</span><span class="token punctuation">:</span> <span class="token string">&quot;/app/tibftl-kafka-avro-1.2.0-thin.jar:/app/deps/*&quot;</span></span>
<span class="line">    <span class="token key atrule">ports</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token punctuation">-</span> 8080<span class="token punctuation">:</span><span class="token number">8080</span></span>
<span class="line">    <span class="token key atrule">links</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token punctuation">-</span> kafka</span>
<span class="line">      <span class="token punctuation">-</span> repo</span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"></div></div>`,3)]))}const i=s(p,[["render",o]]),r=JSON.parse('{"path":"/docs/configuration/schema-registry/tibco.html","title":"TIBCO schema registry","lang":"en-US","frontmatter":{},"git":{"updatedTime":1747659110000,"contributors":[{"name":"dependabot[bot]","username":"dependabot[bot]","email":"49699333+dependabot[bot]@users.noreply.github.com","commits":1,"url":"https://github.com/dependabot[bot]"}],"changelog":[{"hash":"fc03668e8f5a665b71a61f68772b176e18f7f7a7","time":1747659110000,"email":"49699333+dependabot[bot]@users.noreply.github.com","author":"dependabot[bot]","message":"chore(deps): bump io.projectreactor:reactor-core from 3.7.5 to 3.7.6"}]},"filePathRelative":"docs/configuration/schema-registry/tibco.md"}');export{i as comp,r as data};

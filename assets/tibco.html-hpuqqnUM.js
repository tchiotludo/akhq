import{_ as n,o as s,c as a,e}from"./app-jwr6spCk.js";const t={},i=e(`<h1 id="tibco-schema-registry" tabindex="-1"><a class="header-anchor" href="#tibco-schema-registry" aria-hidden="true">#</a> TIBCO schema registry</h1><p>If you are using the TIBCO schema registry, you will also need to mount and use the TIBCO Avro client library and its dependencies. The akhq service in a docker compose file might look something like:</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code>  <span class="token key atrule">akhq</span><span class="token punctuation">:</span>
    <span class="token comment"># build:</span>
    <span class="token comment">#   context: .</span>
    <span class="token key atrule">image</span><span class="token punctuation">:</span> tchiotludo/akhq
    <span class="token key atrule">volumes</span><span class="token punctuation">:</span>
      <span class="token punctuation">-</span> /opt/tibco/akd/repo/1.2/lib/tibftl<span class="token punctuation">-</span>kafka<span class="token punctuation">-</span>avro<span class="token punctuation">-</span>1.2.0<span class="token punctuation">-</span>thin.jar<span class="token punctuation">:</span>/app/tibftl<span class="token punctuation">-</span>kafka<span class="token punctuation">-</span>avro<span class="token punctuation">-</span>1.2.0<span class="token punctuation">-</span>thin.jar
      <span class="token punctuation">-</span> /opt/tibco/akd/repo/1.2/lib/deps<span class="token punctuation">:</span>/app/deps
    <span class="token key atrule">environment</span><span class="token punctuation">:</span>
      <span class="token key atrule">AKHQ_CONFIGURATION</span><span class="token punctuation">:</span> <span class="token punctuation">|</span><span class="token scalar string">
        akhq:
          connections:
            docker-kafka-server:
              properties:
                bootstrap.servers: &quot;kafka:9092&quot;
              schema-registry:
                type: &quot;tibco&quot;
                url: &quot;http://repo:8081&quot;
              connect:
                - name: &quot;connect&quot;
                  url: &quot;http://connect:8083&quot;</span>
      <span class="token key atrule">CLASSPATH</span><span class="token punctuation">:</span> <span class="token string">&quot;/app/tibftl-kafka-avro-1.2.0-thin.jar:/app/deps/*&quot;</span>
    <span class="token key atrule">ports</span><span class="token punctuation">:</span>
      <span class="token punctuation">-</span> 8080<span class="token punctuation">:</span><span class="token number">8080</span>
    <span class="token key atrule">links</span><span class="token punctuation">:</span>
      <span class="token punctuation">-</span> kafka
      <span class="token punctuation">-</span> repo
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,3),c=[i];function o(l,p){return s(),a("div",null,c)}const r=n(t,[["render",o],["__file","tibco.html.vue"]]);export{r as default};

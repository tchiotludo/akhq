import{_ as n,o as e,c as s,e as a}from"./app-kesz-tRh.js";const i={},t=a(`<h1 id="glue-schema-registry" tabindex="-1"><a class="header-anchor" href="#glue-schema-registry" aria-hidden="true">#</a> Glue schema registry</h1><p>Currently ,glue schema registry support is limited to only de-serialisation of avro/protobuf/json serialized messages. It can be configured as below.</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code>  <span class="token key atrule">akhq</span><span class="token punctuation">:</span>
    <span class="token key atrule">environment</span><span class="token punctuation">:</span>
      <span class="token key atrule">AKHQ_CONFIGURATION</span><span class="token punctuation">:</span> <span class="token punctuation">|</span><span class="token scalar string">
        akhq:
          connections:
            docker-kafka-server:
              properties:
                bootstrap.servers: &quot;kafka:9092&quot;
              schema-registry:
                url: &quot;http://schema-registry:8085&quot;
                type: &quot;glue&quot;
                glueSchemaRegistryName: Name of schema Registry
                awsRegion: aws region
              connect:
                - name: &quot;connect&quot;
                  url: &quot;http://connect:8083&quot;</span>
    <span class="token key atrule">ports</span><span class="token punctuation">:</span>
      <span class="token punctuation">-</span> 8080<span class="token punctuation">:</span><span class="token number">8080</span>
    <span class="token key atrule">links</span><span class="token punctuation">:</span>
      <span class="token punctuation">-</span> kafka
      <span class="token punctuation">-</span> repo
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Please note that authentication is done using aws default credentials provider.</p><p>Url key is required to not break the flow.</p>`,5),l=[t];function r(c,o){return e(),s("div",null,l)}const d=n(i,[["render",r],["__file","glue.html.vue"]]);export{d as default};

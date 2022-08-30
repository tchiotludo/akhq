import{_ as n,r as t,o as r,c,b as s,a,w as d,d as e,e as l}from"./app.8c288fc0.js";const u={},h=s("h1",{id:"avro-deserialization",tabindex:"-1"},[s("a",{class:"header-anchor",href:"#avro-deserialization","aria-hidden":"true"},"#"),e(" Avro deserialization")],-1),_=e("Avro messages using Schema registry are automatically decoded if the registry is configured (see "),v=e("Kafka cluster"),m=e(")."),p=e("You can also decode raw binary Avro messages, that is messages encoded directly with "),f={href:"https://avro.apache.org/docs/current/api/java/org/apache/avro/io/DatumWriter.html",target:"_blank",rel:"noopener noreferrer"},b=e("DatumWriter"),g=e(" without any header. You must provide a "),k=s("code",null,"schemas-folder",-1),q=e(" and mappings which associate a "),x=s("code",null,"topic-regex",-1),y=e(" and a schema file name. The schema can be specified either for message keys with "),w=s("code",null,"key-schema-file",-1),V=e(" and/or for values with "),A=s("code",null,"value-schema-file",-1),E=e("."),L=l(`<p>Here is an example of configuration:</p><div class="language-text ext-text line-numbers-mode"><pre class="language-text"><code>akhq:
  connections:
    kafka:
      properties:
        # standard kafka properties
        avro-raw:
          schemas-folder: &quot;/app/avro_schemas&quot;
          topics-mapping:
            - topic-regex: &quot;album.*&quot;
              value-schema-file: &quot;Album.avsc&quot;
            - topic-regex: &quot;film.*&quot;
              value-schema-file: &quot;Film.avsc&quot;
            - topic-regex: &quot;test.*&quot;
              key-schema-file: &quot;Key.avsc&quot;
              value-schema-file: &quot;Value.avsc&quot;
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,2),N=e("Examples can be found in "),z={href:"https://github.com/tchiotludo/akhq/tree/dev/src/main/java/org/akhq/utils",target:"_blank",rel:"noopener noreferrer"},B=e("tests"),j=e(".");function C(D,I){const i=t("RouterLink"),o=t("ExternalLinkIcon");return r(),c("div",null,[h,s("p",null,[_,a(i,{to:"/docs/configuration/brokers.html"},{default:d(()=>[v]),_:1}),m]),s("p",null,[p,s("a",f,[b,a(o)]),g,k,q,x,y,w,V,A,E]),L,s("p",null,[N,s("a",z,[B,a(o)]),j])])}const R=n(u,[["render",C],["__file","avro.html.vue"]]);export{R as default};

import{_ as n,c as a,b as e,o as t}from"./app-DR_w82Kd.js";const l={};function p(o,s){return t(),a("div",null,s[0]||(s[0]=[e(`<h1 id="cluster-configuration" tabindex="-1"><a class="header-anchor" href="#cluster-configuration"><span>Cluster configuration</span></a></h1><ul><li><code>akhq.connections</code> is a key value configuration with : <ul><li><code>key</code>: must be an url friendly (letter, number, _, -, ... dot are not allowed here) string to identify your cluster (<code>my-cluster-1</code> and <code>my-cluster-2</code> is the example above)</li><li><code>properties</code>: all the configurations found on <a href="https://kafka.apache.org/documentation/#consumerconfigs" target="_blank" rel="noopener noreferrer">Kafka consumer documentation</a>. Most important is <code>bootstrap.servers</code> that is a list of host:port of your Kafka brokers.</li><li><code>schema-registry</code>: <em>(optional)</em><ul><li><code>url</code>: the schema registry url</li><li><code>type</code>: the type of schema registry used, either &#39;confluent&#39; or &#39;tibco&#39;</li><li><code>basic-auth-username</code>: schema registry basic auth username</li><li><code>basic-auth-password</code>: schema registry basic auth password</li><li><code>properties</code>: all the configurations for registry client, especially ssl configuration</li></ul></li><li><code>connect</code>: <em>(optional list, define each connector as an element of a list)</em><ul><li><code>name</code>: connect name</li><li><code>url</code>: connect url</li><li><code>basic-auth-username</code>: connect basic auth username</li><li><code>basic-auth-password</code>: connect basic auth password</li><li><code>ssl-trust-store</code>: /app/truststore.jks</li><li><code>ssl-trust-store-password</code>: trust-store-password</li><li><code>ssl-key-store</code>: /app/truststore.jks</li><li><code>ssl-key-store-password</code>: key-store-password</li></ul></li><li><code>ksqldb</code>: <em>(optional list, define each ksqlDB instance as an element of a list)</em><ul><li><code>name</code>: ksqlDB name</li><li><code>url</code>: ksqlDB url</li><li><code>basic-auth-username</code>: ksqlDB basic auth username</li><li><code>basic-auth-password</code>: ksqlDB basic auth password</li></ul></li></ul></li></ul><h2 id="basic-cluster-with-plain-auth" tabindex="-1"><a class="header-anchor" href="#basic-cluster-with-plain-auth"><span>Basic cluster with plain auth</span></a></h2><div class="language-yaml line-numbers-mode" data-highlighter="prismjs" data-ext="yml" data-title="yml"><pre><code><span class="line"><span class="token key atrule">akhq</span><span class="token punctuation">:</span></span>
<span class="line">  <span class="token key atrule">connections</span><span class="token punctuation">:</span></span>
<span class="line">    <span class="token key atrule">local</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token key atrule">properties</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">bootstrap.servers</span><span class="token punctuation">:</span> <span class="token string">&quot;local:9092&quot;</span></span>
<span class="line">      <span class="token key atrule">schema-registry</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">url</span><span class="token punctuation">:</span> <span class="token string">&quot;http://schema-registry:8085&quot;</span></span>
<span class="line">      <span class="token key atrule">connect</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token punctuation">-</span> <span class="token key atrule">name</span><span class="token punctuation">:</span> <span class="token string">&quot;connect&quot;</span></span>
<span class="line">          <span class="token key atrule">url</span><span class="token punctuation">:</span> <span class="token string">&quot;http://connect:8083&quot;</span></span>
<span class="line">      <span class="token key atrule">ksqldb</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token punctuation">-</span> <span class="token key atrule">name</span><span class="token punctuation">:</span> <span class="token string">&quot;ksqldb&quot;</span></span>
<span class="line">          <span class="token key atrule">url</span><span class="token punctuation">:</span> <span class="token string">&quot;http://connect:8088&quot;</span></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="example-for-confluent-cloud" tabindex="-1"><a class="header-anchor" href="#example-for-confluent-cloud"><span>Example for Confluent Cloud</span></a></h2><div class="language-yaml line-numbers-mode" data-highlighter="prismjs" data-ext="yml" data-title="yml"><pre><code><span class="line"><span class="token key atrule">akhq</span><span class="token punctuation">:</span></span>
<span class="line">  <span class="token key atrule">connections</span><span class="token punctuation">:</span></span>
<span class="line">    <span class="token key atrule">ccloud</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token key atrule">properties</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">bootstrap.servers</span><span class="token punctuation">:</span> <span class="token string">&quot;{{ cluster }}.{{ region }}.{{ cloud }}.confluent.cloud:9092&quot;</span></span>
<span class="line">        <span class="token key atrule">security.protocol</span><span class="token punctuation">:</span> SASL_SSL</span>
<span class="line">        <span class="token key atrule">sasl.mechanism</span><span class="token punctuation">:</span> PLAIN</span>
<span class="line">        <span class="token key atrule">sasl.jaas.config</span><span class="token punctuation">:</span> org.apache.kafka.common.security.plain.PlainLoginModule required username=&quot;<span class="token punctuation">{</span><span class="token punctuation">{</span> kafkaUsername <span class="token punctuation">}</span><span class="token punctuation">}</span>&quot; password=&quot;<span class="token punctuation">{</span><span class="token punctuation">{</span> kafkaPassword <span class="token punctuation">}</span><span class="token punctuation">}</span>&quot;;</span>
<span class="line">      <span class="token key atrule">schema-registry</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">url</span><span class="token punctuation">:</span> <span class="token string">&quot;https://{{ cluster }}.{{ region }}.{{ cloud }}.confluent.cloud&quot;</span></span>
<span class="line">        <span class="token key atrule">basic-auth-username</span><span class="token punctuation">:</span> <span class="token string">&quot;{{ schemaRegistryUsername }}&quot;</span></span>
<span class="line">        <span class="token key atrule">basic-auth-password</span><span class="token punctuation">:</span> <span class="token string">&quot;{{ schemaRegistryPaswword }}&quot;</span></span>
<span class="line"></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="ssl-kafka-cluster" tabindex="-1"><a class="header-anchor" href="#ssl-kafka-cluster"><span>SSL Kafka Cluster</span></a></h2><p>Configuration example for kafka cluster secured by ssl for saas provider like aiven (full https &amp; basic auth):</p><p>You need to generate a jks &amp; p12 file from pem, cert files give by saas provider.</p><div class="language-bash line-numbers-mode" data-highlighter="prismjs" data-ext="sh" data-title="sh"><pre><code><span class="line">openssl pkcs12 <span class="token parameter variable">-export</span> <span class="token parameter variable">-inkey</span> service.key <span class="token parameter variable">-in</span> service.cert <span class="token parameter variable">-out</span> client.keystore.p12 <span class="token parameter variable">-name</span> service_key</span>
<span class="line">keytool <span class="token parameter variable">-import</span> <span class="token parameter variable">-file</span> ca.pem <span class="token parameter variable">-alias</span> CA <span class="token parameter variable">-keystore</span> client.truststore.jks</span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"><div class="line-number"></div><div class="line-number"></div></div></div><p>Configurations will look like this example:</p><div class="language-yaml line-numbers-mode" data-highlighter="prismjs" data-ext="yml" data-title="yml"><pre><code><span class="line"><span class="token key atrule">akhq</span><span class="token punctuation">:</span></span>
<span class="line">  <span class="token key atrule">connections</span><span class="token punctuation">:</span></span>
<span class="line">    <span class="token key atrule">ssl-dev</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token key atrule">properties</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">bootstrap.servers</span><span class="token punctuation">:</span> <span class="token string">&quot;{{host}}.aivencloud.com:12835&quot;</span></span>
<span class="line">        <span class="token key atrule">security.protocol</span><span class="token punctuation">:</span> SSL</span>
<span class="line">        <span class="token key atrule">ssl.truststore.location</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>path<span class="token punctuation">}</span><span class="token punctuation">}</span>/avnadmin.truststore.jks</span>
<span class="line">        <span class="token key atrule">ssl.truststore.password</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>password<span class="token punctuation">}</span><span class="token punctuation">}</span></span>
<span class="line">        <span class="token key atrule">ssl.keystore.type</span><span class="token punctuation">:</span> <span class="token string">&quot;PKCS12&quot;</span></span>
<span class="line">        <span class="token key atrule">ssl.keystore.location</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>path<span class="token punctuation">}</span><span class="token punctuation">}</span>/avnadmin.keystore.p12</span>
<span class="line">        <span class="token key atrule">ssl.keystore.password</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>password<span class="token punctuation">}</span><span class="token punctuation">}</span></span>
<span class="line">        <span class="token key atrule">ssl.key.password</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>password<span class="token punctuation">}</span><span class="token punctuation">}</span></span>
<span class="line">      <span class="token key atrule">schema-registry</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">url</span><span class="token punctuation">:</span> <span class="token string">&quot;https://{{host}}.aivencloud.com:12838&quot;</span></span>
<span class="line">        <span class="token key atrule">type</span><span class="token punctuation">:</span> <span class="token string">&quot;confluent&quot;</span></span>
<span class="line">        <span class="token key atrule">basic-auth-username</span><span class="token punctuation">:</span> avnadmin</span>
<span class="line">        <span class="token key atrule">basic-auth-password</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>password<span class="token punctuation">}</span><span class="token punctuation">}</span></span>
<span class="line">        <span class="token key atrule">properties</span><span class="token punctuation">:</span></span>
<span class="line">          <span class="token key atrule">schema.registry.ssl.truststore.location</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>path<span class="token punctuation">}</span><span class="token punctuation">}</span>/avnadmin.truststore.jks</span>
<span class="line">          <span class="token key atrule">schema.registry.ssl.truststore.password</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>password<span class="token punctuation">}</span><span class="token punctuation">}</span></span>
<span class="line">      <span class="token key atrule">connect</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token punctuation">-</span> <span class="token key atrule">name</span><span class="token punctuation">:</span> connect<span class="token punctuation">-</span><span class="token number">1</span></span>
<span class="line">          <span class="token key atrule">url</span><span class="token punctuation">:</span> <span class="token string">&quot;https://{{host}}.aivencloud.com:{{port}}&quot;</span></span>
<span class="line">          <span class="token key atrule">basic-auth-username</span><span class="token punctuation">:</span> avnadmin</span>
<span class="line">          <span class="token key atrule">basic-auth-password</span><span class="token punctuation">:</span> <span class="token punctuation">{</span><span class="token punctuation">{</span>password<span class="token punctuation">}</span><span class="token punctuation">}</span></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><h2 id="oauth2-authentification-for-brokers" tabindex="-1"><a class="header-anchor" href="#oauth2-authentification-for-brokers"><span>OAuth2 authentification for brokers</span></a></h2><p>Requirement Library Strimzi:</p><blockquote><p>The kafka brokers must be configured with the Strimzi library and an OAuth2 provider (Keycloak example).</p></blockquote><blockquote><p>This <a href="https://github.com/strimzi/strimzi-kafka-oauth" target="_blank" rel="noopener noreferrer">repository</a> contains documentation and examples.</p></blockquote><p>Configuration Bootstrap:</p><blockquote><p>It&#39;s not necessary to compile AKHQ to integrate the Strimzi libraries since the libs will be included on the final image !</p></blockquote><p>You must configure AKHQ through the application.yml file.</p><div class="language-yaml line-numbers-mode" data-highlighter="prismjs" data-ext="yml" data-title="yml"><pre><code><span class="line"><span class="token key atrule">akhq</span><span class="token punctuation">:</span></span>
<span class="line">  <span class="token key atrule">connections</span><span class="token punctuation">:</span></span>
<span class="line">    <span class="token key atrule">my-kafka-cluster</span><span class="token punctuation">:</span></span>
<span class="line">      <span class="token key atrule">properties</span><span class="token punctuation">:</span></span>
<span class="line">        <span class="token key atrule">bootstrap.servers</span><span class="token punctuation">:</span> <span class="token string">&quot;&lt;url broker kafka&gt;:9094,&lt;url broker kafka&gt;:9094&quot;</span></span>
<span class="line">        <span class="token key atrule">sasl.jaas.config</span><span class="token punctuation">:</span> org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required auth.valid.issuer.uri=&quot;https<span class="token punctuation">:</span>//&lt;url keycloak<span class="token punctuation">&gt;</span>/auth/realms/sandbox_kafka&quot; oauth.jwks.endpoint.uri=&quot;https<span class="token punctuation">:</span>/&lt;url keycloak<span class="token punctuation">&gt;</span>//auth/realms/sandbox_kafka/protocol/openid<span class="token punctuation">-</span>connect/certs&quot; oauth.username.claim=&quot;preferred_username&quot; oauth.client.id=&quot;kafka<span class="token punctuation">-</span>producer<span class="token punctuation">-</span>client&quot; oauth.client.secret=&quot;&quot; oauth.ssl.truststore.location=&quot;kafka.server.truststore.jks&quot; oauth.ssl.truststore.password=&quot;xxxxx&quot; oauth.ssl.truststore.type=&quot;jks&quot; oauth.ssl.endpoint_identification_algorithm=&quot;&quot; oauth.token.endpoint.uri=&quot;https<span class="token punctuation">:</span>///auth/realms/sandbox_kafka/protocol/openid<span class="token punctuation">-</span>connect/token&quot;;</span>
<span class="line">        <span class="token key atrule">sasl.login.callback.handler.class</span><span class="token punctuation">:</span> io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler</span>
<span class="line">        <span class="token key atrule">security.protocol</span><span class="token punctuation">:</span> SASL_PLAINTEXT</span>
<span class="line">        <span class="token key atrule">sasl.mechanism</span><span class="token punctuation">:</span> OAUTHBEARER</span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>I put oauth.ssl.endpoint_identification_algorithm = &quot;&quot; for testing or my certificates did not match the FQDN. In a production, you have to remove it.</p>`,21)]))}const c=n(l,[["render",p],["__file","brokers.html.vue"]]),u=JSON.parse('{"path":"/docs/configuration/brokers.html","title":"Cluster configuration","lang":"en-US","frontmatter":{},"headers":[{"level":2,"title":"Basic cluster with plain auth","slug":"basic-cluster-with-plain-auth","link":"#basic-cluster-with-plain-auth","children":[]},{"level":2,"title":"Example for Confluent Cloud","slug":"example-for-confluent-cloud","link":"#example-for-confluent-cloud","children":[]},{"level":2,"title":"SSL Kafka Cluster","slug":"ssl-kafka-cluster","link":"#ssl-kafka-cluster","children":[]},{"level":2,"title":"OAuth2 authentification for brokers","slug":"oauth2-authentification-for-brokers","link":"#oauth2-authentification-for-brokers","children":[]}],"git":{"updatedTime":1737760172000,"contributors":[{"name":"Ludovic DEHON","username":"Ludovic DEHON","email":"tchiot.ludo@gmail.com","commits":1,"url":"https://github.com/Ludovic DEHON"}]},"filePathRelative":"docs/configuration/brokers.md"}');export{c as comp,u as data};

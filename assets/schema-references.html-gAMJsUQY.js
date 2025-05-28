import{_ as n,c as a,b as e,o as p}from"./app-DQ7CE0Yb.js";const t={};function o(c,s){return p(),a("div",null,s[0]||(s[0]=[e(`<h1 id="schema-references" tabindex="-1"><a class="header-anchor" href="#schema-references"><span>Schema references</span></a></h1><p>Since Confluent 5.5.0, Avro schemas can now be reused by others schemas through schema references. This feature allows to define a schema once and use it as a record type inside one or more schemas.</p><p>When registering new Avro schemas with AKHQ UI, it is now possible to pass a slightly more complex object with a <code>schema</code> and a <code>references</code> field.</p><p>To register a new schema without references, no need to change anything:</p><div class="language-json line-numbers-mode" data-highlighter="prismjs" data-ext="json"><pre><code class="language-json"><span class="line"><span class="token punctuation">{</span></span>
<span class="line">    <span class="token property">&quot;name&quot;</span><span class="token operator">:</span> <span class="token string">&quot;Schema1&quot;</span><span class="token punctuation">,</span></span>
<span class="line">    <span class="token property">&quot;namespace&quot;</span><span class="token operator">:</span> <span class="token string">&quot;org.akhq&quot;</span><span class="token punctuation">,</span></span>
<span class="line">    <span class="token property">&quot;type&quot;</span><span class="token operator">:</span> <span class="token string">&quot;record&quot;</span><span class="token punctuation">,</span></span>
<span class="line">    <span class="token property">&quot;fields&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span></span>
<span class="line">        <span class="token punctuation">{</span></span>
<span class="line">            <span class="token property">&quot;name&quot;</span><span class="token operator">:</span> <span class="token string">&quot;description&quot;</span><span class="token punctuation">,</span></span>
<span class="line">            <span class="token property">&quot;type&quot;</span><span class="token operator">:</span> <span class="token string">&quot;string&quot;</span></span>
<span class="line">        <span class="token punctuation">}</span></span>
<span class="line">    <span class="token punctuation">]</span></span>
<span class="line"><span class="token punctuation">}</span></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"></div></div><p>To register a new schema with a reference to an already registered schema:</p><div class="language-json line-numbers-mode" data-highlighter="prismjs" data-ext="json"><pre><code class="language-json"><span class="line"><span class="token punctuation">{</span></span>
<span class="line">    <span class="token property">&quot;schema&quot;</span><span class="token operator">:</span> <span class="token punctuation">{</span></span>
<span class="line">        <span class="token property">&quot;name&quot;</span><span class="token operator">:</span> <span class="token string">&quot;Schema2&quot;</span><span class="token punctuation">,</span></span>
<span class="line">        <span class="token property">&quot;namespace&quot;</span><span class="token operator">:</span> <span class="token string">&quot;org.akhq&quot;</span><span class="token punctuation">,</span></span>
<span class="line">        <span class="token property">&quot;type&quot;</span><span class="token operator">:</span> <span class="token string">&quot;record&quot;</span><span class="token punctuation">,</span></span>
<span class="line">        <span class="token property">&quot;fields&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span></span>
<span class="line">            <span class="token punctuation">{</span></span>
<span class="line">                <span class="token property">&quot;name&quot;</span><span class="token operator">:</span> <span class="token string">&quot;name&quot;</span><span class="token punctuation">,</span></span>
<span class="line">                <span class="token property">&quot;type&quot;</span><span class="token operator">:</span> <span class="token string">&quot;string&quot;</span></span>
<span class="line">            <span class="token punctuation">}</span><span class="token punctuation">,</span></span>
<span class="line">            <span class="token punctuation">{</span></span>
<span class="line">                <span class="token property">&quot;name&quot;</span><span class="token operator">:</span> <span class="token string">&quot;schema1&quot;</span><span class="token punctuation">,</span></span>
<span class="line">                <span class="token property">&quot;type&quot;</span><span class="token operator">:</span> <span class="token string">&quot;Schema1&quot;</span></span>
<span class="line">            <span class="token punctuation">}</span></span>
<span class="line">        <span class="token punctuation">]</span></span>
<span class="line">    <span class="token punctuation">}</span><span class="token punctuation">,</span></span>
<span class="line">    <span class="token property">&quot;references&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span></span>
<span class="line">        <span class="token punctuation">{</span></span>
<span class="line">            <span class="token property">&quot;name&quot;</span><span class="token operator">:</span> <span class="token string">&quot;Schema1&quot;</span><span class="token punctuation">,</span></span>
<span class="line">            <span class="token property">&quot;subject&quot;</span><span class="token operator">:</span> <span class="token string">&quot;SCHEMA_1&quot;</span><span class="token punctuation">,</span></span>
<span class="line">            <span class="token property">&quot;version&quot;</span><span class="token operator">:</span> <span class="token number">1</span></span>
<span class="line">        <span class="token punctuation">}</span></span>
<span class="line">    <span class="token punctuation">]</span></span>
<span class="line"><span class="token punctuation">}</span></span>
<span class="line"></span></code></pre><div class="line-numbers" aria-hidden="true" style="counter-reset:line-number 0;"></div></div><p>Documentation on Confluent 5.5 and schema references can be found <a href="https://docs.confluent.io/5.5.0/schema-registry/serdes-develop/index.html" target="_blank" rel="noopener noreferrer">here</a>.</p>`,8)]))}const r=n(t,[["render",o]]),u=JSON.parse('{"path":"/docs/configuration/schema-registry/schema-references.html","title":"Schema references","lang":"en-US","frontmatter":{},"git":{"updatedTime":1748465880000,"contributors":[{"name":"Ludovic DEHON","username":"","email":"tchiot.ludo@gmail.com","commits":1}],"changelog":[{"hash":"c398be2a0aabb9c61bf5ee31d29d2cda885d9617","time":1748465880000,"email":"tchiot.ludo@gmail.com","author":"Ludovic DEHON","message":"chore(deps): update all site deps"}]},"filePathRelative":"docs/configuration/schema-registry/schema-references.md"}');export{r as comp,u as data};

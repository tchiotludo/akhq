import{_ as n,p as s,q as a,a1 as e}from"./framework-96b046e1.js";const t={},i=e(`<h1 id="oidc" tabindex="-1"><a class="header-anchor" href="#oidc" aria-hidden="true">#</a> OIDC</h1><p>To enable OIDC in the application, you&#39;ll first have to enable OIDC in micronaut:</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">micronaut</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">oauth2</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">clients</span><span class="token punctuation">:</span>
        <span class="token key atrule">google</span><span class="token punctuation">:</span>
          <span class="token key atrule">client-id</span><span class="token punctuation">:</span> <span class="token string">&quot;&lt;client-id&gt;&quot;</span>
          <span class="token key atrule">client-secret</span><span class="token punctuation">:</span> <span class="token string">&quot;&lt;client-secret&gt;&quot;</span>
          <span class="token key atrule">openid</span><span class="token punctuation">:</span>
            <span class="token key atrule">issuer</span><span class="token punctuation">:</span> <span class="token string">&quot;&lt;issuer-url&gt;&quot;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>To further tell AKHQ to display OIDC options on the login page and customize claim mapping, configure OIDC in the AKHQ config:</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">oidc</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">providers</span><span class="token punctuation">:</span>
        <span class="token key atrule">google</span><span class="token punctuation">:</span>
          <span class="token key atrule">label</span><span class="token punctuation">:</span> <span class="token string">&quot;Login with Google&quot;</span>
          <span class="token key atrule">username-field</span><span class="token punctuation">:</span> preferred_username
          <span class="token comment"># specifies the field name in the oidc claim containing the use assigned role (eg. in keycloak this would be the Token Claim Name you set in your Client Role Mapper)</span>
          <span class="token key atrule">groups-field</span><span class="token punctuation">:</span> roles
          <span class="token key atrule">default-group</span><span class="token punctuation">:</span> topic<span class="token punctuation">-</span>reader
          <span class="token key atrule">groups</span><span class="token punctuation">:</span>
            <span class="token comment"># the name of the user role set in your oidc provider and associated with your user (eg. in keycloak this would be a client role)</span>
            <span class="token punctuation">-</span> <span class="token key atrule">name</span><span class="token punctuation">:</span> mathematicians
              <span class="token key atrule">groups</span><span class="token punctuation">:</span>
                <span class="token comment"># the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)</span>
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>reader
            <span class="token punctuation">-</span> <span class="token key atrule">name</span><span class="token punctuation">:</span> scientists
              <span class="token key atrule">groups</span><span class="token punctuation">:</span>
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>reader
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>writer
          <span class="token key atrule">users</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token key atrule">username</span><span class="token punctuation">:</span> franz
              <span class="token key atrule">groups</span><span class="token punctuation">:</span>
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>reader
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>writer
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>The username field can be any string field, the roles field has to be a JSON array.</p><h2 id="direct-oidc-mapping" tabindex="-1"><a class="header-anchor" href="#direct-oidc-mapping" aria-hidden="true">#</a> Direct OIDC mapping</h2><p>If you want to manage AKHQ roles an attributes directly with the OIDC provider, you can use the following configuration:</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">oidc</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">providers</span><span class="token punctuation">:</span>
        <span class="token key atrule">google</span><span class="token punctuation">:</span>
          <span class="token key atrule">label</span><span class="token punctuation">:</span> <span class="token string">&quot;Login with Google&quot;</span>
          <span class="token key atrule">username-field</span><span class="token punctuation">:</span> preferred_username
          <span class="token key atrule">use-oidc-claim</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>In this scenario, you need to make the OIDC provider return a JWT which have the following fields:</p><div class="language-json line-numbers-mode" data-ext="json"><pre class="language-json"><code><span class="token punctuation">{</span>
  <span class="token comment">// Standard claims</span>
  <span class="token property">&quot;exp&quot;</span><span class="token operator">:</span> <span class="token number">1635868816</span><span class="token punctuation">,</span>
  <span class="token property">&quot;iat&quot;</span><span class="token operator">:</span> <span class="token number">1635868516</span><span class="token punctuation">,</span>
  <span class="token property">&quot;preferred_username&quot;</span><span class="token operator">:</span> <span class="token string">&quot;json&quot;</span><span class="token punctuation">,</span>
  ...
  <span class="token property">&quot;scope&quot;</span><span class="token operator">:</span> <span class="token string">&quot;openid email profile&quot;</span><span class="token punctuation">,</span>
  <span class="token comment">// Mandatory AKHQ claims</span>
  <span class="token property">&quot;roles&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
    <span class="token string">&quot;acls/read&quot;</span><span class="token punctuation">,</span>
    <span class="token string">&quot;topic/data/delete&quot;</span><span class="token punctuation">,</span>
    <span class="token string">&quot;topic/data/insert&quot;</span><span class="token punctuation">,</span>
    <span class="token string">&quot;...&quot;</span>
  <span class="token punctuation">]</span><span class="token punctuation">,</span>
  <span class="token comment">// Optional AKHQ claims</span>
  <span class="token comment">// If not set, no filtering is applied (full access &quot;.*&quot;)</span>
  <span class="token property">&quot;topicsFilterRegexp&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
    <span class="token string">&quot;^json.*$&quot;</span>
  <span class="token punctuation">]</span><span class="token punctuation">,</span>
  <span class="token property">&quot;connectsFilterRegexp&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
    <span class="token string">&quot;^json.*$&quot;</span>
  <span class="token punctuation">]</span><span class="token punctuation">,</span>
  <span class="token property">&quot;consumerGroupsFilterRegexp&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
    <span class="token string">&quot;^json-consumer.*$&quot;</span>
  <span class="token punctuation">]</span>
<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,11),p=[i];function l(o,c){return s(),a("div",null,p)}const r=n(t,[["render",l],["__file","oidc.html.vue"]]);export{r as default};

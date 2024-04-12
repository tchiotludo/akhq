import{_ as n,o as s,c as a,e}from"./app-1bAxfr7g.js";const t={},i=e(`<h1 id="github-sso-oauth2" tabindex="-1"><a class="header-anchor" href="#github-sso-oauth2" aria-hidden="true">#</a> GitHub SSO / OAuth2</h1><p>To enable GitHub SSO in the application, you&#39;ll first have to enable OAuth2 in micronaut:</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">micronaut</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
    <span class="token key atrule">oauth2</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">clients</span><span class="token punctuation">:</span>
        <span class="token key atrule">github</span><span class="token punctuation">:</span>
          <span class="token key atrule">client-id</span><span class="token punctuation">:</span> <span class="token string">&quot;&lt;client-id&gt;&quot;</span>
          <span class="token key atrule">client-secret</span><span class="token punctuation">:</span> <span class="token string">&quot;&lt;client-secret&gt;&quot;</span>
          <span class="token key atrule">scopes</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> user<span class="token punctuation">:</span>email
            <span class="token punctuation">-</span> read<span class="token punctuation">:</span>user
          <span class="token key atrule">authorization</span><span class="token punctuation">:</span>
            <span class="token key atrule">url</span><span class="token punctuation">:</span> https<span class="token punctuation">:</span>//github.com/login/oauth/authorize
          <span class="token key atrule">token</span><span class="token punctuation">:</span>
            <span class="token key atrule">url</span><span class="token punctuation">:</span> https<span class="token punctuation">:</span>//github.com/login/oauth/access_token
            <span class="token key atrule">auth-method</span><span class="token punctuation">:</span> client<span class="token punctuation">-</span>secret<span class="token punctuation">-</span>post
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>To further tell AKHQ to display GitHub SSO options on the login page and customize claim mapping, configure Oauth in the AKHQ config:</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">default-group</span><span class="token punctuation">:</span> no<span class="token punctuation">-</span>roles
    <span class="token key atrule">oauth2</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">providers</span><span class="token punctuation">:</span>
        <span class="token key atrule">github</span><span class="token punctuation">:</span>
          <span class="token key atrule">label</span><span class="token punctuation">:</span> <span class="token string">&quot;Login with GitHub&quot;</span>
          <span class="token key atrule">username-field</span><span class="token punctuation">:</span> login
          <span class="token key atrule">users</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token key atrule">username</span><span class="token punctuation">:</span> franz
              <span class="token key atrule">groups</span><span class="token punctuation">:</span>
                <span class="token comment"># the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)</span>
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>reader
                <span class="token punctuation">-</span> topic<span class="token punctuation">-</span>writer
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>The username field can be any string field, the roles field has to be a JSON array.</p><h2 id="references" tabindex="-1"><a class="header-anchor" href="#references" aria-hidden="true">#</a> References</h2><p>https://micronaut-projects.github.io/micronaut-security/latest/guide/#oauth2-configuration</p>`,8),l=[i];function p(u,o){return s(),a("div",null,l)}const r=n(t,[["render",p],["__file","github.html.vue"]]);export{r as default};

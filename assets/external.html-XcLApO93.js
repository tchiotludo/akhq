import{_ as n,o as s,c as a,e}from"./app-okElFVOy.js";const t={},p=e(`<h1 id="external-roles-and-attributes-mapping" tabindex="-1"><a class="header-anchor" href="#external-roles-and-attributes-mapping" aria-hidden="true">#</a> External roles and attributes mapping</h1><p>If you managed which topics (or any other resource) in an external system, you have access to 2 more implementations mechanisms to map your authenticated user (from either Local, Header, LDAP or OIDC) into AKHQ roles and attributes:</p><p>If you use this mechanism, keep in mind it will take the local user&#39;s groups for local Auth, and the external groups for Header/LDAP/OIDC (ie. this will NOT do the mapping between Header/LDAP/OIDC and local groups)</p><p><strong>Default configuration-based</strong> This is the current implementation and the default one (doesn&#39;t break compatibility)</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">default-group</span><span class="token punctuation">:</span> no<span class="token punctuation">-</span>roles
    <span class="token key atrule">groups</span><span class="token punctuation">:</span>
      <span class="token key atrule">reader</span><span class="token punctuation">:</span>
        <span class="token key atrule">roles</span><span class="token punctuation">:</span>
          <span class="token punctuation">-</span> topic/read
        <span class="token key atrule">attributes</span><span class="token punctuation">:</span>
          <span class="token key atrule">topics-filter-regexp</span><span class="token punctuation">:</span> <span class="token punctuation">[</span><span class="token string">&quot;.*&quot;</span><span class="token punctuation">]</span>
      <span class="token key atrule">no-roles</span><span class="token punctuation">:</span>
        <span class="token key atrule">roles</span><span class="token punctuation">:</span> <span class="token punctuation">[</span><span class="token punctuation">]</span>
    <span class="token key atrule">ldap</span><span class="token punctuation">:</span> <span class="token comment"># LDAP users/groups to AKHQ groups mapping</span>
    <span class="token key atrule">oidc</span><span class="token punctuation">:</span> <span class="token comment"># OIDC users/groups to AKHQ groups mapping</span>
    <span class="token key atrule">header-auth</span><span class="token punctuation">:</span> <span class="token comment"># header authentication users/groups to AKHQ groups mapping</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p><strong>REST API</strong></p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">default-group</span><span class="token punctuation">:</span> no<span class="token punctuation">-</span>roles
    <span class="token key atrule">rest</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">url</span><span class="token punctuation">:</span> https<span class="token punctuation">:</span>//external.service/get<span class="token punctuation">-</span>roles<span class="token punctuation">-</span>and<span class="token punctuation">-</span>attributes
    <span class="token key atrule">groups</span><span class="token punctuation">:</span> <span class="token comment"># anything set here will not be used</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>In this mode, AKHQ will send to the <code>akhq.security.rest.url</code> endpoint a POST request with the following JSON :</p><div class="language-json line-numbers-mode" data-ext="json"><pre class="language-json"><code><span class="token punctuation">{</span>
  <span class="token property">&quot;providerType&quot;</span><span class="token operator">:</span> <span class="token string">&quot;LDAP or OIDC or BASIC_AUTH or HEADER&quot;</span><span class="token punctuation">,</span>
  <span class="token property">&quot;providerName&quot;</span><span class="token operator">:</span> <span class="token string">&quot;OIDC provider name (OIDC only)&quot;</span><span class="token punctuation">,</span>
  <span class="token property">&quot;username&quot;</span><span class="token operator">:</span> <span class="token string">&quot;user&quot;</span><span class="token punctuation">,</span>
  <span class="token property">&quot;groups&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span><span class="token string">&quot;LDAP-GROUP-1&quot;</span><span class="token punctuation">,</span> <span class="token string">&quot;LDAP-GROUP-2&quot;</span><span class="token punctuation">,</span> <span class="token string">&quot;LDAP-GROUP-3&quot;</span><span class="token punctuation">]</span>
<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>and expect the following JSON as response :</p><div class="language-json line-numbers-mode" data-ext="json"><pre class="language-json"><code><span class="token punctuation">{</span>
  <span class="token property">&quot;groups&quot;</span><span class="token operator">:</span> <span class="token punctuation">{</span>
    <span class="token property">&quot;topic-writer-clusterA-projectA&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
      <span class="token punctuation">{</span>
        <span class="token property">&quot;role&quot;</span><span class="token operator">:</span> <span class="token string">&quot;topic-reader&quot;</span><span class="token punctuation">,</span>
        <span class="token property">&quot;patterns&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
          <span class="token string">&quot;pub.*&quot;</span>
        <span class="token punctuation">]</span>
      <span class="token punctuation">}</span><span class="token punctuation">,</span> <span class="token punctuation">{</span>
        <span class="token property">&quot;role&quot;</span><span class="token operator">:</span> <span class="token string">&quot;topic-writer&quot;</span><span class="token punctuation">,</span>
        <span class="token property">&quot;patterns&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
          <span class="token string">&quot;projectA.*&quot;</span>
        <span class="token punctuation">]</span><span class="token punctuation">,</span>
        <span class="token property">&quot;clusters&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
          <span class="token string">&quot;clusterA.*&quot;</span>
        <span class="token punctuation">]</span>
      <span class="token punctuation">}</span>
    <span class="token punctuation">]</span><span class="token punctuation">,</span>
    <span class="token property">&quot;acl-reader-clusterA&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
      <span class="token punctuation">{</span>
        <span class="token property">&quot;role&quot;</span><span class="token operator">:</span> <span class="token string">&quot;acl-reader&quot;</span><span class="token punctuation">,</span>
        <span class="token property">&quot;clusters&quot;</span><span class="token operator">:</span> <span class="token punctuation">[</span>
          <span class="token string">&quot;clusterA.*&quot;</span>
        <span class="token punctuation">]</span>
      <span class="token punctuation">}</span>
    <span class="token punctuation">]</span>
  <span class="token punctuation">}</span>
<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><div class="custom-container warning"><p class="custom-container-title">WARNING</p><p>The response must contain the <code>Content-Type: application/json</code> header to prevent any issue when reading the response.</p></div><p><strong>Groovy API</strong></p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code><span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">default-group</span><span class="token punctuation">:</span> no<span class="token punctuation">-</span>roles
    <span class="token key atrule">groovy</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">file</span><span class="token punctuation">:</span> <span class="token punctuation">|</span><span class="token scalar string">
        package org.akhq.utils;
        class GroovyCustomClaimProvider implements ClaimProvider {
            @Override
            ClaimResponse generateClaim(ClaimRequest request) {
                ClaimResponse response = ClaimResponse.builder().build()
                response.roles = [&quot;topic/read&quot;]
                response.topicsFilterRegexp: [&quot;.*&quot;]
                response.connectsFilterRegexp: [&quot;.*&quot;]
                response.consumerGroupsFilterRegexp: [&quot;.*&quot;]
                return response
            }
        }</span>
    <span class="token key atrule">groups</span><span class="token punctuation">:</span> <span class="token comment"># anything set here will not be used</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p><code>akhq.security.groovy.file</code> must be a groovy class that implements the interface ClaimProvider :</p><div class="language-java line-numbers-mode" data-ext="java"><pre class="language-java"><code><span class="token keyword">package</span> <span class="token namespace">org<span class="token punctuation">.</span>akhq<span class="token punctuation">.</span>utils</span><span class="token punctuation">;</span>
<span class="token keyword">public</span> <span class="token keyword">interface</span> <span class="token class-name">ClaimProvider</span> <span class="token punctuation">{</span>
    <span class="token class-name">ClaimResponse</span> <span class="token function">generateClaim</span><span class="token punctuation">(</span><span class="token class-name">ClaimRequest</span> request<span class="token punctuation">)</span><span class="token punctuation">;</span>
<span class="token punctuation">}</span>

<span class="token keyword">enum</span> <span class="token class-name">ClaimProviderType</span> <span class="token punctuation">{</span>
  <span class="token constant">BASIC_AUTH</span><span class="token punctuation">,</span>
  <span class="token constant">LDAP</span><span class="token punctuation">,</span>
  <span class="token constant">OIDC</span>
<span class="token punctuation">}</span>

<span class="token keyword">public</span> <span class="token keyword">class</span> <span class="token class-name">ClaimRequest</span> <span class="token punctuation">{</span>
  <span class="token class-name">ClaimProvider<span class="token punctuation">.</span>ProviderType</span> providerType<span class="token punctuation">;</span>
  <span class="token class-name">String</span> providerName<span class="token punctuation">;</span>
  <span class="token class-name">String</span> username<span class="token punctuation">;</span>
  <span class="token class-name">List</span><span class="token generics"><span class="token punctuation">&lt;</span><span class="token class-name">String</span><span class="token punctuation">&gt;</span></span> groups<span class="token punctuation">;</span>
<span class="token punctuation">}</span>

<span class="token keyword">public</span> <span class="token keyword">class</span> <span class="token class-name">ClaimResponse</span> <span class="token punctuation">{</span>
    <span class="token keyword">private</span> <span class="token class-name">List</span><span class="token generics"><span class="token punctuation">&lt;</span><span class="token class-name">String</span><span class="token punctuation">&gt;</span></span> roles<span class="token punctuation">;</span>
    <span class="token keyword">private</span> <span class="token class-name">List</span><span class="token generics"><span class="token punctuation">&lt;</span><span class="token class-name">String</span><span class="token punctuation">&gt;</span></span> topicsFilterRegexp<span class="token punctuation">;</span>
    <span class="token keyword">private</span> <span class="token class-name">List</span><span class="token generics"><span class="token punctuation">&lt;</span><span class="token class-name">String</span><span class="token punctuation">&gt;</span></span> connectsFilterRegexp<span class="token punctuation">;</span>
    <span class="token keyword">private</span> <span class="token class-name">List</span><span class="token generics"><span class="token punctuation">&lt;</span><span class="token class-name">String</span><span class="token punctuation">&gt;</span></span> consumerGroupsFilterRegexp<span class="token punctuation">;</span>
<span class="token punctuation">}</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,16),o=[p];function l(i,c){return s(),a("div",null,o)}const r=n(t,[["render",l],["__file","external.html.vue"]]);export{r as default};

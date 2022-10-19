import{_ as t,r as p,o as l,c as i,b as n,d as s,a as o,e as a}from"./app.2ca30807.js";const c={},u=a('<h1 id="ldap" tabindex="-1"><a class="header-anchor" href="#ldap" aria-hidden="true">#</a> LDAP</h1><p>Configure how the ldap groups will be matched in AKHQ groups</p><ul><li><code>akhq.security.ldap.groups</code>: Ldap groups list <ul><li><code>- name: ldap-group-name</code>: Ldap group name (same name as in ldap) <ul><li><code>groups</code>: AKHQ group list to be used for current ldap group</li></ul></li></ul></li></ul>',3),r={href:"https://www.forumsys.com/tutorials/integration-how-to/ldap/online-ldap-test-server/",target:"_blank",rel:"noopener noreferrer"},d=a(`<p>Configure ldap connection in micronaut</p><div class="language-yaml ext-yml line-numbers-mode"><pre class="language-yaml"><code><span class="token key atrule">micronaut</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
    <span class="token key atrule">ldap</span><span class="token punctuation">:</span>
      <span class="token key atrule">default</span><span class="token punctuation">:</span>
        <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
        <span class="token key atrule">context</span><span class="token punctuation">:</span>
          <span class="token key atrule">server</span><span class="token punctuation">:</span> <span class="token string">&#39;ldap://ldap.forumsys.com:389&#39;</span>
          <span class="token key atrule">managerDn</span><span class="token punctuation">:</span> <span class="token string">&#39;cn=read-only-admin,dc=example,dc=com&#39;</span>
          <span class="token key atrule">managerPassword</span><span class="token punctuation">:</span> <span class="token string">&#39;password&#39;</span>
        <span class="token key atrule">search</span><span class="token punctuation">:</span>
          <span class="token key atrule">base</span><span class="token punctuation">:</span> <span class="token string">&quot;dc=example,dc=com&quot;</span>
        <span class="token key atrule">groups</span><span class="token punctuation">:</span>
          <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
          <span class="token key atrule">base</span><span class="token punctuation">:</span> <span class="token string">&quot;dc=example,dc=com&quot;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>If you want to enable anonymous auth to your LDAP server you can pass :</p><div class="language-yaml ext-yml line-numbers-mode"><pre class="language-yaml"><code><span class="token key atrule">managerDn</span><span class="token punctuation">:</span> <span class="token string">&#39;&#39;</span>
<span class="token key atrule">managerPassword</span><span class="token punctuation">:</span> <span class="token string">&#39;&#39;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div><p>In Case your LDAP groups do not use the default UID for group membership, you can solve this using</p><div class="language-yaml ext-yml line-numbers-mode"><pre class="language-yaml"><code><span class="token key atrule">micronaut</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
    <span class="token key atrule">ldap</span><span class="token punctuation">:</span>
      <span class="token key atrule">default</span><span class="token punctuation">:</span>
        <span class="token key atrule">search</span><span class="token punctuation">:</span>
          <span class="token key atrule">base</span><span class="token punctuation">:</span> <span class="token string">&quot;OU=UserOU,dc=example,dc=com&quot;</span>
          <span class="token key atrule">attributes</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;cn&quot;</span>
        <span class="token key atrule">groups</span><span class="token punctuation">:</span>
          <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
          <span class="token key atrule">base</span><span class="token punctuation">:</span> <span class="token string">&quot;OU=GroupsOU,dc=example,dc=com&quot;</span>
          <span class="token key atrule">filter</span><span class="token punctuation">:</span> <span class="token string">&quot;member={0}&quot;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div><p>Replace</p><div class="language-yaml ext-yml line-numbers-mode"><pre class="language-yaml"><code><span class="token key atrule">attributes</span><span class="token punctuation">:</span>
  <span class="token punctuation">-</span> <span class="token string">&quot;cn&quot;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div></div></div><p>with your group membership attribute</p><p>Configure AKHQ groups and Ldap groups and users</p><div class="language-yaml ext-yml line-numbers-mode"><pre class="language-yaml"><code><span class="token key atrule">micronaut</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
<span class="token key atrule">akhq</span><span class="token punctuation">:</span>
  <span class="token key atrule">security</span><span class="token punctuation">:</span>
    <span class="token key atrule">groups</span><span class="token punctuation">:</span>
      <span class="token key atrule">topic-reader</span><span class="token punctuation">:</span>
        <span class="token key atrule">name</span><span class="token punctuation">:</span> topic<span class="token punctuation">-</span>reader <span class="token comment"># Group name</span>
        <span class="token key atrule">roles</span><span class="token punctuation">:</span>  <span class="token comment"># roles for the group</span>
          <span class="token punctuation">-</span> topic/read
        <span class="token key atrule">attributes</span><span class="token punctuation">:</span>
          <span class="token comment"># List of Regexp to filter topic available for group</span>
          <span class="token comment"># Single line String also allowed</span>
          <span class="token comment"># topics-filter-regexp: &quot;^(projectA_topic|projectB_.*)$&quot;</span>
          <span class="token key atrule">topics-filter-regexp</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;^projectA_topic$&quot;</span> <span class="token comment"># Individual topic</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;^projectB_.*$&quot;</span> <span class="token comment"># Topic group</span>
          <span class="token key atrule">connects-filter-regexp</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;^test.*$&quot;</span>
          <span class="token key atrule">consumer-groups-filter-regexp</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;consumer.*&quot;</span>
      <span class="token key atrule">topic-writer</span><span class="token punctuation">:</span>
        <span class="token key atrule">name</span><span class="token punctuation">:</span> topic<span class="token punctuation">-</span>writer <span class="token comment"># Group name</span>
        <span class="token key atrule">roles</span><span class="token punctuation">:</span>
          <span class="token punctuation">-</span> topic/read
          <span class="token punctuation">-</span> topic/insert
          <span class="token punctuation">-</span> topic/delete
          <span class="token punctuation">-</span> topic/config/update
        <span class="token key atrule">attributes</span><span class="token punctuation">:</span>
          <span class="token key atrule">topics-filter-regexp</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;test.*&quot;</span>
          <span class="token key atrule">connects-filter-regexp</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;^test.*$&quot;</span>
          <span class="token key atrule">consumer-groups-filter-regexp</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> <span class="token string">&quot;consumer.*&quot;</span>
    <span class="token key atrule">ldap</span><span class="token punctuation">:</span>
      <span class="token key atrule">groups</span><span class="token punctuation">:</span>
        <span class="token punctuation">-</span> <span class="token key atrule">name</span><span class="token punctuation">:</span> mathematicians
          <span class="token key atrule">groups</span><span class="token punctuation">:</span>
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

</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,11);function k(v,m){const e=p("ExternalLinkIcon");return l(),i("div",null,[u,n("p",null,[s("Example using "),n("a",r,[s("online ldap test server"),o(e)])]),d])}const y=t(c,[["render",k],["__file","ldap.html.vue"]]);export{y as default};

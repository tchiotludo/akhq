import{_ as n,o as s,c as a,e}from"./app-u8MfXpsE.js";const t={},l=e(`<h1 id="helm" tabindex="-1"><a class="header-anchor" href="#helm" aria-hidden="true">#</a> Helm</h1><p>Basically to create your helm values, you can take a look to the default values and you can see how your values could be defined: https://github.com/tchiotludo/akhq/blob/dev/helm/akhq/values.yaml</p><p>Nextone we will present some helm chart value example used in an AWS MSK that maybe could show how to use and define stuff in the helm chart and understand better how to define that.</p><h2 id="examples" tabindex="-1"><a class="header-anchor" href="#examples" aria-hidden="true">#</a> Examples</h2><h3 id="aws-msk-with-basic-authentication-and-alb-controller-ingress" tabindex="-1"><a class="header-anchor" href="#aws-msk-with-basic-authentication-and-alb-controller-ingress" aria-hidden="true">#</a> AWS MSK with Basic Authentication and ALB controller ingress</h3><p>The following HELM chart is an example of AWS MSK with a basic authentication and also using AWS load balancer controller.</p><p>So mixing the default values.yaml previously linked and adding the basic idea of basic AKHQ authentication (more info here: https://akhq.io/docs/configuration/authentifications/basic-auth.html) and the documentation about how to connect to the AWS MSK here https://akhq.io/docs/configuration/authentifications/aws-iam-auth.html, we created the following example.</p><p>And of course, about <code>ingress</code> and <code>service</code> is using similar Helm configurations like other external helm charts are using in the opensource community.</p><p>Also, if you need to add more stuff like ACL defintions, LDAP integrations or other stuff. In the main documentation there are present a lot of examples https://akhq.io/docs/ .</p><div class="language-yaml line-numbers-mode" data-ext="yml"><pre class="language-yaml"><code>
<span class="token comment"># This is an example with basic auth and a AWS MSK and using a AWS loadbalancer controller ingress</span>

<span class="token key atrule">configuration</span><span class="token punctuation">:</span>
  <span class="token key atrule">micronaut</span><span class="token punctuation">:</span>
    <span class="token key atrule">security</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">default-group</span><span class="token punctuation">:</span> no<span class="token punctuation">-</span>roles
      <span class="token key atrule">token</span><span class="token punctuation">:</span>
      <span class="token key atrule">jwt</span><span class="token punctuation">:</span>
        <span class="token key atrule">signatures</span><span class="token punctuation">:</span>
          <span class="token key atrule">secret</span><span class="token punctuation">:</span>
            <span class="token key atrule">generator</span><span class="token punctuation">:</span>
              <span class="token key atrule">secret</span><span class="token punctuation">:</span> changeme
  <span class="token key atrule">akhq</span><span class="token punctuation">:</span>
    <span class="token key atrule">security</span><span class="token punctuation">:</span>
      <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
      <span class="token key atrule">default-group</span><span class="token punctuation">:</span> no<span class="token punctuation">-</span>roles        
      <span class="token key atrule">basic-auth</span><span class="token punctuation">:</span>
        <span class="token punctuation">-</span> <span class="token key atrule">username</span><span class="token punctuation">:</span> changeme
          <span class="token key atrule">password</span><span class="token punctuation">:</span> changeme
          <span class="token key atrule">groups</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> admin
        <span class="token punctuation">-</span> <span class="token key atrule">username</span><span class="token punctuation">:</span> changeme
          <span class="token key atrule">password</span><span class="token punctuation">:</span> changeme
          <span class="token key atrule">groups</span><span class="token punctuation">:</span>
            <span class="token punctuation">-</span> reader
    <span class="token key atrule">server</span><span class="token punctuation">:</span>
      <span class="token key atrule">access-log</span><span class="token punctuation">:</span>
        <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
        <span class="token key atrule">name</span><span class="token punctuation">:</span> org.akhq.log.access
    <span class="token key atrule">connections</span><span class="token punctuation">:</span>
      <span class="token key atrule">my-cluster-sasl</span><span class="token punctuation">:</span>
        <span class="token key atrule">properties</span><span class="token punctuation">:</span>
          <span class="token key atrule">bootstrap.servers</span><span class="token punctuation">:</span> &lt;your bootsrapservers<span class="token punctuation">:</span>9096<span class="token punctuation">&gt;</span>
          <span class="token key atrule">security.protocol</span><span class="token punctuation">:</span> SASL_SSL
          <span class="token key atrule">sasl.mechanism</span><span class="token punctuation">:</span> SCRAM<span class="token punctuation">-</span>SHA<span class="token punctuation">-</span><span class="token number">512</span>
          <span class="token key atrule">sasl.jaas.config</span><span class="token punctuation">:</span> org.apache.kafka.common.security.scram.ScramLoginModule required username=&quot;username&quot; password=&quot;password&quot;;

<span class="token key atrule">ingress</span><span class="token punctuation">:</span>
  <span class="token key atrule">enabled</span><span class="token punctuation">:</span> <span class="token boolean important">true</span>
  <span class="token key atrule">portnumber</span><span class="token punctuation">:</span> <span class="token number">8080</span>
  <span class="token key atrule">apiVersion</span><span class="token punctuation">:</span> networking.k8s.io/v1
  <span class="token key atrule">annotations</span><span class="token punctuation">:</span>
    <span class="token key atrule">kubernetes.io/ingress.class</span><span class="token punctuation">:</span> <span class="token string">&#39;alb&#39;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/group.name</span><span class="token punctuation">:</span> <span class="token string">&quot;akhq&quot;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/scheme</span><span class="token punctuation">:</span> internal
    <span class="token key atrule">alb.ingress.kubernetes.io/target-type</span><span class="token punctuation">:</span> ip
    <span class="token key atrule">alb.ingress.kubernetes.io/listen-ports</span><span class="token punctuation">:</span> <span class="token string">&#39;[{&quot;HTTPS&quot;:443},{&quot;HTTPS&quot;:80}]&#39;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/load-balancer-attributes</span><span class="token punctuation">:</span> <span class="token string">&#39;routing.http2.enabled=true,idle_timeout.timeout_seconds=60&#39;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/healthcheck-path</span><span class="token punctuation">:</span> <span class="token string">&quot;/api/me&quot;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/subnets</span><span class="token punctuation">:</span> &lt;your_subnets<span class="token punctuation">&gt;</span>
    <span class="token key atrule">external-dns.alpha.kubernetes.io/hostname</span><span class="token punctuation">:</span> <span class="token string">&quot;akhq.domain&quot;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/certificate-arn</span><span class="token punctuation">:</span> <span class="token string">&quot;your_acm_here&quot;</span>
    <span class="token key atrule">alb.ingress.kubernetes.io/ssl-policy</span><span class="token punctuation">:</span> <span class="token string">&quot;ELBSecurityPolicy-TLS-1-2-2017-01&quot;</span>
    <span class="token key atrule">service.beta.kubernetes.io/aws-load-balancer-backend-protocol</span><span class="token punctuation">:</span> <span class="token string">&quot;tls&quot;</span>
    <span class="token key atrule">service.beta.kubernetes.io/aws-load-balancer-ssl-ports</span><span class="token punctuation">:</span> <span class="token string">&quot;443,80&quot;</span>
    <span class="token key atrule">service.beta.kubernetes.io/aws-load-balancer-ssl-negotiation-policy</span><span class="token punctuation">:</span> <span class="token string">&quot;ELBSecurityPolicy-TLS-1-2-2017-01&quot;</span>
  <span class="token key atrule">labels</span><span class="token punctuation">:</span>
    <span class="token key atrule">app</span><span class="token punctuation">:</span> akhq
  <span class="token key atrule">service</span><span class="token punctuation">:</span>
    <span class="token key atrule">port</span><span class="token punctuation">:</span> <span class="token number">443</span>
    <span class="token key atrule">annotations</span><span class="token punctuation">:</span>
      <span class="token key atrule">service.beta.kubernetes.io/target-type</span><span class="token punctuation">:</span> <span class="token string">&quot;ip&quot;</span>
  <span class="token key atrule">hosts</span><span class="token punctuation">:</span> <span class="token punctuation">[</span> <span class="token string">&#39;akhq.domain&#39;</span> <span class="token punctuation">]</span>
  <span class="token key atrule">paths</span><span class="token punctuation">:</span> <span class="token punctuation">[</span> <span class="token string">&quot;/*&quot;</span> <span class="token punctuation">]</span>
  <span class="token key atrule">tls</span><span class="token punctuation">:</span>
    <span class="token punctuation">-</span> <span class="token key atrule">secretName</span><span class="token punctuation">:</span> tls<span class="token punctuation">-</span>credential
      <span class="token key atrule">hosts</span><span class="token punctuation">:</span>
       <span class="token punctuation">-</span> <span class="token string">&#39;akhq.domain&#39;</span>
</code></pre><div class="line-numbers" aria-hidden="true"><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div><div class="line-number"></div></div></div>`,10),i=[l];function o(p,c){return s(),a("div",null,i)}const r=n(t,[["render",o],["__file","helm.html.vue"]]);export{r as default};

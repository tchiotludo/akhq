# Helm  

Basically to create your helm values, you can take a look to the default values and you can see how your values could be defined:
https://github.com/tchiotludo/akhq/blob/dev/helm/akhq/values.yaml

Nextone we will present some helm chart value example used in an AWS MSK that maybe could show how to use and define stuff in the helm chart and understand better how to define that. 


## Examples

### AWS MSK with Basic Authentication and ALB controller ingress

The following HELM chart is an example of AWS MSK with a basic authentication and also using AWS load balancer controller.

So mixing the default values.yaml previously linked and adding the basic idea of basic AKHQ authentication (more info here: https://akhq.io/docs/configuration/authentifications/basic-auth.html) and the documentation about how to connect to the AWS MSK here https://akhq.io/docs/configuration/authentifications/aws-iam-auth.html, we created the following example.

And of course, about `ingress` and `service` is using similar Helm configurations like other external helm charts are using in the opensource community.

Also, if you need to add more stuff like ACL defintions, LDAP integrations or other stuff. In the main documentation there are present a lot of examples https://akhq.io/docs/ .

```yaml

# This is an example with basic auth and a AWS MSK and using a AWS loadbalancer controller ingress

configuration:
  micronaut:
    security:
      enabled: true
      default-group: no-roles
      token:
      jwt:
        signatures:
          secret:
            generator:
              secret: changeme
  akhq:
    security:
      enabled: true
      default-group: no-roles        
      basic-auth:
        - username: changeme
          password: changeme
          groups:
            - admin
        - username: changeme
          password: changeme
          groups:
            - reader
    server:
      access-log:
        enabled: true
        name: org.akhq.log.access
    connections:
      my-cluster-sasl:
        properties:
          bootstrap.servers: <your bootsrapservers:9096>
          security.protocol: SASL_SSL
          sasl.mechanism: SCRAM-SHA-512
          sasl.jaas.config: org.apache.kafka.common.security.scram.ScramLoginModule required username="username" password="password";

ingress:
  enabled: true
  portnumber: 8080
  apiVersion: networking.k8s.io/v1
  annotations:
    kubernetes.io/ingress.class: 'alb'
    alb.ingress.kubernetes.io/group.name: "akhq"
    alb.ingress.kubernetes.io/scheme: internal
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443},{"HTTPS":80}]'
    alb.ingress.kubernetes.io/load-balancer-attributes: 'routing.http2.enabled=true,idle_timeout.timeout_seconds=60'
    alb.ingress.kubernetes.io/healthcheck-path: "/api/me"
    alb.ingress.kubernetes.io/subnets: <your_subnets>
    external-dns.alpha.kubernetes.io/hostname: "akhq.domain"
    alb.ingress.kubernetes.io/certificate-arn: "your_acm_here"
    alb.ingress.kubernetes.io/ssl-policy: "ELBSecurityPolicy-TLS-1-2-2017-01"
    service.beta.kubernetes.io/aws-load-balancer-backend-protocol: "tls"
    service.beta.kubernetes.io/aws-load-balancer-ssl-ports: "443,80"
    service.beta.kubernetes.io/aws-load-balancer-ssl-negotiation-policy: "ELBSecurityPolicy-TLS-1-2-2017-01"
  labels:
    app: akhq
  service:
    port: 443
    annotations:
      service.beta.kubernetes.io/target-type: "ip"
  hosts: [ 'akhq.domain' ]
  paths: [ "/*" ]
  tls:
    - secretName: tls-credential
      hosts:
       - 'akhq.domain'
```

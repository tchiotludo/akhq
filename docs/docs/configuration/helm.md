# Helm  

Taking a look to the default values you can see how your values could be defined:
https://github.com/tchiotludo/akhq/blob/dev/helm/akhq/values.yaml

But sometimes some examples are needed to understand better how to define this, so here we show some examples:


## Examples

### AWS MSK with Basic Authentication and ALB controller ingress

The following HELM chart is an example of AWS MSK with a basic authentication and also using AWS load balancer controller.

So mixing the default values.yaml previously linked and adding the basic idea of basic AKHQ authentication (more info here: https://akhq.io/docs/configuration/authentifications/basic-auth.html) and the documentation about how to connect to the AWS MSK here https://akhq.io/docs/configuration/authentifications/aws-iam-auth.html, we created the following example.

And of course, about ingress and service is using similar configuration like other external helm charts are using in the opensource community.

If you need to add ACL defintions, LDAP integrations or other stuff. You can see some examples in the main documentation of AKHQ https://akhq.io/docs/

```yaml

# This is an example with basic auth and a AWS MSK and using a AWS loadbalancer controller ingress

image:
  repository: tchiotludo/akhq

# custom annotations (example: for prometheus)
annotations: {}
  #prometheus.io/scrape: 'true'
  #prometheus.io/port: '8080'
  #prometheus.io/path: '/prometheus'

podAnnotations: {}

# custom labels
labels: {}
  # custom.label: 'true'

podLabels: {}

## You can put directly your configuration here... or add java opts or any other env vars
#extraEnv: 
#  - name: AKHQ_CONFIGURATION
#    value: |
#        akhq:
#          secrets:
#            docker-kafka-server:
#              properties:
#  - name: JAVA_OPTS
#    value: "-Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts -Djavax.net.ssl.trustStorePassword=password"
#  # - name: CLASSPATH
#  #   value: "/any/additional/jars/desired.jar:/go/here.jar"

## Or you can also use configmap for the configuration...
#in that example we are using BASIC-AUTH #https://akhq.io/docs/configuration/authentifications/basic-auth.html
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


##... and secret for connection information
existingSecrets: ""
# name of the existingSecret
secrets: {}
#  akhq:
#    connections:
#      my-cluster-plain-text:
#        properties:
#          bootstrap.servers: "kafka:9092"
#        schema-registry:
#          url: "http://schema-registry:8085"
#          type: "confluent"
#          basic-auth-username: basic-auth-user
#          basic-auth-password: basic-auth-pass
#        connect:
#          - name: "my-connect"
#            url: "http://connect:8083"
#            basic-auth-username: basic-auth-user
#            basic-auth-password: basic-auth-pass

kafkaSecrets: []
#Provide extra base64 encoded kubernetes secrets (keystore/truststore)

# Any extra volumes to define for the pod (like keystore/truststore)
extraVolumes: []

# Any extra volume mounts to define for the akhq container
extraVolumeMounts: []

# Specify ServiceAccount for pod
serviceAccountName: null
serviceAccount:
  create: false
  #annotations:
  #  eks.amazonaws.com/role-arn: arn:aws:iam::123456789000:role/iam-role-name-here


# Add your own init container or uncomment and modify the example.
initContainers: {}
#   create-keystore:
#     image: "eclipse-temurin:11-jre"
#     command: ['sh', '-c', 'keytool']
#     volumeMounts:
#      - mountPath: /tmp
#        name: certs

# Configure the Pod Security Context
# ref: https://kubernetes.io/docs/tasks/configure-pod-container/security-context/
securityContext: {}
  # runAsNonRoot: true
  # runAsUser: 1000

# Configure the Container Security Context
# ref: https://kubernetes.io/docs/tasks/configure-pod-container/security-context/
containerSecurityContext: {}
  # allowPrivilegeEscalation: false
  # privileged: false
  # capabilities:
  #   drop:
  #     - ALL
  # runAsNonRoot: true
  # runAsUser: 1001
  # readOnlyRootFilesystem: true

readinessProbe:
  prefix: "" # set same as `micronaut.server.context-path`

resources: {}
  # limits:
  #  cpu: 100m
  #  memory: 128Mi
  # requests:
  #  cpu: 100m
  #  memory: 128Mi

nodeSelector: {}

tolerations: []

affinity: {}

service:
  enabled: true
  type: NodePort
  port: 8080
  targetPort: 8080
  labels:
    app: akhq
  portName: service-akhq


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

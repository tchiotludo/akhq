# Cluster configuration
* `akhq.connections` is a key value configuration with :
  * `key`: must be an url friendly (letter, number, _, -, ... dot are not allowed here)  string to identify your cluster (`my-cluster-1` and `my-cluster-2` is the example above)
  * `properties`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.
  * `schema-registry`: *(optional)*
    * `url`: the schema registry url
    * `type`: the type of schema registry used, either 'confluent' or 'tibco'
    * `basic-auth-username`: schema registry basic auth username
    * `basic-auth-password`: schema registry basic auth password
    * `properties`: all the configurations for registry client, especially ssl configuration
  * `connect`: *(optional list, define each connector as an element of a list)*
    * `name`: connect name
    * `url`: connect url
    * `basic-auth-username`: connect basic auth username
    * `basic-auth-password`: connect basic auth password
    * `ssl-trust-store`: /app/truststore.jks
    * `ssl-trust-store-password`: trust-store-password
    * `ssl-key-store`: /app/truststore.jks
    * `ssl-key-store-password`: key-store-password

## Basic cluster with plain auth

```yaml
akhq:
  connections:
    local:
      properties:
        bootstrap.servers: "local:9092"
      schema-registry:
        url: "http://schema-registry:8085"
      connect:
        - name: "connect"
          url: "http://connect:8083"
```
## Example for Confluent Cloud

```yaml
akhq:
  connections:
    ccloud:
      properties:
        bootstrap.servers: "{{ cluster }}.{{ region }}.{{ cloud }}.confluent.cloud:9092"
        security.protocol: SASL_SSL
        sasl.mechanism: PLAIN
        sasl.jaas.config: org.apache.kafka.common.security.plain.PlainLoginModule required username="{{ kafkaUsername }}" password="{{ kafkaPassword }}";
      schema-registry:
        url: "https://{{ cluster }}.{{ region }}.{{ cloud }}.confluent.cloud"
        basic-auth-username: "{{ schemaRegistryUsername }}"
        basic-auth-password: "{{ schemaRegistryPaswword }}"

```

## SSL Kafka Cluster
Configuration example for kafka cluster secured by ssl for saas provider like aiven (full https & basic auth):

You need to generate a jks & p12 file from pem, cert files give by saas provider.
```bash
openssl pkcs12 -export -inkey service.key -in service.cert -out client.keystore.p12 -name service_key
keytool -import -file ca.pem -alias CA -keystore client.truststore.jks
```

Configurations will look like this example:

```yaml
akhq:
  connections:
    ssl-dev:
      properties:
        bootstrap.servers: "{{host}}.aivencloud.com:12835"
        security.protocol: SSL
        ssl.truststore.location: {{path}}/avnadmin.truststore.jks
        ssl.truststore.password: {{password}}
        ssl.keystore.type: "PKCS12"
        ssl.keystore.location: {{path}}/avnadmin.keystore.p12
        ssl.keystore.password: {{password}}
        ssl.key.password: {{password}}
      schema-registry:
        url: "https://{{host}}.aivencloud.com:12838"
        type: "confluent"
        basic-auth-username: avnadmin
        basic-auth-password: {{password}}
        properties:
          schema.registry.ssl.truststore.location: {{path}}/avnadmin.truststore.jks
          schema.registry.ssl.truststore.password: {{password}}
      connect:
        - name: connect-1
          url: "https://{{host}}.aivencloud.com:{{port}}"
          basic-auth-username: avnadmin
          basic-auth-password: {{password}}
```


## OAuth2 authentification for brokers

Requirement Library Strimzi:

> The kafka brokers must be configured with the Strimzi library and an OAuth2 provider (Keycloak example).

> This [repository](https://github.com/strimzi/strimzi-kafka-oauth) contains documentation and examples.

Configuration Bootstrap:

> It's not necessary to compile AKHQ to integrate the Strimzi libraries since the libs will be included on the final image !

You must configure AKHQ through the application.yml file.

```yaml
akhq:
  connections:
    my-kafka-cluster:
      properties:
        bootstrap.servers: "<url broker kafka>:9094,<url broker kafka>:9094"
        sasl.jaas.config: org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required auth.valid.issuer.uri="https://<url keycloak>/auth/realms/sandbox_kafka" oauth.jwks.endpoint.uri="https:/<url keycloak>//auth/realms/sandbox_kafka/protocol/openid-connect/certs" oauth.username.claim="preferred_username" oauth.client.id="kafka-producer-client" oauth.client.secret="" oauth.ssl.truststore.location="kafka.server.truststore.jks" oauth.ssl.truststore.password="xxxxx" oauth.ssl.truststore.type="jks" oauth.ssl.endpoint_identification_algorithm="" oauth.token.endpoint.uri="https:///auth/realms/sandbox_kafka/protocol/openid-connect/token";
        sasl.login.callback.handler.class: io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler
        security.protocol: SASL_PLAINTEXT
        sasl.mechanism: OAUTHBEARER
```
I put oauth.ssl.endpoint_identification_algorithm = "" for testing or my certificates did not match the FQDN. In a production, you have to remove it.





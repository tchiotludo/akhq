# AKHQ (previously known as KafkaHQ)

![Last Version](https://img.shields.io/github/tag-pre/tchiotludo/akhq.svg)
![License](https://img.shields.io/github/license/tchiotludo/akhq)
![Docker Pull](https://img.shields.io/docker/pulls/tchiotludo/akhq.svg)
![Github Downloads](https://img.shields.io/github/downloads/tchiotludo/akhq/total)
![Github Start](https://img.shields.io/github/stars/tchiotludo/akhq.svg)
![Main](https://github.com/tchiotludo/akhq/workflows/Main/badge.svg)
[![Artifact HUB](https://img.shields.io/endpoint?url=https://artifacthub.io/badge/repository/akhq)](https://artifacthub.io/packages/search?repo=akhq)

> Kafka GUI for [Apache Kafka](http://kafka.apache.org/) to manage topics, topics data, consumers group, schema registry, connect and more...

<p align="center">
  <img width="460" src="client/src/images/logo_black.svg"  alt="AKHQ for Kafka logo" /><br /><br />
  <img width="720" src="docs/assets/images/video.gif"  alt="AKHQ for Kafka preview" />
</p>

## Contents

- [Features](#features)
- [Quick Preview](#quick-preview)
- [Installation](#installation)
    - [Docker](#docker)
    - [Stand Alone](#stand-alone)
    - [Kubernetes using Helm](#running-in-kubernetes-using-a-helm-chart)
- [Configuration](#configuration)
    - [JVM.options file](#run-with-another-jvmoptions-file)
    - [Kafka cluster](#kafka-cluster-configuration)
    - [AKHQ](#akhq-configuration)
    - [AKHQ Configuration Bootstrap OAuth2](#akhq-configuration-bootstrap-oauth2)
    - [Security](#security)
    - [Server](#server)
    - [Micronaut](#micronaut-configuration)
- [Api](#api)
- [Monitoring Endpoint](#monitoring-endpoint)
- [Development Environment](#development-environment)
- [Schema references](#schema-references)
- [Who's using AKHQ](#whos-using-akhq)


## Features

- **General**
  - Works with modern Kafka cluster (1.0+)
  - Connection on standard or ssl, sasl cluster
  - Multi cluster
- **Topics**
  - List
  - Configurations view
  - Partitions view
  - ACLS view
  - Consumer groups assignments view
  - Node leader & assignments view
  - Create a topic
  - Configure a topic
  - Delete a topic
- **Browse Topic data**
  - View data, offset, key, timestamp & headers
  - Automatic deserialization of avro message encoded with schema registry
  - Configurations view
  - Logs view
  - Delete a record
  - Empty a Topic (Delete all the record from one topic)
  - Sort view
  - Filter per partitions
  - Filter with a starting time
  - Filter data with a search string
- **Consumer Groups** (only with kafka internal storage, not with the old Zookeeper one)
  - List with lag, topics assignments
  - Partitions view & lag
  - ACLS view
  - Node leader & assignments view
  - Display active and pending consumers groups
  - Delete a consumer group
  - Update consumer group offsets to start / end / timestamp
- **Schema Registry**
  - List schema
  - Create / Update / Delete a schema
  - View and delete individual schema version
- **Connect**
  - List connect definition
  - Create / Update / Delete a definition
  - Pause / Resume / Restart a definition or a task
- **Nodes**
  - List
  - Configurations view
  - Logs view
  - Configure a node
- **ACLS**
  - List principals
  - List principals topic & group acls
- **Authentication and Roles**
  - Read only mode
  - BasicHttp with roles per user
  - User groups configuration
  - Filter topics with regexp for current groups
  - Ldap configuration to match AKHQ groups/roles
  - Filter consumer groups with regexp for current groups

## New React UI

Since this is a major rework, the new UI can have some issues, so please [report any issue](https://github.com/tchiotludo/akhq/issues), thanks!

## Quick preview
* Download [docker-compose.yml](https://raw.githubusercontent.com/tchiotludo/akhq/master/docker-compose.yml) file
* run `docker-compose pull` to be sure to have the last version of AKHQ
* run `docker-compose up`
* go to [http://localhost:8080](http://localhost:8080)

It will start a Kafka node, a Zookeeper node, a Schema Registry, a Kafka Connect, fill with some sample data, start a consumer
group and a kafka stream & start AKHQ.

## Installation

First you need a [configuration files](#configuration) in order to configure AKHQ connections to Kafka Brokers.

### Docker

```sh
docker run -d \
    -p 8080:8080 \
    -v /tmp/application.yml:/app/application.yml \
    tchiotludo/akhq
```
* With `-v /tmp/application.yml` must be an absolute path to configuration file
* Go to <http://localhost:8080>


### Stand Alone
* Install Java 11
* Download the latest jar on [release page](https://github.com/tchiotludo/akhq/releases)
* Create an [configuration files](#configuration)
* Launch the application with `java -Dmicronaut.config.files=/path/to/application.yml -jar akhq.jar`
* Go to <http://localhost:8080>


### Running in Kubernetes (using a Helm Chart)

### Using Helm repository

* Add the AKHQ helm charts repository:
```sh
helm repo add akhq https://akhq.io/
```
* Install or upgrade
```sh
helm upgrade --install akhq akhq/akhq
```
#### Requirements

* Chart version >=0.1.1 requires Kubernetes version >=1.14
* Chart version 0.1.0 works on previous Kubernetes versions
```sh
helm install akhq akhq/akhq --version 0.1.0
```

### Using git
* Clone the repository:
```sh
git clone https://github.com/tchiotludo/akhq && cd akhq/helm/akhq
```
* Update helm values located in [values.yaml](helm/akhq/values.yaml)
  * `configuration` values will contains all related configuration that you can find in [application.example.yml](application.example.yml) and will be store in a `ConfigMap`
  * `secrets` values will contains all sensitive configurations (with credentials) that you can find in [application.example.yml](application.example.yml) and will be store in `Secret`
  * Both values will be merged at startup
* Apply the chart:
```sh
helm install --name=akhq-release-name  .
```


## Configuration
Configuration file can by default be provided in either Java properties, YAML, JSON or Groovy files. YML Configuration
file example can be found here :[application.example.yml](application.example.yml)

### Pass custom Java opts

By default, the docker container will allow a custom JVM options setting the environments vars `JAVA_OPTS`.
For example, if you want to change the default timezone, just add `-e "JAVA_OPTS=-Duser.timezone=Europe/Paris"`

### Run with another jvm.options file

By default, the docker container will run with a [jvm.options](docker/app/jvm.options) file, you can override it with
your own with an Environment Variable. With the `JVM_OPTS_FILE` environment variable, you can override the jvm.options file by passing
the path of your file instead.

Override the `JVM_OPTS_FILE` with docker run:

```sh
docker run -d \
    --env JVM_OPTS_FILE={{path-of-your-jvm.options-file}}
    -p 8080:8080 \
    -v /tmp/application.yml:/app/application.yml \
    tchiotludo/akhq
```

Override the `JVM_OPTS_FILE` with docker-compose:

```yaml
version: '3.7'
services:
  akhq:
    image: tchiotludo/akhq-jvm:dev
    environment:
      JVM_OPTS_FILE: /app/jvm.options
    ports:
      - "8080:8080"
    volumes:
      - /tmp/application.yml:/app/application.yml
```

If you do not override the `JVM_OPTS_FILE`, the docker container will take the defaults one instead.

### Kafka cluster configuration
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

#### SSL Kafka Cluster with basic auth
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

### AKHQ configuration

#### Pagination
* `akhq.pagination.page-size` number of topics per page (default : 25)

#### Topic List
* `akhq.topic.internal-regexps` is list of regexp to be considered as internal (internal topic can't be deleted or updated)
* `akhq.topic.stream-regexps` is list of regexp to be considered as internal stream topic

#### Topic creation default values

These parameters are the default values used in the topic creation page.

* `akhq.topic.replication` Default number of replica to use
* `akhq.topic.partition` Default number of partition

#### Topic Data
* `akhq.topic-data.sort`: default sort order (OLDEST, NEWEST) (default: OLDEST)
* `akhq.topic-data.size`: max record per page (default: 50)
* `akhq.topic-data.poll-timeout`: The time, in milliseconds, spent waiting in poll if data is not available in the buffer (default: 1000).

#### Ui Settings
##### Topics
* `akhq.ui-options.topic.default-view` is default list view (ALL, HIDE_INTERNAL, HIDE_INTERNAL_STREAM, HIDE_STREAM) (default: HIDE_INTERNAL)
* `akhq.ui-options.topic.skip-consumer-groups` hide consumer groups columns on topic list
* `akhq.ui-options.topic.skip-last-record` hide the last records on topic list

##### Topic Data
* `akhq.ui-options.topic-data.sort`: default sort order (OLDEST, NEWEST) (default: OLDEST)


#### Protobuf deserialization

To deserialize topics containing data in Protobuf format, you can set topics mapping:
for each `topic-regex` you can specify `descriptor-file-base64` (descriptor file encoded to Base64 format),
or you can put descriptor files in `descriptors-folder` and specify `descriptor-file` name,
also specify corresponding message types for keys and values.
If, for example, keys are not in Protobuf format, `key-message-type` can be omitted,
the same for `value-message-type`.
This configuration can be specified for each Kafka cluster.

Example configuration can look like as follows:

```
akhq:
  connections:
    kafka:
      properties:
        # standard kafka properties
      deserialization:
        protobuf:
          descriptors-folder: "/app/protobuf_desc"
          topics-mapping:
            - topic-regex: "album.*"
              descriptor-file-base64: "Cs4BCgthbGJ1bS5wcm90bxIXY29tLm5ldGNyYWNrZXIucHJvdG9idWYidwoFQWxidW0SFAoFdGl0bGUYASABKAlSBXRpdGxlEhYKBmFydGlzdBgCIAMoCVIGYXJ0aXN0EiEKDHJlbGVhc2VfeWVhchgDIAEoBVILcmVsZWFzZVllYXISHQoKc29uZ190aXRsZRgEIAMoCVIJc29uZ1RpdGxlQiUKF2NvbS5uZXRjcmFja2VyLnByb3RvYnVmQgpBbGJ1bVByb3RvYgZwcm90bzM="
              value-message-type: "Album"
            - topic-regex: "film.*"
              descriptor-file-base64: "CuEBCgpmaWxtLnByb3RvEhRjb20uY29tcGFueS5wcm90b2J1ZiKRAQoERmlsbRISCgRuYW1lGAEgASgJUgRuYW1lEhoKCHByb2R1Y2VyGAIgASgJUghwcm9kdWNlchIhCgxyZWxlYXNlX3llYXIYAyABKAVSC3JlbGVhc2VZZWFyEhoKCGR1cmF0aW9uGAQgASgFUghkdXJhdGlvbhIaCghzdGFycmluZxgFIAMoCVIIc3RhcnJpbmdCIQoUY29tLmNvbXBhbnkucHJvdG9idWZCCUZpbG1Qcm90b2IGcHJvdG8z"
              value-message-type: "Film"
            - topic-regex: "test.*"
              descriptor-file: "other.desc"
              key-message-type: "Row"
              value-message-type: "Envelope"
```

More examples about Protobuf deserialization can be found in [tests](./src/test/java/org/akhq/utils).
Info about the descriptor files generation can be found in [test resources](./src/test/resources/protobuf_proto).

### AKHQ Configuration Bootstrap OAuth2

#### Requirement Library Strimzi

> The kafka brokers must be configured with the Strimzi library and an OAuth2 provider (Keycloak example).

> This ![repository](https://github.com/strimzi/strimzi-kafka-oauth) contains documentation and examples.

#### Configuration Bootstrap

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

### Security
* `akhq.security.default-group`: Default group for all the user even unlogged user.
By default, the default group is `admin` and allow you all read / write access on the whole app.

By default, security & roles is disabled and anonymous user have full access, i.e. `micronaut.security.enabled: false`.
To enable security & roles set `micronaut.security.enabled: true` and configure desired type of authentication (basic auth, LDAP, etc.).

If you need a read-only application, simply add this to your configuration files :
```yaml
akhq:
  security:
    default-group: reader
```



#### Auth

##### JWT

AKHQ uses JWT tokens to perform authentication.
Please generate a secret that is at least 256 bits and change the config like this:

```yaml
micronaut:
  security:
    enabled: true
    token:
      jwt:
        signatures:
          secret:
            generator:
              secret: <Your secret here>
```

##### Groups

Groups allow you to limit user

Define groups with specific roles for your users
* `akhq.security.default-group`: Default group for all the user even unlogged user

* `akhq.security.groups`: Groups map definition
  * `key:` a uniq key used as name if not specified
    * `  name: group-name` Group identifier
    * `roles`: Roles list for the group
    * `attributes.topics-filter-regexp`: Regexp list to filter topics available for current group
    * `attributes.connects-filter-regexp`: Regexp list to filter Connect tasks available for current group
    * `attributes.consumer-groups-filter-regexp`: Regexp list to filter Consumer Groups available for current group

:warning: `topics-filter-regexp`, `connects-filter-regexp` and `consumer-groups-filter-regexp` are only used when listing resources.
If you have `topics/create` or `connect/create` roles and you try to create a resource that doesn't follow the regexp, that resource **WILL** be created.

3 defaults group are available :
- `admin` with all right
- `reader` with only read access on all AKHQ
- `no-roles` without any roles, that force user to login

##### Basic Auth
* `akhq.security.basic-auth`: List user & password with affected roles
  * `- username: actual-username`: Login of the current user as a yaml key (maybe anything email, login, ...)
    * `password`: Password in sha256 (default) or bcrypt. The password can be converted
      * For default SHA256, with command `echo -n "password" | sha256sum` or Ansible filter `{{ 'password' | hash('sha256') }}`
      * For BCrypt, with Ansible filter `{{ 'password' | password_hash('blowfish') }}`
    * `passwordHash`: Password hashing algorithm, either `SHA256` or `BCRYPT`
    * `groups`: Groups for current user

> Take care that basic auth will use session store in the server **memory**. If your instance is behind a reverse proxy or a
> loadbalancer, you will need to forward the session cookie named `SESSION` and / or use
> [session stickiness](https://en.wikipedia.org/wiki/Load_balancing_(computing)#Persistence)

Configure basic-auth connection in AKHQ
```yaml
micronaut:
  security:
    enabled: true
akhq.security:
  basic-auth:
    - username: admin
      password: "$2a$<hashed password>"
      passwordHash: BCRYPT
      groups:
      - admin
    - username: reader
      password: "<SHA-256 hashed password>"
      groups:
      - reader
```

##### LDAP
Configure how the ldap groups will be matched in AKHQ groups
* `akhq.security.ldap.groups`: Ldap groups list
  * `- name: ldap-group-name`: Ldap group name (same name as in ldap)
    * `groups`: AKHQ group list to be used for current ldap group

Example using [online ldap test server](https://www.forumsys.com/tutorials/integration-how-to/ldap/online-ldap-test-server/)

Configure ldap connection in micronaut
```yaml
micronaut:
  security:
    enabled: true
    ldap:
      default:
        enabled: true
        context:
          server: 'ldap://ldap.forumsys.com:389'
          managerDn: 'cn=read-only-admin,dc=example,dc=com'
          managerPassword: 'password'
        search:
          base: "dc=example,dc=com"
        groups:
          enabled: true
          base: "dc=example,dc=com"
```

If you want to enable anonymous auth to your LDAP server you can pass :
```yaml
managerDn: ''
managerPassword: ''
```

In Case your LDAP groups do not use the default UID for group membership, you can solve this using

```yaml
micronaut:
  security:
    enabled: true
    ldap:
      default:
        search:
          base: "OU=UserOU,dc=example,dc=com"
          attributes:
            - "cn"
        groups:
          enabled: true
          base: "OU=GroupsOU,dc=example,dc=com"
          filter: "member={0}"
```
Replace
```yaml
attributes:
  - "cn"
```
with your group membership attribute

Configure AKHQ groups and Ldap groups and users
```yaml
micronaut:
  security:
    enabled: true
akhq:
  security:
    groups:
      topic-reader:
        name: topic-reader # Group name
        roles:  # roles for the group
          - topic/read
        attributes:
          # List of Regexp to filter topic available for group
          # Single line String also allowed
          # topics-filter-regexp: "^(projectA_topic|projectB_.*)$"
          topics-filter-regexp:
            - "^projectA_topic$" # Individual topic
            - "^projectB_.*$" # Topic group
          connects-filter-regexp:
            - "^test.*$"
          consumer-groups-filter-regexp:
            - "consumer.*"
      topic-writer:
        name: topic-writer # Group name
        roles:
          - topic/read
          - topic/insert
          - topic/delete
          - topic/config/update
        attributes:
          topics-filter-regexp:
            - "test.*"
          connects-filter-regexp:
            - "^test.*$"
          consumer-groups-filter-regexp:
            - "consumer.*"
    ldap:
      groups:
        - name: mathematicians
          groups:
            - topic-reader
        - name: scientists
          groups:
            - topic-reader
            - topic-writer
      users:
        - username: franz
          groups:
            - topic-reader
            - topic-writer

```

### OIDC
To enable OIDC in the application, you'll first have to enable OIDC in micronaut:

```yaml
micronaut:
  security:
    oauth2:
      enabled: true
      clients:
        google:
          client-id: "<client-id>"
          client-secret: "<client-secret>"
          openid:
            issuer: "<issuer-url>"
```

To further tell AKHQ to display OIDC options on the login page and customize claim mapping, configure OIDC in the AKHQ config:

```yaml
akhq:
  security:
    oidc:
      enabled: true
      providers:
        google:
          label: "Login with Google"
          username-field: preferred_username
          # specifies the field name in the oidc claim containing the use assigned role (eg. in keycloak this would be the Token Claim Name you set in your Client Role Mapper)
          groups-field: roles
          default-group: topic-reader
          groups:
            # the name of the user role set in your oidc provider and associated with your user (eg. in keycloak this would be a client role)
            - name: mathematicians
              groups:
                # the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)
                - topic-reader
            - name: scientists
              groups:
                - topic-reader
                - topic-writer
          users:
            - username: franz
              groups:
                - topic-reader
                - topic-writer
```

The username field can be any string field, the roles field has to be a JSON array.


### Header configuration (reverse proxy)

To enable Header authentication in the application, you'll have to configure the header that will resolve users & groups:

```yaml
akhq:
  security:
    # Header configuration (reverse proxy)
    header-auth:
      user-header: x-akhq-user # mandatory (the header name that will contain username)
      groups-header: x-akhq-group # optional (the header name that will contain groups separated by groups-header-separator)
      groups-header-separator: , # optional (separator, defaults to ',')
      ip-patterns: [0.0.0.0] # optional (Java regular expressions for matching trusted IP addresses, '0.0.0.0' matches all addresses)
      default-group: topic-reader
      groups: # optional
        # the name of the user group read from header
        - name: header-admin-group
          groups:
            # the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)
            - admin
      users: # optional
        - username: header-user # username matching the `user-header` value
          groups: # list of groups / additional groups
            - topic-writer
        - username: header-admin
          groups:
            - admin
```

* `user-header` is mandatory in order to map the user with `users` list or to display the user on the ui if no `users` is provided.
* `groups-header` is optional and can be used in order to inject a list of groups for all the users. This list will be merged with `groups` for the current users.
* `groups-header-separator` is optional and can be used to customize group separator used when parsing `groups-header` header, defaults to `,`.
* `ip-patterns` limits the IP addresses that header authentication will accept, given as a list of Java regular expressions, omit or set to `[0.0.0.0]` to allow all addresses
* `default-group` default AKHQ group, used when no groups were read from `groups-header`
* `groups` maps external group names read from headers to AKHQ groups.
* `users` assigns additional AKHQ groups to users.

### External roles and attributes mapping

If you managed which topics (or any other resource) in an external system, you have access to 2 more implementations mechanisms to map your authenticated user (from either Local, Header, LDAP or OIDC) into AKHQ roles and attributes:

If you use this mechanism, keep in mind it will take the local user's groups for local Auth, and the external groups for Header/LDAP/OIDC (ie. this will NOT do the mapping between Header/LDAP/OIDC and local groups)

**Default configuration-based**
This is the current implementation and the default one (doesn't break compatibility)
````yaml
akhq:
  security:
    default-group: no-roles
    groups:
      reader:
        roles:
          - topic/read
        attributes:
          topics-filter-regexp: [".*"]
      no-roles:
        roles: []
    ldap: # LDAP users/groups to AKHQ groups mapping
    oidc: # OIDC users/groups to AKHQ groups mapping
    header-auth: # header authentication users/groups to AKHQ groups mapping
````

**REST API**
````yaml
akhq:
  security:
    default-group: no-roles
    rest:
      enabled: true
      url: https://external.service/get-roles-and-attributes
    groups: # anything set here will not be used
````

In this mode, AKHQ will send to the ``akhq.security.rest.url`` endpoint a POST request with the following JSON :

````json
{
  "providerType": "LDAP or OIDC or BASIC_AUTH or HEADER",
  "providerName": "OIDC provider name (OIDC only)",
  "username": "user",
  "groups": ["LDAP-GROUP-1", "LDAP-GROUP-2", "LDAP-GROUP-3"]
}
````
and expect the following JSON as response :
````json
{
  "roles": ["topic/read", "topic/write", "..."],
  "attributes":
  {
    "topics-filter-regexp": [".*"],
    "connects-filter-regexp": [".*"],
    "consumer-groups-filter-regexp": [".*"]
  }
}
````

**Groovy API**
````yaml
akhq:
  security:
    default-group: no-roles
    groovy:
      enabled: true
      file: |
        package org.akhq.utils;
        class GroovyCustomClaimProvider implements ClaimProvider {
            @Override
            AKHQClaimResponse generateClaim(AKHQClaimRequest request) {
                AKHQClaimResponse a = new AKHQClaimResponse();
                a.roles = ["topic/read"]
                a.attributes = [
                        topicsFilterRegexp: [".*"],
                        connectsFilterRegexp: [".*"],
                        consumerGroupsFilterRegexp: [".*"]
                ]
                return a
            }
        }
    groups: # anything set here will not be used
````
``akhq.security.groovy.file`` must be a groovy class that implements the interface ClaimProvider :
````java
package org.akhq.utils;
public interface ClaimProvider {

    AKHQClaimResponse generateClaim(AKHQClaimRequest request);

    class AKHQClaimRequest{
        ProviderType providerType;
        String providerName;
        String username;
        List<String> groups;
    }
    class AKHQClaimResponse {
        private List<String> roles;
        private Map<String,Object> attributes;
    }
    enum ProviderType {
        BASIC_AUTH,
        LDAP,
        OIDC
    }
}
````

### Debugging authentication

Debugging auth can be done by increasing log level on Micronaut that handle most of the authentication part :
```bash
curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:28081/loggers/io.micronaut.security


curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:28081/loggers/org.akhq.configs
```

### Server
* `micronaut.server.context-path`: if behind a reverse proxy, path to akhq with trailing slash (optional). Example:
  akhq is behind a reverse proxy with url <http://my-server/akhq>, set base-path: "/akhq/". Not needed if you're
  behind a reverse proxy with subdomain <http://akhq.my-server/>

### Kafka admin / producer / consumer default properties
* `akhq.clients-defaults.{{admin|producer|consumer}}.properties`: default configuration for admin producer or
  consumer. All properties from [Kafka documentation](https://kafka.apache.org/documentation/) is available.

### Micronaut configuration
> Since AKHQ is based on [Micronaut](https://micronaut.io/), you can customize configurations (server port, ssl, ...) with [Micronaut configuration](https://docs.micronaut.io/snapshot/guide/configurationreference.html#io.micronaut.http.server.HttpServerConfiguration).
> More information can be found on [Micronaut documentation](https://docs.micronaut.io/snapshot/guide/index.html#config)

### Docker

The AKHQ docker image supports 4 environment variables to handle configuration :
* `AKHQ_CONFIGURATION`: a string that contains the full configuration in yml that will be written on
  /app/configuration.yml on the container.
* `MICRONAUT_APPLICATION_JSON`: a string that contains the full configuration in JSON format
* `MICRONAUT_CONFIG_FILES`: a path to a configuration file in the container. Default path is `/app/application.yml`
* `CLASSPATH`: additional Java classpath entries. Must be used to specify the location of the TIBCO Avro client library
  jar if a 'tibco' schema registry type is used

#### How to mount configuration file

Take care when you mount configuration files to not remove akhq files located on /app.
You need to explicitly mount the `/app/application.yml` and not mount the `/app` directory.
This will remove the AKHQ binaries and give you this error: `
/usr/local/bin/docker-entrypoint.sh: 9: exec: ./akhq: not found`

```yaml
volumeMounts:
- mountPath: /app/application.yml
  subPath: application.yml
  name: config
  readOnly: true

```

#### Using the TIBCO schema registry

If you are using the TIBCO schema registry, you will also need to mount and use the TIBCO Avro client library and its
dependencies. The akhq service in a docker compose file might look something like:

```yaml
  akhq:
    # build:
    #   context: .
    image: tchiotludo/akhq
    volumes:
      - /opt/tibco/akd/repo/1.2/lib/tibftl-kafka-avro-1.2.0-thin.jar:/app/tibftl-kafka-avro-1.2.0-thin.jar
      - /opt/tibco/akd/repo/1.2/lib/deps:/app/deps
    environment:
      AKHQ_CONFIGURATION: |
        akhq:
          connections:
            docker-kafka-server:
              properties:
                bootstrap.servers: "kafka:9092"
              schema-registry:
                type: "tibco"
                url: "http://repo:8081"
              connect:
                - name: "connect"
                  url: "http://connect:8083"
      CLASSPATH: "/app/tibftl-kafka-avro-1.2.0-thin.jar:/app/deps/*"
    ports:
      - 8080:8080
    links:
      - kafka
      - repo
```

## Api
An **experimental** api is available that allow you to fetch all the exposed on AKHQ through api.

Take care that this api is **experimental** and **will** change in a future release.
Some endpoints expose too many data and is slow to fetch, and we will remove
some properties in a future in order to be fast.

Example: List topic endpoint expose log dir, consumer groups, offsets. Fetching all theses
is slow for now, and we will remove these in a future.

You can discover the api endpoint here :
* `/api`: a [RapiDoc](https://mrin9.github.io/RapiDoc/) webpage that document all the endpoints.
* `/swagger/akhq.yml`: a full [OpenApi](https://www.openapis.org/) specifications files

## Monitoring endpoint
Several monitoring endpoint is enabled by default and available on port `28081` only.

You can disable it, change the port or restrict access only for authenticated users following micronaut configuration below.


* `/info` [Info Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#infoEndpoint) with git status information.
* `/health` [Health Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#healthEndpoint)
* `/loggers` [Loggers Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#loggersEndpoint)
* `/metrics` [Metrics Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#metricsEndpoint)
* `/prometheus` [Prometheus Endpoint](https://micronaut-projects.github.io/micronaut-micrometer/latest/guide/)

## Debugging AKHQ performance issues

You can debug all query duration from AKHQ with this commands
```bash
curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:28081/loggers/org.akhq
```

## Development Environment

### Early dev image

You can have access to last feature / bug fix with docker dev image automatically build on tag `dev`
```bash
docker pull tchiotludo/akhq:dev
```

The dev jar is not publish on GitHub, you have 2 solutions to have the `dev` jar :

Get it from docker image
```bash
docker pull tchiotludo/akhq:dev
docker run --rm --name=akhq -it tchiotludo/akhq:dev
docker cp akhq:/app/akhq.jar .
```
Or build it with a `./gradlew shadowJar`, the jar will be located here `build/libs/akhq-*.jar`


### Development Server

A docker-compose is provided to start a development environment.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.

The configuration for the dev server is in `application.dev.yml`.

### Setup local dev environment on Windows

In case you want to develop for AKHQ on Windows with IntelliJ IDEA without Docker (for any reason) you can follow this
brief guide. For the following steps, please, make sure you meet these requirements:

 * OS: Windows (10)
 * Kafka (2.6.0) is downloaded and extracted, the installation directory is referred to as $KAFKA_HOME in the latter
 * Git is installed and configured
 * IntelliJ IDEA (Community Edition 2020.2) with the following plugins installed:
   * Gradle (bundled with IDEA)
   * [Lombok](https://plugins.jetbrains.com/plugin/6317-lombok)

First run a Kafka server locally. Therefore, you need to start Zookeeper first by opening a CMD and doing:
```bash
$KAFKA_HOME\bin\windows\zookeeper-server-start.bat config\zookeper.properties
$KAFKA_HOME\bin\windows\kafka-server-start.bat config\server.properties
```
A zero-config Kafka server should be up and running locally on your machine now. For further details or troubleshooting
see [Kafka Getting started guide](https://kafka.apache.org/quickstart). In the next step we're going to checkout AKHQ from GitHub:
```bash
git clone https://github.com/tchiotludo/akhq.git
```

Open the checked out directory in IntelliJ IDEA. The current version (0.16.0) of AKHQ is built with Java 11. If you
don't have OpenJDK 11 installed already, do the following in IntelliJ IDEA: _File > Project Structure... > Platform Settings >
SDKs > + > Download JDK... >_ select a vendor of your choice (but make sure it's version 11), download + install. Make sure
that JDK 11 is set under _Project Settings > Project SDK_ and language level is Java 11. Now tell Gradle to use Java 11
as well: _File > Settings > Plugins > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM_: any JDK 11.

To configure AKHQ for using the Kafka server you set up before, edit `application.yml` by adding the following under `akhq`:
```yaml
akhq:
  connections:
    kafka:
      properties:
        bootstrap.servers: "localhost:9092"
```
/!\ Do not commit this part of `application.yml`. A more secure way to configure your local development Kafka server is
described in the Micronaut doc, chapter ["Application Configuration"](https://docs.micronaut.io/1.3.0.M1/guide/index.html#config).

Now you should be able to build the project with Gradle. Therefore, go to the Gradle view in IDEA, select _Tasks > build >
build_. If an error occurs saying that any filename is too long: move your project directory to a root directory in your
filesystem or as a fix (only for testing purposes) set the argument `-x test` to skip tests temporarily.

To debug a running AKHQ instance, go to the Gradle tab in IntelliJ IDEA, _Tasks > application_ > right click `run` and click
"_Debug(...)_". AKHQ should start up and hit the breakpoints you set in your IDE. Happy developing/debugging!

## Schema references

Since Confluent 5.5.0, Avro schemas can now be reused by others schemas through schema references. This feature allows to define a schema once and use it as a record type inside one or more schemas.

When registering new Avro schemas with AKHQ UI, it is now possible to pass a slightly more complex object with a `schema` and a `references` field.

To register a new schema without references, no need to change anything:

```json
{
    "name": "Schema1",
    "namespace": "org.akhq",
    "type": "record",
    "fields": [
        {
            "name": "description",
            "type": "string"
        }
    ]
}
```

To register a new schema with a reference to an already registered schema:

```json
{
    "schema": {
        "name": "Schema2",
        "namespace": "org.akhq",
        "type": "record",
        "fields": [
            {
                "name": "name",
                "type": "string"
            },
            {
                "name": "schema1",
                "type": "Schema1"
            }
        ]
    },
    "references": [
        {
            "name": "Schema1",
            "subject": "SCHEMA_1",
            "version": 1
        }
    ]
}
````

Documentation on Confluent 5.5 and schema references can be found [here](https://docs.confluent.io/5.5.0/schema-registry/serdes-develop/index.html).


## Who's using AKHQ
* [Adeo](https://www.adeo.com/)
* [Auchan Retail](https://www.auchan-retail.com/)
* [Bell](https://www.bell.ca)
* [BMW Group](https://www.bmwgroup.com)
* [Boulanger](https://www.boulanger.com/)
* [GetYourGuide](https://www.getyourguide.com)
* [Klarna](https://www.klarna.com)
* [La Redoute](https://laredoute.io/)
* [Leroy Merlin](https://www.leroymerlin.fr/)
* [NEXT Technologies](https://www.nextapp.co/)
* [Nuxeo](https://www.nuxeo.com/)
* [Pipedrive](https://www.pipedrive.com)
* [BARMER](https://www.barmer.de/)
* [TVG](https://www.tvg.com)
* [Depop](https://www.depop.com)
* [BPCE-IT](https://www.bpce-it.fr/)



## Credits

Many thanks to:

* [JetBrains](https://www.jetbrains.com/?from=AKHQ) for their free OpenSource license.
* Apache, Apache Kafka, Kafka, and associated open source project names are trademarks of the Apache Software Foundation. AKHQ is not affiliated with, endorsed by, or otherwise associated with the Apache Software.

[![Jetbrains](https://user-images.githubusercontent.com/2064609/55432917-6df7fc00-5594-11e9-90c4-5133fbb6d4da.png)](https://www.jetbrains.com/?from=AKHQ)


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)

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
  - Consumers groups assignments view
  - Node leader & assignments view
  - Create a topic
  - Configure a topic
  - Delete a topic
- **Browse Topic datas**
  - View data, offset, key, timestamp & headers
  - Automatic deserializarion of avro message encoded with schema registry
  - Configurations view
  - Logs view
  - Delete a record
  - Empty a Topic (Delete all the record from one topic)
  - Sort view
  - Filter per partitions
  - Filter with a starting time
  - Filter data with a search string
- **Consumer Groups** (only with kafka internal storage, not with old Zookeeper)
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
- **Authentification and Roles**
  - Read only mode
  - BasicHttp with roles per user
  - User groups configuration  
  - Filter topics with regexp for current groups
  - Ldap configuration to match AKHQ groups/roles

## New React UI
Release `0.15.0` introduce a new UI based on React. This is the default one when you reach AKHQ.

The old one is still available on path `/{cluster}/topic` but will be remove on release `0.16.0`.

Since this is a major rework, the new UI can have some issue, so please [report any issue](https://github.com/tchiotludo/akhq/issues), thanks ! 

## Quick preview
* Download [docker-compose.yml](https://raw.githubusercontent.com/tchiotludo/akhq/master/docker-compose.yml) file
* run `docker-compose pull` to be sure to have the last version of AKHQ
* run `docker-compose up`
* go to [http://localhost:8080](http://localhost:8080)

It will start a Kafka node, a Zookeeper node, a Schema Registry, a Connect, fill with some sample data, start a consumer
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
* Install it:
```sh
helm install --name akhq akhq/akhq
```

#### Requirements

* Chart version >=0.1.1 requires Kubernetes version >=1.14
* Chart version 0.1.0 works on previous Kubernetes versions
```sh
helm install --name akhq akhq/akhq --version 0.1.0
```

### Using git
* Clone the repository:
```sh
git clone https://github.com/tchiotludo/akhq && cd akhq/deploy/helm/akhq
```
* Update helm values located in [deploy/helm/values.yaml](helm/akhq/values.yaml)
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

By default, the docker container will allow a custom jvn options setting the environnments vars `JAVA_OPTS`.
For example, if you want to change the default timezome, just add `-e "JAVA_OPTS=-Duser.timezone=Europe/Paris"`

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
  * `key`: must be an url friendly (letter, number, _, -, ... dot are not allowed here)  string the identify your cluster (`my-cluster-1` and `my-cluster-2` is the example above)
  * `properties`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.
  * `schema-registry`: *(optional)*
    * `url`: the schema registry url 
    * `basic-auth-username`: schema registry basic auth username
    * `basic-auth-password`: schema registry basic auth password
    * `properties`: all the configurations for registry client, especially ssl configuration
  * `connect`: *(optional list, define each connector as a element of a list)*
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
        basic-auth-username: avnadmin
        basic-auth-password: {{password}}
        properties: {}
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
* `akhq.topic.default-view` is default list view (ALL, HIDE_INTERNAL, HIDE_INTERNAL_STREAM, HIDE_STREAM)
* `akhq.topic.internal-regexps` is list of regexp to be considered as internal (internal topic can't be deleted or updated)
* `akhq.topic.stream-regexps` is list of regexp to be considered as internal stream topic
* `akhq.topic.skip-consumer-groups` disable loading of consumer group information when showing topics (`true`), default is to load the information

#### Topic creation default values

These parameters are the default values used in the topic creation page.

* `akhq.topic.retention` Default retention in ms
* `akhq.topic.replication` Default number of replica to use
* `akhq.topic.partition` Default number of partition

#### Topic Data
* `akhq.topic-data.sort`: default sort order (OLDEST, NEWEST) (default: OLDEST)
* `akhq.topic-data.size`: max record per page (default: 50)
* `akhq.topic-data.poll-timeout`: The time, in milliseconds, spent waiting in poll if data is not available in the
  buffer (default: 1000).

    
### Security
* `akhq.security.default-group`: Default group for all the user even unlogged user.
By default, the default group is `admin` and allow you all read / write access on the whole app.

By default, security & roles is enabled by default but anonymous user have full access. You can completely disabled
security with `micronaut.security.enabled: false`.

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

* `akhq.security.groups`: Groups list definition
  * `- name: group-name` Group identifier
    * `roles`: Roles list for the group
    * `attributes.topics-filter-regexp`: Regexp to filter topics available for current group


3 defaults group are available :
- `admin` with all right
- `reader` with only read acces on all AKHQ
- `no-roles` without any roles, that force user to login 

##### Basic Auth
* `akhq.security.basic-auth`: List user & password with affected roles 
  * `- username: actual-username`: Login of the current user as a yaml key (may be anything email, login, ...)
    * `password`: Password in sha256 (default) or bcrypt. The password can be converted 
      * For default SHA256, with command `echo -n "password" | sha256sum` or Ansible filter `{{ 'password' | hash('sha256') }}`
      * For BCrypt, with Ansible filter `{{ 'password' | password_hash('blowfish') }}`
    * `passwordHash`: Password hashing algorithm, either `SHA256` or `BCRYPT`
    * `groups`: Groups for current user

> Take care that basic auth will use session store in server **memory**. If your instance is behind a reverse proxy or a
> loadbalancer, you will need to forward the session cookie named `SESSION` and / or use
> [sesssion stickiness](https://en.wikipedia.org/wiki/Load_balancing_(computing)#Persistence)

Configure basic-auth connection in AKHQ
```yaml
akhq.security:
  basic-auth:
    admin:
      password: "$2a$<hashed password>"
      passwordHash: BCRYPT
      groups: admin
    reader:
      password: "<SHA-256 hashed password>"
      groups: reader
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

Debuging ldap connection can be done with 
```bash
curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:8080/loggers/io.micronaut.configuration.security
```


Configure AKHQ groups and Ldap groups and users
```yaml
akhq:
  security:
    groups:
      - name: topic-reader # Group name
        roles:  # roles for the group
          - topic/read
        attributes:
          # Regexp to filter topic available for group
          topics-filter-regexp: "test\\.reader.*"
      - name: topic-writer # Group name
        roles:
          - topic/read
          - topic/insert
          - topic/delete
          - topic/config/update
        attributes:
          topics-filter-regexp: "test.*"
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
          groups-field: roles
          default-group: topic-reader
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

The username field can be any string field, the roles field has to be a JSON array.

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
AKHQ docker image support 3 environment variables to handle configuraiton :
* `AKHQ_CONFIGURATION`: a string that contains the full configuration in yml that will be written on
  /app/configuration.yml on container.
* `MICRONAUT_APPLICATION_JSON`: a string that contains the full configuration in JSON format
* `MICRONAUT_CONFIG_FILES`: a path to to a configuration file on container. Default path is `/app/application.yml`

#### How to mount configuration file

Take care when you mount configuration files to not remove akhq files located on /app.
You need to explicitely mount the `/app/application.yml` and not mount the `/app` directory.
This will remove the AKHQ binnaries and give you this error: `
/usr/local/bin/docker-entrypoint.sh: 9: exec: ./akhq: not found`

```yaml
volumeMounts:
- mountPath: /app/application.yml
  subPath: application.yml
  name: config
  readOnly: true

``` 

## Api
An **experimental** api is available that allow you to fetch all the exposed on AKHQ through api.

Take care that this api is **experimental** and **will** change in a future release. 
Some endpoint expose too many datas and is slow to fetch, and we will remove 
some properties in a future in order to be fast.

Example: List topic endpoint expose log dir, consumer groups, offsets. Fetching all of theses 
is slow for now and we will remove these in a future.

You can discover the api endpoint here : 
* `/api`: a [RapiDoc](https://mrin9.github.io/RapiDoc/) webpage that document all the endpoints.
* `/swagger/akhq.yml`: a full [OpenApi](https://www.openapis.org/) specifications files 

## Monitoring endpoint 
Several monitoring endpoint is enabled by default. You can disabled it or restrict access only for authenticated users
following micronaut configuration below.

* `/info` [Info Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#infoEndpoint) with git status
  informations.
* `/health` [Health Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#healthEndpoint)
* `/loggers` [Loggers Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#loggersEndpoint)
* `/metrics` [Metrics Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#metricsEndpoint)
* `/prometheus` [Prometheus Endpoint](https://micronaut-projects.github.io/micronaut-micrometer/latest/guide/)

## Debugging AKHQ performance issues 

You can debug all query duration from AKHQ with this commands
```bash
curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:8080/loggers/org.akhq
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

A docker-compose is provide to start a development environnement.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.


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


## Credits

Many thanks to:

* [JetBrains](https://www.jetbrains.com/?from=AKHQ) for their free OpenSource license.
* Apache, Apache Kafka, Kafka, and associated open source project names are trademarks of the Apache Software Foundation. AKHQ is not affiliated with, endorsed by, or otherwise associated with the Apache Software.

[![Jetbrains](https://user-images.githubusercontent.com/2064609/55432917-6df7fc00-5594-11e9-90c4-5133fbb6d4da.png)](https://www.jetbrains.com/?from=AKHQ)


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)

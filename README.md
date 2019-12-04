# KafkaHQ
[![Build Status](https://travis-ci.org/tchiotludo/kafkahq.svg?branch=master)](https://travis-ci.org/tchiotludo/kafkahq)
![Last Version](https://img.shields.io/github/tag-pre/tchiotludo/kafkahq.svg)

> Kafka GUI for topics, topics data, consumers group, schema registry, connect and more...


![preview](https://user-images.githubusercontent.com/2064609/50536651-e050de80-0b56-11e9-816f-9d3aca3f1c88.gif)

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
  - Sort view
  - Filter per partitions
  - Filter with a starting time
  - Filter data with a search string
- **Consumer Groups** (only with kafka internal storage, not with old Zookepper)
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
  - Ldap configuration to match KafkaHQ groups/roles

## Quick preview
* Download [docker-compose.yml](https://raw.githubusercontent.com/tchiotludo/kafkahq/master/docker-compose.yml) file
* run `docker-compose pull` to be sure to have the last version of KafkaHQ
* run `docker-compose up`
* go to [http://localhost:8080](http://localhost:8080)

It will start a Kafka node, a Zookeeper node, a Schema Registry, a Connect, fill with some sample data, start a consumer
group and a kafka stream & start KafkaHQ.

## Installation

First you need a [configuration files](#configuration) in order to configure KafkaHQ connections to Kafka Brokers.

### Docker

```sh
docker run -d \
    -p 8080:8080 \
    -v /tmp/application.yml:/app/application.yml \
    tchiotludo/kafkahq
```
* With `-v /tmp/application.yml` must be an absolute path to configuration file
* Go to <http://localhost:8080>


### Stand Alone

* Install Java 11
* Download the latest jar on [release page](https://github.com/tchiotludo/kafkahq/releases)
* Create an [configuration files](#configuration)
* Launch the application with `java -Dmicronaut.config.files=/path/to/application.yml -jar kafkahq.jar`
* Go to <http://localhost:8080>


## Configuration
Configuration file can by default be provided in either Java properties, YAML, JSON or Groovy files. YML Configuration
file example can be found here :[application.example.yml](application.example.yml)

### Kafka cluster configuration 
* `kafkahq.connections` is a key value configuration with :
  * `key`: must be an url friendly (letter, number, _, -, ... dot are not allowed here)  string the identify your cluster (`my-cluster-1` and `my-cluster-2` is the example above)
  * `properties`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.
  * `schema-registry`: *(optional)*
    * `url`: the schema registry url 
    * `basic-auth-username`: schema registry basic auth username
    * `basic-auth-password`: schema registry basic auth password
  * `connect`: *(optional)*
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
kafkahq:
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
      connect:
        url: "https://{{host}}.aivencloud.com:{{port}}"
        basic-auth-username: avnadmin
        basic-auth-password: {{password}}
```

### KafkaHQ configuration 

#### Pagination
* `kafkahq.pagination.page-size` number of topics per page (default : 25)
* `kafkahq.pagination.threads` number of parallel threads to resolve current page (default : 16). This setting can have a significant impact on performance on list page since it will fetch in parallel the Kafka API.

#### Topic List 
* `kafkahq.topic.default-view` is default list view (ALL, HIDE_INTERNAL, HIDE_INTERNAL_STREAM, HIDE_STREAM)
* `kafkahq.topic.internal-regexps` is list of regexp to be considered as internal (internal topic can't be deleted or updated)
* `kafkahq.topic.stream-regexps` is list of regexp to be considered as internal stream topic
* `kafkahq.topic.skip-consumer-groups` disable loading of consumer group information when showing topics (`true`), default is to load the information

#### Topic creation default values

These parameters are the default values used in the topic creation page.

* `kafkahq.topic.retention` Default retention in ms
* `kafkahq.topic.replication` Default number of replica to use
* `kafkahq.topic.partition` Default number of partition

#### Topic Data
* `kafkahq.topic-data.sort`: default sort order (OLDEST, NEWEST) (default: OLDEST)
* `kafkahq.topic-data.size`: max record per page (default: 50)
* `kafkahq.topic-data.poll-timeout`: The time, in milliseconds, spent waiting in poll if data is not available in the
  buffer (default: 1000).

    
### Security
* `kafkahq.security.default-roles`: Roles available for all the user even unlogged user, roles available are :
  * `topic/read`
  * `topic/insert`
  * `topic/delete`
  * `topic/config/update`
  * `node/read`
  * `node/config/update`
  * `topic/data/read`
  * `topic/data/insert`
  * `topic/data/delete`
  * `group/read`
  * `group/delete`
  * `group/offsets/update`
  * `acls/read`
  * `registry/read`
  * `registry/insert`
  * `registry/update`
  * `registry/delete`
  * `registry/version/delete`

By default, security & roles is enabled by default but anonymous user have full access. You can completely disabled
security with `micronaut.security.enabled: false`.

If you need a read-only application, simply add this to your configuration files : 
```yaml
kafkahq:
  security:
    default-roles:
      - topic/read
      - node/read
      - topic/data/read
      - group/read
      - registry/read
      - connect/read
```



#### Auth

##### Groups

Groups allow you to limit user 

Define groups with specific roles for your users
* `kafkahq.security.groups`: Groups list definition
  * `group-name`: Group identifier
    * `roles`: Roles list for the group
    * `attributes.topics-filter-regexp`: Regexp to filter topics available for current group

2 defaults group are available :
- `admin` with all right
- `reader` with only read acces on all KafkaHQ

##### Basic Auth
* `kafkahq.security.basic-auth`: List user & password with affected roles 
  * `actual-username`: Login of the current user as a yaml key (may be anything email, login, ...)
    * `password`: Password in sha256, can be converted with command `echo -n "password" | sha256sum`
    * `groups`: Groups for current user

> Take care that basic auth will use session store in server **memory**. If your instance is behind a reverse proxy or a
> loadbalancer, you will need to forward the session cookie named `SESSION` and / or use
> [sesssion stickiness](https://en.wikipedia.org/wiki/Load_balancing_(computing)#Persistence)



##### LDAP
Configure how the ldap groups will be matched in KafkaHQ groups 
* `kafkahq.security.ldap.group`: Ldap groups list
  * `ldap-group-name`: Ldap group name (same name as in ldap)
    * `groups`: KafkaHQ group list to be used for current ldap group

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

Configure KafkaHQ groups and Ldap groups
```yaml
kafkahq:
  security:
    groups:
      topic-reader: # Group name
        roles:  # roles for the group
          - topic/read
        attributes:
          # Regexp to filter topic available for group
          topics-filter-regexp: "test\\.reader.*"
      topic-writer: 
        roles:
          - topic/read
          - topic/insert
          - topic/delete
          - topic/config/update
        attributes:
          topics-filter-regexp: "test.*"
    ldap:
      group:
        mathematicians:
          groups:
            - topic-reader
        scientists:
          groups:
            - topic-reader
            - topic-writer
```

### Server 
* `kafkahq.server.base-path`: if behind a reverse proxy, path to kafkahq with trailing slash (optional). Example:
  kafkahq is behind a reverse proxy with url <http://my-server/kafkahq>, set base-path: "/kafkahq/". Not needed if you're
  behind a reverse proxy with subdomain <http://kafkahq.my-server/>

### Kafka admin / producer / consumer default properties
* `kafkahq.clients-defaults.{{admin|producer|consumer}}.properties`: default configuration for admin producer or
  consumer. All properties from [Kafka documentation](https://kafka.apache.org/documentation/) is available.

### Micronaut configuration 
> Since KafkaHQ is based on [Micronaut](https://micronaut.io/), you can customize configurations (server port, ssl, ...) with [Micronaut configuration](https://docs.micronaut.io/snapshot/guide/configurationreference.html#io.micronaut.http.server.HttpServerConfiguration).
> More information can be found on [Micronaut documentation](https://docs.micronaut.io/snapshot/guide/index.html#config)

### Docker
KafkaHQ docker image support 3 environment variables to handle configuraiton :
* `KAFKAHQ_CONFIGURATION`: a string that contains the full configuration in yml that will be written on
  /app/configuration.yml on container.
* `MICRONAUT_APPLICATION_JSON`: a string that contains the full configuration in JSON format
* `MICRONAUT_CONFIG_FILES`: a path to to a configuration file on container. Default path is `/app/application.yml`

#### How to mount configuration file

Take care when you mount configuration files to not remove kafkahq files located on /app.
You need to explicitely mount the `/app/application.yml` and not mount the `/app` directory.
This will remove the KafkaHQ binnaries and give you this error: `
/usr/local/bin/docker-entrypoint.sh: 9: exec: ./kafkahq: not found`

```yaml
volumeMounts:
- mountPath: /app/application.yml
  subPath: application.yml
  name: config
  readOnly: true

``` 


## Monitoring endpoint 
Several monitoring endpoint is enabled by default. You can disabled it or restrict access only for authenticated users
following micronaut configuration below.

* `/info` [Info Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#infoEndpoint) with git status
  informations.
* `/health` [Health Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#healthEndpoint)
* `/loggers` [Loggers Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#loggersEndpoint)
* `/metrics` [Metrics Endpoint](https://docs.micronaut.io/snapshot/guide/index.html#metricsEndpoint)
* `/prometheus` [Prometheus Endpoint](https://micronaut-projects.github.io/micronaut-micrometer/latest/guide/)

## Development Environment
A docker-compose is provide to start a development environnement.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.

## Who's using KafkaHQ 
* [Adeo](https://www.adeo.com/)
* [Auchan Retail](https://www.auchan-retail.com/)
* [Leroy Merlin](https://www.leroymerlin.fr/)
* [La Redoute](https://laredoute.io/)
* [Nuxeo](https://www.nuxeo.com/)

## Credits

Many thanks to:

[![Jetbrains](https://user-images.githubusercontent.com/2064609/55432917-6df7fc00-5594-11e9-90c4-5133fbb6d4da.png)](https://www.jetbrains.com/?from=KafkaHQ)

[JetBrains](https://www.jetbrains.com/?from=KafkaHQ) for their free OpenSource license.


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)

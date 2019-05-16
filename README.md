# KafkaHQ
[![Build Status](https://travis-ci.org/tchiotludo/kafkahq.svg?branch=master)](https://travis-ci.org/tchiotludo/kafkahq)

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
- **Authentification and Roles**
  - Read only mode
  - BasicHttp with roles per user

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
  * `key`: must be an url friendly string the identify your cluster (`my-cluster-1` and `my-cluster-2` is the example above)
  * `properties`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.
  * `schema-registry`: *(optional)*
    * `url`: the schema registry url 
    * `basic-auth.username`: schema registry basic auth username
    * `basic-auth.password`: schema registry basic auth password
  * `connect`: *(optional)*
    * `url`: connect url 
    * `basic-auth.username`: connect basic auth username
    * `basic-auth.password`: connect basic auth password
    * `ssl.trust-store`: /app/truststore.jks
    * `ssl.trust-store-password`: trust-store-password
    * `ssl.key-store`: /app/truststore.jks 
    * `ssl.key-store-password`: key-store-password

### KafkaHQ configuration 

#### Topic List 
* `kafkahq.topic.default-view` is default list view (ALL, HIDE_INTERNAL, HIDE_INTERNAL_STREAM, HIDE_STREAM)
* `kafkahq.topic.internal-regexps` is list of regexp to be considered as internal (internal topic can't be deleted or updated)
* `kafkahq.topic.stream-regexps` is list of regexp to be considered as internal stream topic


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

#### Basic Auth
* `kafkahq.security.basic-auth`: List user & password with affected roles 
  * `actual-username`: login of the current user as a yaml key (may be anything email, login, ...)
    * `password`: Password in sha256, can be converted with command `echo -n "password" | sha256sum`
    * `roles`: Role for current users

> Take care that basic auth will use session store in server **memory**. If your instance is behind a reverse proxy or a
> loadbalancer, you will need to forward the session cookie named `SESSION` and / or use
> [sesssion stickiness](https://en.wikipedia.org/wiki/Load_balancing_(computing)#Persistence)


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

## Development Environment
A docker-compose is provide to start a development environnement.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.


## Credits

Many thanks to:

[![Jetbrains](https://user-images.githubusercontent.com/2064609/55432917-6df7fc00-5594-11e9-90c4-5133fbb6d4da.png)](https://www.jetbrains.com/?from=KafkaHQ)

[JetBrains](https://www.jetbrains.com/?from=KafkaHQ) for their free OpenSource license.


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)

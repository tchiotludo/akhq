# KafkaHQ
[![Build Status](https://travis-ci.org/tchiotludo/kafkahq.svg?branch=master)](https://travis-ci.org/tchiotludo/kafkahq)

> Kafka GUI for topics, topics data, consumers group, schema registry and more... 


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
  - Create a schema
  - Update a schema
  - Delete a schema
  - View and delete individual schema version
- **Nodes**
  - List
  - Configurations view
  - Logs view
  - Configure a node


## Quick preview
* Download [docker-compose.yml](https://raw.githubusercontent.com/tchiotludo/kafkahq/master/docker-compose.yml) file
* run `docker-compose up`
* go to [http://localhost:8080](http://localhost:8080)

It will start a Kafka node, a Zookeeper node, a Schema Registry, fill with some sample data, start a consumer group and a kafka stream & start KafkaHQ.

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
* Go to http://localhost:8080


### Stand Alone

* Install Java 8
* Download the latest jar on [release page](https://github.com/tchiotludo/kafkahq/releases)
* Create an [configuration files](#configuration) 
* Launch the application with `java -Dmicronaut.config.files=/path/to/application.yml -jar kafkahq.jar`
* Go to http://localhost:8080


## Configuration
Configuration file can by default be provided in either Java properties, YAML, JSON or Groovy files.
Configuration file example in YML :

```yml
kafkahq:
  server:
    # if behind a reverse proxy, path to kafkahq with trailing slash (optionnal)
    base-path: ""
    # Access log configuration (optionnal)
    access-log:
      enabled: true # true by default 
      name: org.kafkahq.log.access # Logger name
      format: "[Date: {}] [Duration: {} ms] [Url: {} {} {}] [Status: {}] [Ip: {}] [Length: {}] [Port: {}]" # Logger format

  # default kafka properties for each clients, available for admin / producer / consumer (optionnal)
  clients-defaults:
    consumer:
      properties:
        isolation.level: read_committed

  # list of kafka cluster available for kafkahq
  connections:
    # url friendly name for the cluster
    my-cluster-1:
      # standard kafka properties (optionnal)
      properties:
        bootstrap.servers: "kafka:9092"
      # schema registry url (optionnal)
      schema-registry: "http://schema-registry:8085"

    my-cluster-2:
      properties:
        bootstrap.servers: "kafka:9093"
        security.protocol: SSL
        ssl.truststore.location: /app/truststore.jks
        ssl.truststore.password: password
        ssl.keystore.location: /app/keystore.jks
        ssl.keystore.password: password
        ssl.key.password: password
        
  # Topic display data options (optionnal)
  topic-data:
    # default sort order (OLDEST, NEWEST) (default: OLDEST)
    sort: OLDEST
    # max record per page (default: 50)
    size: 50
```

* `kafkahq.server.base-path`: if behind a reverse proxy, path to kafkahq with trailing slash
* `kafkahq.clients-defaults.{{admin|producer|consumer}}.properties`: if behind a reverse proxy, path to kafkahq with trailing slash
* `kafkahq.connections` is a key value configuration with :
  * `key`: must be an url friendly string the identify your cluster (`my-cluster-1` and `my-cluster-2` is the example above)
  * `properties`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.
  * `schema-registry`: the schema registry url *(optional)*

> Since KafkaHQ is based on [Micronaut](https://micronaut.io/), you can customize configurations (server port, ssl, ...) with [Micronaut configuration](https://docs.micronaut.io/snapshot/guide/configurationreference.html#io.micronaut.http.server.HttpServerConfiguration).
> More information can be found on [Micronaut documentation](https://docs.micronaut.io/snapshot/guide/index.html#config)

KafkaHQ docker image support 2 environment variables to handle configuraiton :
* `MICRONAUT_APPLICATION_JSON`: a string that contains the full configuration in JSON format
* `MICRONAUT_CONFIG_FILES`: a path to to a configuration file on container. Default path is `/app/application.yml`

## Development Environment
A docker-compose is provide to start a development environnement.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)

# KafkaHQ
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
    -v /tmp/application.conf:/app/application.conf \
    tchiotludo/kafkahq
```
* With `-v /tmp/application.conf` must be an absolute path to configuration file
* Go to http://localhost:8080


### Stand Alone

* Install Java 8
* Download the latest jar on [release page](https://github.com/tchiotludo/kafkahq/releases)
* Create an `application.conf` in the same directory
* Launch the application with `java -jar kafkahq.jar prod`
* Go to http://localhost:8080


## Configuration
Configuration file is a [HOCON configuration](https://github.com/lightbend/config/blob/master/HOCON.md) file with an example below :
```
{
  kafka {
    connections {
      my-cluster-1 {
        properties {
          bootstrap.servers: "kafka:9092"
        }
        registry: "http://schema-registry:8085"
      }
      my-cluster-2 {
        properties {
          bootstrap.servers: "kafka:9093"
          security.protocol: SSL
          ssl.truststore.location: /app/truststore.jks
          ssl.truststore.password: password
          ssl.keystore.location: /app/keystore.jks
          ssl.keystore.password: password
          ssl.key.password: password
        }
      }
    }
  }
}
```

`kafka.connections` is a key value configuration with :
* `key`: must be an url friendly string the identify your cluster (`my-cluster-1` and `my-cluster-2` is the example above)
* `properties`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.
* `registry`: the schema registry url *(optional)*

KafkaHQ docker image support 1 environment variables to handle configuraiton :
* `KAFKAHQ_CONFIGURATION`: a string that contains the full configuration that will be written on /app/configuration.conf on container.


## Development Environment
A docker-compose is provide to start a development environnement.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)

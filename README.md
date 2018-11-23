# KafkaHQ
> Kafka GUI for topics, topics data, consumers group and more...

## Features

- [ ] General
  - [x] Works with modern Kafka cluster (1.0+)
  - [x] Connection on standard or ssl, sasl cluster
  - [x] Multi cluster
- [ ] Topics
  - [x] List
  - [x] Configurations view
  - [x] Partitions view
  - [x] Consumers groups assignemnts view
  - [x] Node leader & assignemnts view
  - [x] Delete a topic
- [ ] Browse Topic data
  - [x] View data, offset, key & timestamp
  - [x] Sort view
  - [x] Filter per partitions
  - [ ] Handle Kafka Registry Deserialization
  - [ ] Filter with a starting offset
  - [ ] Filter with a starting time
- [ ] Consumer Groups (only with kafka internal storage, not with old Zookepper)
  - [x] List with lag, topics assignemnts
  - [x] Partitions view & lag
  - [x] Node leader & assignemnts view
  - [x] Display active and pending consumers groups
  - [x] Delete a consumer group
  - [ ] Update consumer group offsets
- [ ] Nodes
  - [x] List
  - [x] Configurations view


## Installation

First you need a [configuration files](#configuration) in order to configure KafkaHQ connections to Kafka Brokers.

### Docker

```sh
docker run -d \
    -p 8080:8080 \
    -v application.conf:/app/application.conf
    tchiotludo/kafkahq
```

Go to http://localhost:8080


### Stand Alone

* Install Java 8
* Download the latest jar on [release page](TODO)
* Create an `application.conf` in the same directory
* Launch the application with `java kafkahq.jar prod`
* Go to http://localhost:8080


## Configuration
Configuration file is a [HOCON configuration](https://github.com/lightbend/config/blob/master/HOCON.md) file with an example below :
```
{
  kafka {
    connections {
      plaintext {
          bootstrap.servers: "kafka:9092"
      }
      ssl {
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
```

`kafka.connections` is a key value configuration with :
* `key`: must be an url friendly string the identify your cluster
* `value`: all the configurations found on [Kafka consumer documentation](https://kafka.apache.org/documentation/#consumerconfigs). Most important is `bootstrap.servers` that is a list of host:port of your Kafka brokers.

KafkaHQ docker image support 1 environment variables to handle configuraiton :
* `KAFKAHQ_CONFIGURATION`: a string that contains the full configuration that will be written on /app/configuration.conf on container.


## License
Apache 2.0 © [tchiotludo](https://github.com/tchiotludo)
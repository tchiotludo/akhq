# Quick preview

* Download [docker-compose.yml](https://raw.githubusercontent.com/tchiotludo/akhq/master/docker-compose.yml) file
* run `docker-compose pull` to be sure to have the last version of AKHQ
* run `docker-compose up`
* go to [http://localhost:8080](http://localhost:8080)

It will start a Kafka node, a Zookeeper node, a Schema Registry, a Kafka Connect, fill with some sample data, start a consumer
group and a kafka stream & start AKHQ.


## Installation
More standard installation can be found [here](./installation.md)
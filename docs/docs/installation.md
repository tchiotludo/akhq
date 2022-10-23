# Installation

First you need a [configuration files](./configuration/README.md) in order to configure AKHQ connections to Kafka Brokers.

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
* Create a [configuration file](./configuration/README.md)
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
  * `configuration` values will contains all related configuration that you can find in [application.example.yml](https://github.com/tchiotludo/akhq/blob/dev/application.example.yml) and will be store in a `ConfigMap`
  * `secrets` values will contains all sensitive configurations (with credentials) that you can find in [application.example.yml](https://github.com/tchiotludo/akhq/blob/dev/application.example.yml) and will be store in `Secret`
  * Both values will be merged at startup
* Apply the chart:
```sh
helm install --name=akhq-release-name  .
```

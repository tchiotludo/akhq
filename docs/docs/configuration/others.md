# Others

## Server
* `micronaut.server.context-path`: if behind a reverse proxy, path to akhq without trailing slash (optional).
  Example: akhq is behind a reverse proxy with url <http://my-server/akhq>, set `context-path: "/akhq"`.
  Not needed if you're behind a reverse proxy with subdomain <http://akhq.my-server/>

## Kafka admin / producer / consumer default properties
* `akhq.clients-defaults.{{admin|producer|consumer}}.properties`: default configuration for admin producer or
  consumer. All properties from [Kafka documentation](https://kafka.apache.org/documentation/) is available.

## Micronaut configuration
> Since AKHQ is based on [Micronaut](https://micronaut.io/), you can customize configurations (server port, ssl, ...) with [Micronaut configuration](https://docs.micronaut.io/snapshot/guide/configurationreference.html#io.micronaut.http.server.HttpServerConfiguration).
> More information can be found on [Micronaut documentation](https://docs.micronaut.io/snapshot/guide/index.html#config)


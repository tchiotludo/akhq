# Debug & Monitoring

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


## Debugging authentication

Debugging auth can be done by increasing log level on Micronaut that handle most of the authentication part :
```bash
curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:28081/loggers/io.micronaut.security


curl -i -X POST -H "Content-Type: application/json" \
       -d '{ "configuredLevel": "TRACE" }' \
       http://localhost:28081/loggers/org.akhq.configs
```

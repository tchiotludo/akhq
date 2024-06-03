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

### Activating SSL

When using HTTPS for communication, Micronaut will need to get the certificate within Netty. This uses classes of the java.base package which are no longer activated inside the JDK we use. The configuration at the bottom needs to be extended by this environment variable:

```
JDK_JAVA_OPTIONS: --add-exports\=java.base/sun.security.x509\=ALL-UNNAMED
```

```yaml
micronaut:
  server:
    ssl:
      enabled: true
      build-self-signed: true
```

## JSON Logging
In order to configure AKHQ to output log in JSON format, a logback configuration needs to be provided, e.g. `logback.xml`
```
<configuration>
  <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="ch.qos.logback.classic.encoder.JsonEncoder">
    </encoder>
  </appender>

  <root level="debug">
    <appender-ref ref="stdout"/>
  </root>
</configuration>
```
This file then needs to be mounted to `/app/logback.xml` and referenced in `JAVA_OPTS` via `-Dlogback.configurationFile=/app/logback.xml` (see [docker](docker.md) for more information).
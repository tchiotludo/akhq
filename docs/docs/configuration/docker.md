# Docker


## Pass custom Java opts

By default, the docker container will allow a custom JVM options setting the environments vars `JAVA_OPTS`.
For example, if you want to change the default timezone, just add `-e "JAVA_OPTS=-Duser.timezone=Europe/Paris"`

## Enable remote JMX

Remote JMX is not enabled by default. If you need it, pass the JMX flags with `JAVA_OPTS`.
For example, to open JMX on port 8686 for local connections only:

```sh
docker run -d \
    --env JAVA_OPTS="-Dcom.sun.management.jmxremote.port=8686 -Dcom.sun.management.jmxremote.host=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false" \
    -p 8080:8080 \
    -v /tmp/application.yml:/app/application.yml \
    tchiotludo/akhq
```

Don't expose the JMX port without authentication, see
[Monitoring and Management Using JMX Technology](https://docs.oracle.com/en/java/javase/25/management/monitoring-and-management-using-jmx-technology.html).

## Run with another jvm.options file

By default, the docker container will run with a [jvm.options](https://github.com/tchiotludo/akhq/blob/dev/docker/app/jvm.options) file, you can override it with
your own with an Environment Variable. With the `JVM_OPTS_FILE` environment variable, you can override the jvm.options file by passing
the path of your file instead.

Override the `JVM_OPTS_FILE` with docker run:

```sh
docker run -d \
    --env JVM_OPTS_FILE={{path-of-your-jvm.options-file}}
    -p 8080:8080 \
    -v /tmp/application.yml:/app/application.yml \
    tchiotludo/akhq
```

Override the `JVM_OPTS_FILE` with docker-compose:

```yaml
services:
  akhq:
    image: tchiotludo/akhq-jvm:dev
    environment:
      JVM_OPTS_FILE: /app/jvm.options
    ports:
      - "8080:8080"
    volumes:
      - /tmp/application.yml:/app/application.yml
```

If you do not override the `JVM_OPTS_FILE`, the docker container will take the defaults one instead.

The AKHQ docker image supports 4 environment variables to handle configuration :
* `AKHQ_CONFIGURATION`: a string that contains the full configuration in yml that will be written on
  /app/configuration.yml on the container.
* `MICRONAUT_APPLICATION_JSON`: a string that contains the full configuration in JSON format
* `MICRONAUT_CONFIG_FILES`: a path to a configuration file in the container. Default path is `/app/application.yml`
* `CLASSPATH`: additional Java classpath entries. Must be used to specify the location of the TIBCO Avro client library
  jar if a 'tibco' schema registry type is used

## How to mount configuration file

Take care when you mount configuration files to not remove akhq files located on /app.
You need to explicitly mount the `/app/application.yml` and not mount the `/app` directory.
This will remove the AKHQ binaries and give you this error: `
/usr/local/bin/docker-entrypoint.sh: 9: exec: ./akhq: not found`

```yaml
volumeMounts:
- mountPath: /app/application.yml
  subPath: application.yml
  name: config
  readOnly: true

```
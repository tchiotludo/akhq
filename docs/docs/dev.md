
# Development Environment

## Early dev image

You can have access to last feature / bug fix with docker dev image automatically build on tag `dev`
```bash
docker pull tchiotludo/akhq:dev
```

The dev jar is not publish on GitHub, you have 2 solutions to have the `dev` jar :

Get it from docker image
```bash
docker pull tchiotludo/akhq:dev
docker run --rm --name=akhq -it tchiotludo/akhq:dev
docker cp akhq:/app/akhq.jar .
```
Or build it with a `./gradlew shadowJar`, the jar will be located here `build/libs/akhq-*.jar`


## Development Server

A docker-compose is provided to start a development environment.
Just install docker & docker-compose, clone the repository and issue a simple `docker-compose -f docker-compose-dev.yml up` to start a dev server.
Dev server is a java server & webpack-dev-server with live reload.

The configuration for the dev server is in `application.dev.yml`.

## Setup local dev environment on Windows

In case you want to develop for AKHQ on Windows with IntelliJ IDEA without Docker (for any reason) you can follow this
brief guide. For the following steps, please, make sure you meet these requirements:

* OS: Windows (10)
* Kafka (2.6.0) is downloaded and extracted, the installation directory is referred to as $KAFKA_HOME in the latter
* Git is installed and configured
* IntelliJ IDEA (Community Edition 2020.2) with the following plugins installed:
  * Gradle (bundled with IDEA)
  * [Lombok](https://plugins.jetbrains.com/plugin/6317-lombok)

First run a Kafka server locally. Therefore, you need to start Zookeeper first by opening a CMD and doing:
```bash
$KAFKA_HOME\bin\windows\zookeeper-server-start.bat config\zookeper.properties
$KAFKA_HOME\bin\windows\kafka-server-start.bat config\server.properties
```
A zero-config Kafka server should be up and running locally on your machine now. For further details or troubleshooting
see [Kafka Getting started guide](https://kafka.apache.org/quickstart). In the next step we're going to checkout AKHQ from GitHub:
```bash
git clone https://github.com/tchiotludo/akhq.git
```

Open the checked out directory in IntelliJ IDEA. The current version (0.16.0) of AKHQ is built with Java 11. If you
don't have OpenJDK 11 installed already, do the following in IntelliJ IDEA:
* _File > Project Structure... > Platform Settings >
SDKs > + > Download JDK... >_ select a vendor of your choice (but make sure it's version 11)
* download + install. Make sure
that JDK 11 is set under _Project Settings > Project SDK_
* language level is Java 11.
* Now tell Gradle to use Java 11
as well: _File > Settings > Plugins > Build, Execution, Deployment > Build Tools > Gradle > Gradle JVM_: any JDK 11.

To configure AKHQ for using the Kafka server you set up before, edit `application.yml` by adding the following under `akhq`:
```yaml
akhq:
  connections:
    kafka:
      properties:
        bootstrap.servers: "localhost:9092"
```
::: warning
Do not commit this part of `application.yml`. A more secure way to configure your local development Kafka server is
described in the Micronaut doc, chapter ["Application Configuration"](https://docs.micronaut.io/2.5.13/guide/index.html#config).
:::

Now you should be able to build the project with Gradle. Therefore, go to the Gradle view in IDEA, select _Tasks > build >
build_. If an error occurs saying that any filename is too long: move your project directory to a root directory in your
filesystem or as a fix (only for testing purposes) set the argument `-x test` to skip tests temporarily.

To debug a running AKHQ instance, go to the Gradle tab in IntelliJ IDEA, _Tasks > application_ > right click `run` and click
"_Debug(...)_". AKHQ should start up and hit the breakpoints you set in your IDE. Happy developing/debugging!
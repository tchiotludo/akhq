# TIBCO schema registry

If you are using the TIBCO schema registry, you will also need to mount and use the TIBCO Avro client library and its
dependencies. The akhq service in a docker compose file might look something like:

```yaml
  akhq:
    # build:
    #   context: .
    image: tchiotludo/akhq
    volumes:
      - /opt/tibco/akd/repo/1.2/lib/tibftl-kafka-avro-1.2.0-thin.jar:/app/tibftl-kafka-avro-1.2.0-thin.jar
      - /opt/tibco/akd/repo/1.2/lib/deps:/app/deps
    environment:
      AKHQ_CONFIGURATION: |
        akhq:
          connections:
            docker-kafka-server:
              properties:
                bootstrap.servers: "kafka:9092"
              schema-registry:
                type: "tibco"
                url: "http://repo:8081"
              connect:
                - name: "connect"
                  url: "http://connect:8083"
      CLASSPATH: "/app/tibftl-kafka-avro-1.2.0-thin.jar:/app/deps/*"
    ports:
      - 8080:8080
    links:
      - kafka
      - repo
```
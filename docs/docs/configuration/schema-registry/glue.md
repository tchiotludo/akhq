# Glue schema registry
Currently ,glue schema registry support is limited to only de-serialisation of avro/protobuf/json serialized messages.
It can be configured as below.
```yaml
  akhq:
    environment:
      AKHQ_CONFIGURATION: |
        akhq:
          connections:
            docker-kafka-server:
              properties:
                bootstrap.servers: "kafka:9092"
              schema-registry:
                url: "http://schema-registry:8085"
                type: "glue"
                glueSchemaRegistryName: Name of schema Registry
                awsRegion: aws region
              connect:
                - name: "connect"
                  url: "http://connect:8083"
    ports:
      - 8080:8080
    links:
      - kafka
      - repo
```
Please note that authentication is done using aws default credentials provider.

Url key is required to not break the flow.
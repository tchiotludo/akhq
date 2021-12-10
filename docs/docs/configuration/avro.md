

# Avro deserialization

Avro messages using Schema registry are automatically decoded if the registry is configured (see [Kafka cluster](../configuration/brokers.md)).

You can also decode raw binary Avro messages, that is messages encoded directly with [DatumWriter](https://avro.apache.org/docs/current/api/java/org/apache/avro/io/DatumWriter.html) without any header.
You must provide a `schemas-folder` and mappings which associate a `topic-regex` and a schema file name. The schema can be
specified either for message keys with `key-schema-file` and/or for values with `value-schema-file`.

Here is an example of configuration:

```
akhq:
  connections:
    kafka:
      properties:
        # standard kafka properties
        avro-raw:
          schemas-folder: "/app/avro_schemas"
          topics-mapping:
            - topic-regex: "album.*"
              value-schema-file: "Album.avsc"
            - topic-regex: "film.*"
              value-schema-file: "Film.avsc"
            - topic-regex: "test.*"
              key-schema-file: "Key.avsc"
              value-schema-file: "Value.avsc"
```

Examples can be found in [tests](https://github.com/tchiotludo/akhq/tree/dev/src/main/java/org/akhq/utils).

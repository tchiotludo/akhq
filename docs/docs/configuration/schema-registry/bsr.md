# Buf Schema Registry
Integration with Buf Schema Registry allows you to deserialize protobuf messages for which the schema is managed in
Buf Schema Registry. If using Bufstream or using headers injected into Kafka records with the bsr message type and 
commit id, the AKHQ config does not need the topic mappings to be configured and only the schema registry host and 
bsr token is required. However, where records do not have the headers, the topic mappings can be configured as follows.


```yaml
akqh:
  environment:
    AKHQ_CONFIGURATION: | 
      akhq:
        connections:
          bsr-test-cluster:
            properties:
              bootstrap.servers: "localhost:9092"  # Your Kafka broker
            schema-registry:
              type: "bsr"
              bsr-host: "bufbuild.internal"
              bsr-token: "c7036237f576ccbe48b07fe5b99f12a0f725235aa4b75da04d357d2bbaebcb19"
            deserialization:
              protobuf:
                topics-mapping:
                  # Config-based example (optional if using headers)
                  - topic-regex: "test.*"
                    bsr-message-type: "bufstream.demo.v1.EmailUpdated"  # Replace with your message type
                    # bsr-commit: "commit-id"  # Optional: specific commit/version
```


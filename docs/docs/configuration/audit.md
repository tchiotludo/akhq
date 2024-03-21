# Audit configuration

akhq can be configured to emit audit event to a kafka cluster for the following user actions:

- Topic level
  - Topic creation
  - Topic configuration change
  - Topic partition increase
  - Topic deletion
- Consumer group level
  - Update offsets
  - Delete offsets
  - Delete consumer group
- Schema registry
  - Create new schema for a subject
  - Update existing schema for a subject
  - Change compatibility level of a subject
  - Delete a subject
- Kafka connect
  - Create new connector
  - Update existing connector
  - Pause and resume connector
  - Restart connector or task
  - Delete connector

The following configuration is an example of akhq with audit turned ON. All events mentioned above
will be sent to the `my-audit-cluster-plain-text` cluster in the topic `audit`.

```yaml
akhq:

  connections:
    my-cluster-plain-text:
      properties:
        bootstrap.servers: "kafka:9092"
    my-audit-cluster-plain-text:
      properties:
        bootstrap.servers: "audit:9092"

  audit:
    enabled: true
    cluster-id: my-audit-cluster-plain-text
    topic-name: audit
```

To be able to identify the user who performed these actions, security must be turned ON (otherwise the userName field is
left empty).
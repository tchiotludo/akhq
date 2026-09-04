# Azure Schema Registry

AKHQ supports deserialization of Avro messages produced to Azure Event Hub using the
[Azure Schema Registry](https://learn.microsoft.com/en-us/azure/event-hubs/schema-registry-overview).

Currently, Azure Schema Registry support is limited to the deserialization of Avro messages. JSON and Protobuf
schemas are not supported for this registry type. Also, since Azure Event Hub does not use the record key to store
schema information, key schema lookup is not supported and will always return `null`.

## Configuration

* `type`: must be set to `azure`
* `url`: the fully qualified namespace of your Event Hub / Schema Registry (e.g. `my-eventhub.servicebus.windows.net`).
  The `.servicebus.windows.net` suffix is appended automatically if not already present.
* `properties.schema.group`: the name of the schema group in Azure Schema Registry that contains the schemas for this
  cluster.

Authentication against the Azure Schema Registry is done using the
[`DefaultAzureCredential`](https://learn.microsoft.com/en-us/java/api/overview/azure/identity-readme) chain provided
by the Azure Identity SDK (environment variables, managed identity, Azure CLI, etc.). It reuses the same credential
chain as the [Azure Event Hub OAuth2 broker authentication](../brokers.md#oauth2-authentication-for-azure-event-hub).

```yaml
akhq:
  connections:
    azure-eventhub:
      properties:
        bootstrap.servers: "my-eventhub.servicebus.windows.net:9093"
        security.protocol: SASL_SSL
        sasl.mechanism: OAUTHBEARER
        sasl.jaas.config: >
          org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
          oauth.scope="kafka-cluster";
        sasl.login.callback.handler.class: "org.akhq.azure.KafkaOAuth2AuthenticateCallbackHandler"
        ssl.endpoint.identification.algorithm: https
      schema-registry:
        type: "azure"
        url: "my-eventhub.servicebus.windows.net"
        properties:
          schema.group: "my-schema-group"
```

The client used to connect to the Azure Schema Registry is authenticated the same way as the Kafka connection
(`DefaultAzureCredentialBuilder`), so no additional `basic-auth-username` / `basic-auth-password` is required or
supported for this registry type.

# Apicurio schema registry

AKHQ natively supports [Apicurio Registry](https://www.apicur.io/registry/) (v3 Core API `/apis/registry/v3`) as a schema registry provider.

## Supported features

- Browse, search, create, update, and delete schemas (Avro, JSON Schema, and Protobuf), including schema references and version management
- Multi-group support across all Apicurio groups, or scoped to a single group via `apicurio-group-id`
- Manage `COMPATIBILITY` rules per artifact (with automatic fallback to group and global rules)
- Deserialize Kafka message keys and values produced by Apicurio SerDes:
  - 4-byte IDs (Apicurio Registry v3 default) and 8-byte IDs (Apicurio Registry v2 / Debezium legacy format)
  - `contentId` and `globalId` identifiers
  - Kafka header-based references (`apicurio.key.*` / `apicurio.value.*` headers, including `globalId`, `contentId`, `contentHash`, `groupId`/`artifactId`/`version`, Avro `encoding`, Protobuf `msgType`, and inline schemas)
  - Avro (binary and JSON encoding), JSON Schema, and Protobuf (Apicurio `Ref`, Confluent message indexes, and `msgType` header)
- Serialize and produce Avro, JSON Schema, and Protobuf messages from AKHQ
- Authentication via Anonymous, Basic Auth, OAuth2/OIDC client credentials, Bearer token, and custom TLS/mTLS

## Basic configuration

```yaml
akhq:
  connections:
    my-kafka-cluster:
      properties:
        bootstrap.servers: "kafka:9092"
      schema-registry:
        url: "http://apicurio-registry:8080/apis/registry/v3"
        type: "apicurio"
```

If `/apis/registry/v3` is omitted from `url`, it is appended automatically.

## Groups

By default, AKHQ lists artifacts from **all** Apicurio groups:

- Artifacts in the `default` group appear as `<artifactId>` (for example, `orders-value`).
- Artifacts in custom groups appear as `<groupId>:<artifactId>` (for example, `com.acme.orders:orders-value`).
- When creating a schema from AKHQ, you can enter `<groupId>:<artifactId>` to target a specific group, or `<artifactId>` to use the `default` group.

To restrict AKHQ to a single group and make it the default group when creating schemas, set `apicurio-group-id`:

```yaml
akhq:
  connections:
    my-kafka-cluster:
      properties:
        bootstrap.servers: "kafka:9092"
      schema-registry:
        url: "http://apicurio-registry:8080/apis/registry/v3"
        type: "apicurio"
        apicurio-group-id: "com.acme.orders"
```

## Authentication and TLS

### Basic authentication

```yaml
      schema-registry:
        url: "https://apicurio-registry:8443/apis/registry/v3"
        type: "apicurio"
        basic-auth-username: "registry-user"
        basic-auth-password: "registry-password"
```

### OAuth2 / OIDC client credentials

```yaml
      schema-registry:
        url: "https://apicurio-registry:8443/apis/registry/v3"
        type: "apicurio"
        properties:
          apicurio.auth.token-endpoint: "https://keycloak.example.com/realms/apicurio/protocol/openid-connect/token"
          apicurio.auth.client-id: "akhq-client"
          apicurio.auth.client-secret: "secret"
          # apicurio.auth.scope: "optional-scope"
```

### Bearer token

```yaml
      schema-registry:
        url: "https://apicurio-registry:8443/apis/registry/v3"
        type: "apicurio"
        properties:
          apicurio.auth.bearer-token: "<access-token>"
```

### TLS / mTLS

```yaml
      schema-registry:
        url: "https://apicurio-registry:8443/apis/registry/v3"
        type: "apicurio"
        properties:
          apicurio.tls.truststore.location: "/app/truststore.jks"
          apicurio.tls.truststore.password: "changeit"
          apicurio.tls.truststore.type: "JKS" # JKS, PKCS12, or PEM
          # Optional client certificate (mTLS):
          apicurio.tls.keystore.location: "/app/keystore.p12"
          apicurio.tls.keystore.password: "changeit"
          apicurio.tls.keystore.type: "PKCS12"
```

## Advanced properties

All optional properties are configured under `schema-registry.properties`:

| Property | Default | Description |
| --- | --- | --- |
| `apicurio.registry.group-id` | *(all groups)* | Alternative to `apicurio-group-id`. Restricts listing to one group and uses it as default group. |
| `apicurio.registry.use-id` | `auto` | Schema identifier type: `auto`, `contentId`, or `globalId`. In `auto` mode, 4-byte wire IDs prefer `contentId` (v3 default) and 8-byte wire IDs prefer `globalId` (v2/Debezium default), falling back on 404. |
| `apicurio.registry.id-handler` | `auto` | Wire ID size: `auto` (optimistic 4-byte/8-byte detection on read, 4-byte on write), `4byte`, or `8byte`. |
| `apicurio.registry.artifact-types` | `AVRO,JSON,PROTOBUF` | Comma-separated Apicurio artifact types shown in AKHQ (`*` for all types). |
| `apicurio.registry.serde.send-type-ref` | `true` | Write Apicurio length-delimited `Ref` message when producing Protobuf records. |
| `apicurio.registry.serde.send-indexes` | `false` | Write Confluent-compatible message indexes when producing Protobuf records. |
| `apicurio.registry.cache.max-size` | `1000` | Maximum number of cached schemas for record deserialization. |
| `apicurio.registry.cache.ttl-seconds` | `300` | Cache TTL (in seconds) for mutable coordinate lookups (for example `branch=latest`). |
| `apicurio.tls.trust-all` | `false` | Disable TLS certificate verification (development/testing only). |
| `apicurio.tls.verify-host` | `true` | Enable or disable TLS hostname verification. |
| `apicurio.proxy.host` / `apicurio.proxy.port` | *(none)* | Optional HTTP/HTTPS proxy configuration (with optional `apicurio.proxy.username` / `apicurio.proxy.password`). |
| `apicurio.http.connect-timeout-ms` / `apicurio.http.read-timeout-ms` | *(SDK default)* | Connect and read idle timeouts in milliseconds. |

Header names used when deserializing header-encoded messages can also be overridden with the standard Apicurio properties (for example `apicurio.registry.headers.value.globalId.name`, `apicurio.registry.headers.value.contentId.name`, `apicurio.registry.headers.value.groupId.name`, `apicurio.registry.headers.value.artifactId.name`, `apicurio.registry.headers.value.version.name`).

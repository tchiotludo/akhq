# Api
An **experimental** api is available that allow you to fetch all the exposed on AKHQ through api.

Take care that this api is **experimental** and **will** change in a future release.
Some endpoints expose too many data and is slow to fetch, and we will remove
some properties in a future in order to be fast.

Example: List topic endpoint expose log dir, consumer groups, offsets. Fetching all theses
is slow for now, and we will remove these in a future.

You can discover the api endpoint here :
* `/api`: a [RapiDoc](https://mrin9.github.io/RapiDoc/) webpage that document all the endpoints.
* `/swagger/akhq.yml`: a full [OpenApi](https://www.openapis.org/) specifications files

## MCP endpoint

AKHQ also exposes an MCP JSON-RPC endpoint on `POST /mcp`.

Authentication is the same as the other AKHQ API endpoints:

* If you already authenticated in the UI, send the same session cookie.
* For programmatic clients, send a JWT as `Authorization: Bearer <token>`.

Authorization is also the same model as classic endpoints:

* Request must be authenticated.
* Caller must have `TOPIC_DATA` / `READ` permission on the target cluster.
* For `tools/call`, caller must also be allowed on the requested topic name pattern.

Current tools:

* `akhq.find_message_in_topic`: search message(s) and return message overviews (`partition`, `offset`, `timestamp`, `key`, short value preview).
* `akhq.get_message_detail`: fetch one exact message with full `value` payload and all headers.
* `akhq.get_topic_last_record_timestamp`: return the latest record timestamp across every partition of one topic. It returns `found: false` with a null timestamp when the topic has no records.

For every search literal, use the matching `*MatchType` field to select `CONTAINS` (the default), `EQUALS`, or `NOT_CONTAINS`. Do not append AKHQ's internal `_C`, `_E`, or `_N` suffixes to a literal.

Timestamps must be ISO-8601 strings, such as `2026-09-14T10:00:00Z`. Numeric epoch-millisecond timestamps are not part of the MCP input schema.

### Request shape

Current tool methods use an argument envelope, so `params.arguments` contains an inner `arguments` object.

### OAuth 2.0 for MCP clients

AKHQ can authenticate MCP clients with OAuth 2.0 access tokens issued by a standards-compliant OIDC provider. This is separate from AKHQ's browser-login OIDC configuration: the MCP client obtains an access token directly from the provider and sends it in the `Authorization` header.

```yaml
micronaut:
  security:
    token:
      bearer:
        enabled: false # AKHQ validates MCP bearer tokens separately from UI cookies.

akhq:
  security:
    mcp-oauth:
      enabled: true
      authorization-server: https://identity.example.com/realms/akhq
      issuer: https://identity.example.com/realms/akhq
      jwks-url: https://identity.example.com/realms/akhq/protocol/openid-connect/certs
      audience: akhq-mcp
      # Set this when AKHQ is behind a proxy that changes its public URL.
      resource: https://akhq.example.com/mcp
      username-claim: preferred_username
      groups-claim: groups
      required-scope: akhq.mcp.read
      default-group: topic-reader
      groups:
        - name: mcp-topic-readers
          groups: [topic-reader]
```

When enabled, AKHQ serves RFC 9728 protected-resource metadata at `/.well-known/oauth-protected-resource` and requires `Authorization: Bearer <access-token>` for `/mcp`. A missing or invalid MCP token receives `401 Unauthorized` with a `WWW-Authenticate` challenge pointing to that metadata; authorization failures after authentication return `403 Forbidden`. AKHQ validates the access token's signature against `jwks-url`, as well as its issuer, audience, expiry, and subject. This validation is scoped to `/mcp`, so AKHQ's existing UI cookie authentication remains unchanged. Its `groups-claim` values are mapped to AKHQ groups using the configured `groups`, `users`, and `default-group` mappings.

The MCP OAuth implementation separates request matching, token validation, and claim resolution. It currently validates JWT access tokens through JWKS; the token-validator interface allows adding opaque-token introspection without changing MCP routing or AKHQ authorization.

Register the Copilot App as a public OIDC client with Authorization Code + PKCE, configure the exact redirect URI shown by the Copilot App at the provider, and request an access token for the configured `audience` and `required-scope`. Enter that registered client ID in Copilot. The authorization server must expose standard OIDC discovery, authorization, token, and JWKS endpoints.

### Example request (`akhq.find_message_in_topic`)

```bash
curl -X POST "http://localhost:8081/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "jsonrpc": "2.0",
    "id": "call-1",
    "method": "tools/call",
    "params": {
      "name": "akhq.find_message_in_topic",
      "arguments": {
        "arguments": {
          "cluster": "local",
          "topic": "my-topic",
          "searchByValue": "needle",
          "searchByValueMatchType": "CONTAINS",
          "maxMatches": 1
        }
      }
    }
  }'
```

### Example request (`akhq.get_message_detail`)

```bash
curl -X POST "http://localhost:8081/mcp" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "jsonrpc": "2.0",
    "id": "call-2",
    "method": "tools/call",
    "params": {
      "name": "akhq.get_message_detail",
      "arguments": {
        "arguments": {
          "cluster": "local",
          "topic": "my-topic",
          "partition": 0,
          "offset": 42
        }
      }
    }
  }'
```

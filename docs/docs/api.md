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

The MCP server is **disabled by default** and must be enabled explicitly:

```yaml
akhq:
  mcp:
    enabled: true
```

Enable it only on an instance that has an authentication mechanism configured. AKHQ ships with
`micronaut.security.enabled: false`, and on such an instance the MCP endpoint would be reachable anonymously:
AKHQ logs a warning at startup in that case.

Additional `akhq.mcp` properties:

| Property | Default | Description |
| --- | --- | --- |
| `allowed-origins` | empty | Browser origins (for example `https://mcp-client.example.com`) allowed to call `/mcp`. Requests without an `Origin` header, as sent by native MCP clients, are always accepted; any other origin is rejected with `403` to protect against DNS rebinding. |
| `search-timeout` | `30s` | Maximum duration of an `akhq.find_message_in_topic` search. When reached, the tool returns the matches found so far with a notice. |
| `max-result-length` | `100000` | Size budget, in characters, of the values returned by an `akhq.find_message_in_topic` call. Above it, the longest values are truncated evenly (never below 200 characters), or fewer projected matches are returned with a cursor to the next ones. |

Authentication is the same as the other AKHQ API endpoints:

* If you already authenticated in the UI, send the same session cookie.
* For programmatic clients, send a JWT as `Authorization: Bearer <token>`.

When MCP OAuth 2.0 is enabled (see below), those two options no longer apply to `/mcp`: the endpoint then accepts
**only** access tokens issued by the configured authorization server, and an AKHQ session cookie or an AKHQ-issued
JWT is rejected with `401`. The rest of the API keeps accepting them.

Authorization is also the same model as classic endpoints:

* Request must be authenticated.
* Caller must have `TOPIC_DATA` / `READ` permission on the target cluster (`TOPIC` / `READ` for `akhq.search_topics`).
* For `tools/call`, caller must also be allowed on the requested topic name pattern. `akhq.search_topics` only returns the topics matching the caller's patterns.

Current tools:

* `akhq.search_topics`: list the topics of a cluster (`name`, `partitions`), sorted by name, internal topics included. Optional space-separated `search` terms must all appear in the name (case insensitive). `maxResults` defaults to 50 and is capped at 200; `truncated` and `totalMatches` tell when more topics matched.
* `akhq.find_message_in_topic`: search message(s) and return the matches (`partition`, `offset`, `timestamp`, `key`, `value`). Values are returned in full within the `max-result-length` budget. `fields` extracts dot-separated paths from JSON values instead (up to 500 matches per call), and `hasMore`/`nextCursor` with the `after` argument page through all the matches.
* `akhq.get_message_detail`: fetch one exact message with full `value` payload and all headers.
* `akhq.get_topic_last_record_timestamp`: return the latest record timestamp across every partition of one topic. It returns `found: false` with a null timestamp when the topic has no records.

For every search literal, use the matching `*MatchType` field to select `CONTAINS` (the default), `EQUALS`, or `NOT_CONTAINS`. Do not append AKHQ's internal `_C`, `_E`, or `_N` suffixes to a literal.

Timestamps must be ISO-8601 strings, such as `2026-09-14T10:00:00Z`. Numeric epoch-millisecond timestamps are not part of the MCP input schema.

Invalid arguments and missing permissions are returned as tool execution errors: a regular result with `isError: true` and the reason as text, so the language model can correct its call. Unknown tools, malformed requests, and unexpected failures are returned as JSON-RPC errors, without internal details.

### Request shape

Current tool methods use an argument envelope, so `params.arguments` contains an inner `arguments` object.

### OAuth 2.0 for MCP clients

AKHQ can authenticate MCP clients with OAuth 2.0 access tokens issued by a standards-compliant OIDC provider. This is separate from AKHQ's browser-login OIDC configuration: the MCP client obtains an access token directly from the provider and sends it in the `Authorization` header.

MCP OAuth requires `micronaut.security.enabled: true`; AKHQ refuses to start otherwise.

```yaml
akhq:
  security:
    mcp-oauth:
      enabled: true
      issuer: https://identity.example.com/realms/akhq
      jwks-url: https://identity.example.com/realms/akhq/protocol/openid-connect/certs
      # Expected `aud` claim of the access tokens, ideally AKHQ's resource URL.
      audience: https://akhq.example.com/mcp
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

`issuer`, `jwks-url` and `audience` are mandatory: AKHQ validates them at startup and fails fast with an explicit
message when one is missing or is not an absolute URL. This validation aborts the startup of the whole application,
web UI included, so an instance never runs with a half configured MCP OAuth setup. Set
`akhq.security.mcp-oauth.enabled: false` to disable both the feature and its validation. `authorization-server` defaults to `issuer`, and the following properties are optional:

| Property | Default | Description |
| --- | --- | --- |
| `authorization-server` | `issuer` | Authorization server advertised in the protected-resource metadata. |
| `resource` | Public AKHQ origin + context path + MCP endpoint | Resource identifier advertised in the metadata and the `WWW-Authenticate` challenge. |
| `jws-algorithms` | Every RSA, EC and EdDSA algorithm of the JWK set | Restricts the accepted token signature algorithms. |
| `jwks-connect-timeout` | `5s` | Connect timeout for the JWKS endpoint. |
| `jwks-read-timeout` | `5s` | Read timeout for the JWKS endpoint. |

When enabled, AKHQ serves RFC 9728 protected-resource metadata at `/.well-known/oauth-protected-resource/mcp` (and at `/.well-known/oauth-protected-resource`, both under the configured context path) and requires `Authorization: Bearer <access-token>` for `/mcp`. A missing or invalid MCP token receives `401 Unauthorized` with a `WWW-Authenticate` challenge pointing to that metadata; authorization failures after authentication return `403 Forbidden`. AKHQ validates the access token's signature against `jwks-url`, as well as its issuer, audience, expiry, and subject. Tokens typed `JWT` or `at+jwt` (RFC 9068), or untyped, are accepted. `audience` must match the `aud` claim of the tokens. Never use an OIDC client ID, otherwise ID tokens issued to that client would also be accepted. We recommend using AKHQ's resource URL (the `resource` value advertised in the metadata, such as `https://akhq.example.com/mcp`): MCP clients send it to the provider as the RFC 8707 `resource` parameter, and providers supporting it put it in `aud` without extra configuration. Providers ignoring this parameter, such as Keycloak, need an audience mapper on the MCP client that adds this same value to its access tokens. Only asymmetric signature algorithms are accepted, so a token signed with `none` or with a symmetric key is rejected. This validation is scoped to `/mcp` and takes precedence over AKHQ's own JWT validation there, so no change to `micronaut.security.token.*` is needed and AKHQ's existing UI authentication remains unchanged. Its `groups-claim` values are mapped to AKHQ groups using the configured `groups`, `users`, and `default-group` mappings.

The MCP OAuth implementation separates request matching, token validation, and claim resolution. It currently validates JWT access tokens through JWKS; the token-validator interface allows adding opaque-token introspection without changing MCP routing or AKHQ authorization.

For interactive MCP clients (IDE or desktop AI assistants), register the client at the provider as a public OIDC client with Authorization Code + PKCE, allow the exact redirect URI used by that MCP client, and make sure its access tokens carry the configured `audience` and `required-scope`. Then configure that client ID in the MCP client. The authorization server must expose standard OIDC discovery, authorization, token, and JWKS endpoints.

For programmatic (non-interactive) MCP clients, register a confidential client at the provider and use the
client-credentials grant to obtain an access token with the same `audience` and `required-scope`, then send it as a
bearer token to `/mcp`.

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

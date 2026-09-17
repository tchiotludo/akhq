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

For every search literal, use the matching `*MatchType` field to select `CONTAINS` (the default), `EQUALS`, or `NOT_CONTAINS`. Do not append AKHQ's internal `_C`, `_E`, or `_N` suffixes to a literal.

Timestamps must be ISO-8601 strings, such as `2026-09-14T10:00:00Z`. Numeric epoch-millisecond timestamps are not part of the MCP input schema.

### Request shape

Current tool methods use an argument envelope, so `params.arguments` contains an inner `arguments` object.

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

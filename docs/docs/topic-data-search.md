# Topic data search

The topic data page searches messages with groups of conditions. The same model is used by the UI, the REST API and the
MCP tool `akhq.find_message_in_topic`.

## Simple and advanced search

* **Simple search**: one field and one operator applied to several terms, joined with *all* (AND) or *any* (OR).
* **Advanced filters**: several groups of conditions. Each condition has a field, an operator and a value. Conditions of a
  group are joined with AND/OR, and groups are joined together with AND/OR.

Joins are applied strictly left to right: `A OR B AND C` is evaluated as `(A OR B) AND C`. Use groups to express other
combinations.

| Field | Searches in |
|-------|-------------|
| `value` / `key` | the message value / key |
| `headerKey` / `headerValue` | the header keys / values (any header may match) |
| `keySubject` / `valueSubject` | the schema registry subject of the key / value |

| Operator | Meaning |
|----------|---------|
| `contains` | the text appears anywhere (case insensitive) |
| `equals` | the whole text is equal (case insensitive) |
| `notContains` | the text does not appear |
| `matchesRegex` | the regular expression finds a hit **anywhere** in the text (not a full-string match) |

The literal `null` targets null keys, values or headers, as in the legacy search.

## Regular expressions

Regular expressions use the [RE2 syntax](https://github.com/google/re2/wiki/Syntax) (no backreferences or lookarounds),
which guarantees a search time linear in the message size. Matching is case insensitive and not anchored: use `^` and `$`
for a full match. For example `error-\d+` matches `error-1` and `an error-42 occurred`. Invalid expressions are rejected before the
search starts.

## Sort

A search (or a timestamp filter) always scans from the oldest message: the *Newest* sort is not available with them, and the
UI switches to *Oldest* automatically.

## Sharing a search

The applied search is stored in the page URL as `searchQuery=v1.<payload>`, a compact base64url encoding (deflated when
smaller) of the expression, so that a link reproduces the same search. Expressions whose encoding exceeds 2000 characters
are still executed but not written in the URL, and the UI warns that the search cannot be shared. Old links using
`searchByKey`, `searchByValue`, `searchByHeaderKey` and `searchByHeaderValue` keep working.

## Limits

| Property | Default |
|----------|---------|
| `akhq.topic-data.search.max-groups` | 10 |
| `akhq.topic-data.search.max-conditions` | 50 |
| `akhq.topic-data.search.max-value-length` | 1000 |

See the [configuration](configuration/akhq.md#topic-data).

## REST API

* `POST /api/{cluster}/topic/{topic}/data/search` streams the results as server-sent events.
  Query parameters: `after`, `partition`, `sort`, `timestamp`, `endTimestamp`, `size`.
* `POST /api/{cluster}/topic/{topic}/data/download` downloads the matching messages.

Both take the expression as JSON body:

```json
{
  "version": 1,
  "groups": [
    {"conditions": [
      {"field": "value", "operator": "matchesRegex", "value": "error-\\d+"},
      {"join": "AND", "field": "key", "operator": "contains", "value": "customer-42"}
    ]},
    {"join": "OR", "conditions": [
      {"field": "headerValue", "operator": "equals", "value": "checkout"}
    ]}
  ]
}
```

The first group and the first condition of a group have no `join`; all the others must have one (`AND` or `OR`). Invalid
expressions return `400` with the reason. The legacy `GET` endpoints with `searchBy*` parameters remain supported.

## MCP

`akhq.find_message_in_topic` accepts the same object as `expression` instead of the `searchBy*` literal arguments (the two
cannot be combined). See the [API page](api.md).

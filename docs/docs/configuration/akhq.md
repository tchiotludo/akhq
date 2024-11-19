
# AKHQ configuration

## Pagination
* `akhq.pagination.page-size` number of topics per page (default : 25)

## Avro Serializer
* `akhq.avro-serializer.json.serialization.inclusions` is list of ObjectMapper serialization inclusions that is used for converting Avro message to more
  readable Json format in the UI. Supports Enums of JsonInclude.Include from Jackson library

## Topic List
* `akhq.topic.internal-regexps` is list of regexp to be considered as internal (internal topic can't be deleted or updated)
* `akhq.topic.stream-regexps` is list of regexp to be considered as internal stream topic

## Topic creation default values

These parameters are the default values used in the topic creation page.

* `akhq.topic.replication` Default number of replica to use
* `akhq.topic.partition` Default number of partition

## Topic Data
* `akhq.topic-data.size`: max record per page (default: 50)
* `akhq.topic-data.poll-timeout`: The time, in milliseconds, spent waiting in poll if data is not available in the buffer (default: 1000).
* `akhq.topic-data.kafka-max-message-length`: Max message length allowed to send to UI when retrieving a list of records (dafault: 1000000 bytes).

## Ui Settings
### Topics
* `akhq.ui-options.topic.default-view` is default list view (ALL, HIDE_INTERNAL, HIDE_INTERNAL_STREAM, HIDE_STREAM) (default: HIDE_INTERNAL)
* `akhq.ui-options.topic.skip-consumer-groups` hide consumer groups columns on topic list
* `akhq.ui-options.topic.skip-last-record` hide the last records on topic list
* `akhq.ui-options.topic.show-all-consumer-groups` expand lists of consumer groups on topic list

### Topic Data
* `akhq.ui-options.topic-data.sort`: default sort order (OLDEST, NEWEST) (default: OLDEST)

### Inject some css or javascript
* `akhq.html-head`: Append some head tags on the webserver application
Mostly useful in order to inject some css or javascript to customize the web application.

Examples, add a environment information on the left menu:
```yaml
akhq:
  html-head: |
    <style type="text/css">
      .logo-wrapper:after {
        display: block;
        content: "Local";
        position: relative;
        text-transform: uppercase;
        text-align: center;
        color: white;
        margin-top: 10px;
      }
    </style>
```

## Custom HTTP response headers
To add headers to every response please add the headers like in following example:
```yaml
akhq:
  server:
    customHttpResponseHeaders:
      - name: "Content-Security-Policy"
        value: "default-src 'none'; frame-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self'; style-src 'self'; frame-ancestors 'self'; form-action 'self'; upgrade-insecure-requests"
      - name: "X-Permitted-Cross-Domain-Policies"
        value: "none"
```

## Data Masking
If you want to hide some data in your records, there are two approaches.

### Regex Masking
You can use regex masking - configure this with the following filters.
These will be applied to all record values and keys.

```yaml
akhq:
  security:
    data-masking:
      mode: regex # Note - this is not explicitly required as regex is the 'default' masker that gets applied for backwards compatibility
      filters:
        - description: "Masks value for secret-key fields"
          search-regex: '"(secret-key)":".*"'
          replacement: '"$1":"xxxx"'
        - description: "Masks last digits of phone numbers"
          search-regex: '"([\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?)[0-9]{4,6}"'
          replacement: '"$1xxxx"'
```

### JSON Masking
This is useful for records which are interpreted as JSON on deserialization to strings - for example,
Avro records, or normal JSON payloads.
These can be configured per-topic, and you can select distinct fields to mask/unmask.
There are two JSON masking modes: `json_show_by_default` and `json_mask_by_default`.

#### Show by default config
This means, by default, nothing is masked. If you wish to mask data this way, you can:
- Set a value in `akhq.security.data-masking.jsonMaskReplacement` (this defaults to `xxxx`)
- Set `akhq.security.data-masking.mode` to `json_show_by_default`
- Add as many filters as desired under `akhq.security.data-masking.json-filters` (see below for an example) to select fields you want to *mask*

NOTES: Only one filter per topic is currently supported. If you are using `RecordNameStrategy` on a topic with multiple record types,
there is (currently) no way to distinguish between different records, so any records which have the JSON field at the
selected path(s) will be masked. If you have a misconfiguration and have defined multiple filters per topic, only the first will
actually be selected.


```yaml
akhq:
  security:
    data-masking:
      mode: json_show_by_default
      jsonMaskReplacement: xxxx
      json-filters:
        - description: Mask sensitive values
          topic: users
          keys:
            - name
            - dateOfBirth
            - address.firstLine
            - address.town
```

Given a record on `users` that looks like:

```json
{
  "specialId": 123,
  "status": "ACTIVE",
  "name": "John Smith",
  "dateOfBirth": "01-01-1991",
  "address": {
    "firstLine": "123 Example Avenue",
    "town": "Faketown",
    "country": "United Kingdom"
  },
  "metadata": {
    "trusted": true,
    "rating": "10",
    "notes": "All in good order"
  }
}
```

With the above configuration, it will appear as:

```json
{
  "specialId": 123,
  "status": "ACTIVE",
  "name": "xxxx",
  "dateOfBirth": "xxxx",
  "address": {
    "firstLine": "xxxx",
    "town": "xxxx",
    "country": "United Kingdom"
  },
  "metadata": {
    "trusted": true,
    "rating": "10",
    "notes": "All in good order"
  }
}
```

### Mask by default config
This means, by default, everything is masked.
This is useful in production scenarios where data must be carefully selected and made available to
users of AKHQ - usually this is for regulatory compliance of personal/sensitive information.
If you wish to mask data this way, you can:
- Set a value in `akhq.security.data-masking.jsonMaskReplacement` (this defaults to `xxxx`)
- Set `akhq.security.data-masking.mode` to `json_mask_by_default`
- Add as many filters as desired under `akhq.security.data-masking.json-filters` (see below for an example) to select fields you want to *show*

NOTES: Only one filter per topic is currently supported. If you are using `RecordNameStrategy` on a topic with multiple record types,
there is (currently) no way to distinguish between different records, so any records which have the JSON field at the
selected path(s) will be shown. If you have a misconfiguration and have defined multiple filters per topic, only the first will
actually be selected.
```yaml
akhq:
  security:
    data-masking:
      mode: json_mask_by_default
      jsonMaskReplacement: xxxx
      json-filters:
        - description: Unmask non-sensitive values
          topic: users
          keys:
            - specialId
            - status
            - address.country
            - metadata.trusted
            - metadata.rating
```

Given a record on `users` that looks like:

```json
{
  "specialId": 123,
  "status": "ACTIVE",
  "name": "John Smith",
  "dateOfBirth": "01-01-1991",
  "address": {
    "firstLine": "123 Example Avenue",
    "town": "Faketown",
    "country": "United Kingdom"
  },
  "metadata": {
    "trusted": true,
    "rating": "10",
    "notes": "All in good order"
  }
}
```

With the above configuration, it will appear as:

```json
{
  "specialId": 123,
  "status": "ACTIVE",
  "name": "xxxx",
  "dateOfBirth": "xxxx",
  "address": {
    "firstLine": "xxxx",
    "town": "xxxx",
    "country": "United Kingdom"
  },
  "metadata": {
    "trusted": true,
    "rating": "10",
    "notes": "xxxx"
  }
}
```

### No masking required
You can set `akhq.security.data-masking.mode` to `none` to disable masking altogether.

## Audit
If you want to audit user action that modified topics or consumer group state, you can configure akhq to send
audit events to a pre-configured cluster:

```yaml
akhq:
  audit:
    enabled: true
    cluster-id: my-audit-cluster-plain-text
    topic-name: audit
```
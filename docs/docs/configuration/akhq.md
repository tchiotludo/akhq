
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
If you want to hide some data in your records, you can configure this with the following filters.
These will be applied to all record values and keys.
```yaml
akhq:
  security:
    data-masking:
      filters:
        - description: "Masks value for secret-key fields"
          search-regex: '"(secret-key)":".*"'
          replacement: '"$1":"xxxx"'
        - description: "Masks last digits of phone numbers"
          search-regex: '"([\+]?[(]?[0-9]{3}[)]?[-\s\.]?[0-9]{3}[-\s\.]?)[0-9]{4,6}"'
          replacement: '"$1xxxx"'
```
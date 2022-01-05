
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

## Ui Settings
### Topics
* `akhq.ui-options.topic.default-view` is default list view (ALL, HIDE_INTERNAL, HIDE_INTERNAL_STREAM, HIDE_STREAM) (default: HIDE_INTERNAL)
* `akhq.ui-options.topic.skip-consumer-groups` hide consumer groups columns on topic list
* `akhq.ui-options.topic.skip-last-record` hide the last records on topic list
* `akhq.ui-options.topic.show-all-consumer-groups` expand lists of consumer groups on topic list

### Topic Data
* `akhq.ui-options.topic-data.sort`: default sort order (OLDEST, NEWEST) (default: OLDEST)



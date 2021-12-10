
# Protobuf deserialization

To deserialize topics containing data in Protobuf format, you can set topics mapping:
for each `topic-regex` you can specify `descriptor-file-base64` (descriptor file encoded to Base64 format),
or you can put descriptor files in `descriptors-folder` and specify `descriptor-file` name,
also specify corresponding message types for keys and values.
If, for example, keys are not in Protobuf format, `key-message-type` can be omitted,
the same for `value-message-type`. . It's important to keep in mind that both `key-message-type` and `value-message-type`
require a fully-qualified name.
This configuration can be specified for each Kafka cluster.

Example configuration can look like as follows:

```yaml
akhq:
  connections:
    kafka:
      properties:
        # standard kafka properties
      deserialization:
        protobuf:
          descriptors-folder: "/app/protobuf_desc"
          topics-mapping:
            - topic-regex: "album.*"
              descriptor-file-base64: "Cs4BCgthbGJ1bS5wcm90bxIXY29tLm5ldGNyYWNrZXIucHJvdG9idWYidwoFQWxidW0SFAoFdGl0bGUYASABKAlSBXRpdGxlEhYKBmFydGlzdBgCIAMoCVIGYXJ0aXN0EiEKDHJlbGVhc2VfeWVhchgDIAEoBVILcmVsZWFzZVllYXISHQoKc29uZ190aXRsZRgEIAMoCVIJc29uZ1RpdGxlQiUKF2NvbS5uZXRjcmFja2VyLnByb3RvYnVmQgpBbGJ1bVByb3RvYgZwcm90bzM="
              value-message-type: "org.akhq.utils.Album"
            - topic-regex: "film.*"
              descriptor-file-base64: "CuEBCgpmaWxtLnByb3RvEhRjb20uY29tcGFueS5wcm90b2J1ZiKRAQoERmlsbRISCgRuYW1lGAEgASgJUgRuYW1lEhoKCHByb2R1Y2VyGAIgASgJUghwcm9kdWNlchIhCgxyZWxlYXNlX3llYXIYAyABKAVSC3JlbGVhc2VZZWFyEhoKCGR1cmF0aW9uGAQgASgFUghkdXJhdGlvbhIaCghzdGFycmluZxgFIAMoCVIIc3RhcnJpbmdCIQoUY29tLmNvbXBhbnkucHJvdG9idWZCCUZpbG1Qcm90b2IGcHJvdG8z"
              value-message-type: "org.akhq.utils.Film"
            - topic-regex: "test.*"
              descriptor-file: "other.desc"
              key-message-type: "org.akhq.utils.Row"
              value-message-type: "org.akhq.utils.Envelope"
```

More examples about Protobuf deserialization can be found in [tests](https://github.com/tchiotludo/akhq/tree/dev/src/test/java/org/akhq/utils).
Info about the descriptor files generation can be found in [test resources](https://github.com/tchiotludo/akhq/tree/dev/src/test/resources/protobuf_proto).
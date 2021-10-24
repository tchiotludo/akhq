

# Schema references

Since Confluent 5.5.0, Avro schemas can now be reused by others schemas through schema references. This feature allows to define a schema once and use it as a record type inside one or more schemas.

When registering new Avro schemas with AKHQ UI, it is now possible to pass a slightly more complex object with a `schema` and a `references` field.

To register a new schema without references, no need to change anything:

```json
{
    "name": "Schema1",
    "namespace": "org.akhq",
    "type": "record",
    "fields": [
        {
            "name": "description",
            "type": "string"
        }
    ]
}
```

To register a new schema with a reference to an already registered schema:

```json
{
    "schema": {
        "name": "Schema2",
        "namespace": "org.akhq",
        "type": "record",
        "fields": [
            {
                "name": "name",
                "type": "string"
            },
            {
                "name": "schema1",
                "type": "Schema1"
            }
        ]
    },
    "references": [
        {
            "name": "Schema1",
            "subject": "SCHEMA_1",
            "version": 1
        }
    ]
}
````

Documentation on Confluent 5.5 and schema references can be found [here](https://docs.confluent.io/5.5.0/schema-registry/serdes-develop/index.html).

package org.akhq.modules.schemaregistry.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.VersionState;
import io.apicurio.registry.rest.client.models.WrappedVersionState;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.models.Record;
import org.akhq.models.Schema;
import org.akhq.models.Topic;
import org.akhq.utils.AvroToJsonSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Optional end-to-end test against a real Apicurio Registry v3 instance.
 * Enable by setting {@code APICURIO_LIVE_URL=http://localhost:8080/apis/registry/v3}.
 */
@EnabledIfEnvironmentVariable(named = "APICURIO_LIVE_URL", matches = ".+")
class ApicurioLiveRegistryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String AVRO_V1 = """
        {"type":"record","name":"LiveOrder","namespace":"com.acme.live","fields":[{"name":"id","type":"string"},{"name":"amount","type":"int"}]}""";
    private static final String AVRO_V2_COMPAT = """
        {"type":"record","name":"LiveOrder","namespace":"com.acme.live","fields":[{"name":"id","type":"string"},{"name":"amount","type":"int"},{"name":"note","type":["null","string"],"default":null}]}""";
    private static final String AVRO_V2_INCOMPAT = """
        {"type":"record","name":"LiveOrder","namespace":"com.acme.live","fields":[{"name":"id","type":"string"},{"name":"requiredNewField","type":"string"}]}""";
    private static final String AVRO_ADDR = """
        {"type":"record","name":"LiveAddress","namespace":"com.acme.live","fields":[{"name":"city","type":"string"}]}""";
    private static final String AVRO_CUST = """
        {"type":"record","name":"LiveCustomer","namespace":"com.acme.live","fields":[{"name":"name","type":"string"},{"name":"address","type":"com.acme.live.LiveAddress"}]}""";
    private static final String JSON_SCHEMA = """
        {"$schema":"http://json-schema.org/draft-07/schema#","type":"object","properties":{"sku":{"type":"string"},"qty":{"type":"integer"}},"required":["sku"]}""";
    private static final String PROTO_SCHEMA = """
        syntax = "proto3";
        package com.acme.live;
        message LivePayment {
          string id = 1;
          int64 total = 2;
        }
        """;

    @Test
    void fullEndToEndAgainstRealApicurioRegistryV3() throws Exception {
        String url = System.getenv("APICURIO_LIVE_URL");
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String customGroup = "com.acme.live." + suffix;
        String orderSubject = customGroup + ":orders-" + suffix + "-value";
        String defaultSubject = "items-" + suffix + "-value";
        String protoSubject = customGroup + ":payments-" + suffix + "-value";
        String addrSubject = customGroup + ":address-" + suffix;
        String custSubject = customGroup + ":customer-" + suffix;

        Connection.SchemaRegistry sr = ApicurioConfigTest.createSchemaRegistry(url, null, null, null, null);
        ApicurioConfig config = new ApicurioConfig(sr);
        ApicurioSchemaRegistryClient client = new ApicurioSchemaRegistryClient(config);
        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(client);
        AvroToJsonSerializer avroToJson = new AvroToJsonSerializer(List.of());
        Topic topic = mock(Topic.class);
        when(topic.getName()).thenReturn("orders");
        when(topic.isInternalTopic()).thenReturn(false);

        try {
            // 1. Register Avro in custom group + verify v1
            Schema v1 = client.register(orderSubject, "AVRO", AVRO_V1, List.of());
            assertEquals(orderSubject, v1.getSubject());
            assertEquals(1, v1.getVersion());
            assertEquals("AVRO", v1.getSchemaType());
            assertNotNull(v1.getId());

            // 2. Compatibility rules: default NONE -> set BACKWARD -> test compatible & incompatible
            assertEquals(Schema.Config.CompatibilityLevelConfig.NONE, client.getConfig(orderSubject).getCompatibilityLevel());
            client.updateConfig(orderSubject, new Schema.Config(Schema.Config.CompatibilityLevelConfig.BACKWARD));
            assertEquals(Schema.Config.CompatibilityLevelConfig.BACKWARD, client.getConfig(orderSubject).getCompatibilityLevel());

            assertTrue(client.testCompatibility(orderSubject, new org.apache.avro.Schema.Parser().parse(AVRO_V2_COMPAT)).isEmpty());
            List<String> violations = client.testCompatibility(orderSubject, new org.apache.avro.Schema.Parser().parse(AVRO_V2_INCOMPAT));
            assertFalse(violations.isEmpty(), "Expected compatibility rule violation on incompatible Avro schema");

            // 3. Register compatible v2 + verify getLatestVersion & getAllVersions
            Schema v2 = client.register(orderSubject, "AVRO", AVRO_V2_COMPAT, List.of());
            assertEquals(2, v2.getVersion());
            assertEquals(2, client.getAllVersions(orderSubject).size());
            assertEquals(2, client.getLatestVersion(orderSubject).getVersion());

            // 4. Disable v2 via native SDK and verify getLatestVersion gracefully falls back to v1!
            RegistryClient rawSdk = RegistryClientFactory.create(config.buildClientOptions());
            WrappedVersionState disabledState = new WrappedVersionState();
            disabledState.setState(VersionState.DISABLED);
            rawSdk.groups().byGroupId(customGroup)
                .artifacts().byArtifactId("orders-" + suffix + "-value")
                .versions().byVersionExpression("2")
                .state().put(disabledState);
            client.invalidateCaches();
            assertEquals(1, client.getLatestVersion(orderSubject).getVersion());
            assertEquals(1, client.getAllVersions(orderSubject).size());

            // Re-enable v2
            WrappedVersionState enabledState = new WrappedVersionState();
            enabledState.setState(VersionState.ENABLED);
            rawSdk.groups().byGroupId(customGroup)
                .artifacts().byArtifactId("orders-" + suffix + "-value")
                .versions().byVersionExpression("2")
                .state().put(enabledState);
            client.invalidateCaches();
            assertEquals(2, client.getLatestVersion(orderSubject).getVersion());

            // 5. Register JSON Schema (default group) & Protobuf (custom group) & Avro with references
            Schema jsonSchema = client.register(defaultSubject, "JSON", JSON_SCHEMA, List.of());
            Schema protoSchema = client.register(protoSubject, "PROTOBUF", PROTO_SCHEMA, List.of());
            client.register(addrSubject, "AVRO", AVRO_ADDR, List.of());
            Schema custSchema = client.register(
                custSubject,
                "AVRO",
                AVRO_CUST,
                List.of(new SchemaReference("com.acme.live.LiveAddress", addrSubject, -1))
            );
            assertEquals("com.acme.live.LiveCustomer", custSchema.getAvroSchema().getFullName());

            // 6. List subjects across all groups & verify grouping format
            List<String> allSubjects = client.listSubjects();
            assertTrue(allSubjects.contains(defaultSubject));
            assertTrue(allSubjects.contains(orderSubject));
            assertTrue(allSubjects.contains(protoSubject));

            // 7. Scoped group client sees only customGroup without prefix
            Connection.SchemaRegistry scopedSr = ApicurioConfigTest.createSchemaRegistry(url, null, null, customGroup, null);
            ApicurioSchemaRegistryClient scopedClient = new ApicurioSchemaRegistryClient(new ApicurioConfig(scopedSr));
            List<String> scopedSubjects = scopedClient.listSubjects();
            assertTrue(scopedSubjects.contains("orders-" + suffix + "-value"));
            assertFalse(scopedSubjects.contains(defaultSubject));

            // 8. Lookup subjects by ID
            List<Schema> byId = client.getSubjectsBySchemaId(v2.getId());
            assertTrue(byId.stream().anyMatch(s -> s.getSubject().equals(orderSubject)));

            // 9. Serialize & deserialize round-trips (Avro 4-byte, Avro 8-byte globalId, Avro headers, JSON Schema, Protobuf Ref)
            ResolvedApicurioSchema resolvedOrder = client.resolveById(v2.getId(), ApicurioSchemaRegistryClient.IdLookupHint.CONTENT_ID);
            byte[] avro4Wire = new ApicurioSchemaSerializer(v2.getId(), resolvedOrder.parsedSchema(), config)
                .serialize("{\"id\":\"live-1\",\"amount\":100,\"note\":\"ok\"}");
            Record avroRec = buildRecord(topic, avroToJson, deserializer, null, avro4Wire, new RecordHeaders());
            assertEquals(String.valueOf(v2.getId()), avroRec.getValueSchemaId());
            assertEquals(orderSubject, avroRec.getValueSubject());
            JsonNode avroNode = MAPPER.readTree(avroRec.getValue());
            assertEquals("live-1", avroNode.get("id").asText());
            assertEquals(100, avroNode.get("amount").asInt());
            assertEquals("ok", avroNode.get("note").asText());

            // 8-byte legacy globalId round-trip!
            ApicurioConfig cfg8 = new ApicurioConfig(ApicurioConfigTest.createSchemaRegistry(
                url, null, null, null, Map.of(ApicurioConfig.PROP_ID_HANDLER, "8byte", ApicurioConfig.PROP_USE_ID, "globalId")
            ));
            byte[] avro8Wire = new ApicurioSchemaSerializer(resolvedOrder.globalId(), resolvedOrder.parsedSchema(), cfg8)
                .serialize("{\"id\":\"live-8\",\"amount\":88,\"note\":\"legacy\"}");
            Record avro8Rec = buildRecord(topic, avroToJson, deserializer, null, avro8Wire, new RecordHeaders());
            assertEquals(String.valueOf(resolvedOrder.globalId()), avro8Rec.getValueSchemaId());
            assertEquals("live-8", MAPPER.readTree(avro8Rec.getValue()).get("id").asText());

            // JSON Schema round-trip
            ResolvedApicurioSchema resolvedJson = client.resolveById(jsonSchema.getId(), ApicurioSchemaRegistryClient.IdLookupHint.CONTENT_ID);
            byte[] jsonWire = new ApicurioSchemaSerializer(jsonSchema.getId(), resolvedJson.parsedSchema(), config)
                .serialize("{\"sku\":\"SKU-LIVE\",\"qty\":3}");
            Record jsonRec = buildRecord(topic, avroToJson, deserializer, null, jsonWire, new RecordHeaders());
            assertEquals("SKU-LIVE", MAPPER.readTree(jsonRec.getValue()).get("sku").asText());

            // Protobuf Ref round-trip
            ResolvedApicurioSchema resolvedProto = client.resolveById(protoSchema.getId(), ApicurioSchemaRegistryClient.IdLookupHint.CONTENT_ID);
            byte[] protoWire = new ApicurioSchemaSerializer(protoSchema.getId(), resolvedProto.parsedSchema(), config)
                .serialize("{\"id\":\"pay-1\",\"total\":999}");
            Record protoRec = buildRecord(topic, avroToJson, deserializer, null, protoWire, new RecordHeaders());
            JsonNode protoNode = MAPPER.readTree(protoRec.getValue());
            assertEquals("pay-1", protoNode.get("id").asText());
            assertEquals("999", protoNode.get("total").asText());

            // Headers mode (globalId in Kafka headers, raw JSON payload)
            RecordHeaders hdrs = new RecordHeaders();
            hdrs.add(ApicurioConfig.DEFAULT_VALUE_GLOBAL_ID_HEADER, ByteBuffer.allocate(8).putLong(resolvedJson.globalId()).array());
            Record hdrRec = buildRecord(topic, avroToJson, deserializer, null, "{\"sku\":\"HDR-LIVE\",\"qty\":9}".getBytes(StandardCharsets.UTF_8), hdrs);
            assertEquals("HDR-LIVE", MAPPER.readTree(hdrRec.getValue()).get("sku").asText());

            // 10. Delete version & delete artifacts
            client.deleteVersion(orderSubject, 1);
            assertEquals(1, client.getAllVersions(orderSubject).size());

            client.delete(orderSubject);
            RestClientException notFound = assertThrows(RestClientException.class, () -> client.getLatestVersion(orderSubject));
            assertEquals(404, notFound.getStatus());
        } finally {
            for (String s : List.of(custSubject, addrSubject, protoSubject, defaultSubject, orderSubject)) {
                try {
                    client.delete(s);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Record buildRecord(
        Topic topic,
        AvroToJsonSerializer avroToJson,
        ApicurioRecordDeserializer deserializer,
        byte[] key,
        byte[] value,
        RecordHeaders headers
    ) {
        ConsumerRecord<byte[], byte[]> cr = new ConsumerRecord<>(
            "orders", 0, 0L, 1700000000000L,
            org.apache.kafka.common.record.TimestampType.CREATE_TIME,
            key == null ? -1 : key.length,
            value == null ? -1 : value.length,
            key,
            value,
            headers,
            java.util.Optional.empty()
        );
        return new Record(
            null, cr, SchemaRegistryType.APICURIO,
            null, null, null,
            avroToJson,
            null, null,
            value, topic, null, deserializer
        );
    }
}

package org.akhq.modules.schemaregistry.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.models.Record;
import org.akhq.models.Topic;
import org.akhq.utils.AvroToJsonSerializer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApicurioSerdeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String AVRO_ORDER = """
        {"type":"record","name":"Order","namespace":"com.acme","fields":[{"name":"id","type":"string"},{"name":"amount","type":"int"}]}""";
    private static final String AVRO_STRING = "\"string\"";
    private static final String AVRO_ADDRESS = """
        {"type":"record","name":"Address","namespace":"com.acme","fields":[{"name":"city","type":"string"}]}""";
    private static final String AVRO_CUSTOMER = """
        {"type":"record","name":"Customer","namespace":"com.acme","fields":[{"name":"name","type":"string"},{"name":"address","type":"com.acme.Address"}]}""";
    private static final String JSON_ITEM = """
        {"$schema":"http://json-schema.org/draft-07/schema#","type":"object","properties":{"sku":{"type":"string"},"qty":{"type":"integer"}},"required":["sku"]}""";
    private static final String PROTO_MULTI = """
        syntax = "proto3";
        package com.acme;
        message FirstEvent {
          string id = 1;
        }
        message SecondEvent {
          string title = 1;
          int32 count = 2;
        }
        """;
    private static final String PROTO_COMMON = """
        syntax = "proto3";
        package com.acme.common;
        message Money {
          string currency = 1;
          int64 units = 2;
        }
        """;
    private static final String PROTO_INVOICE = """
        syntax = "proto3";
        package com.acme.billing;
        import "common/money.proto";
        message Invoice {
          string invoiceId = 1;
          com.acme.common.Money total = 2;
        }
        """;

    private InMemoryApicurioV3Server server;
    private AvroToJsonSerializer avroToJsonSerializer;
    private Topic topic;

    @BeforeEach
    void setUp() throws Exception {
        server = new InMemoryApicurioV3Server();
        avroToJsonSerializer = new AvroToJsonSerializer(List.of());
        topic = mock(Topic.class);
        when(topic.getName()).thenReturn("orders");
        when(topic.isInternalTopic()).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    private ApicurioSchemaRegistryClient clientWithProps(Map<String, String> props) {
        Connection.SchemaRegistry sr = ApicurioConfigTest.createSchemaRegistry(server.getV3Url(), null, null, null, props);
        return new ApicurioSchemaRegistryClient(new ApicurioConfig(sr));
    }

    @Test
    void avro4ByteContentIdAnd8ByteLegacyGlobalIdAutoDetection() throws Exception {
        var ver = server.seedArtifactVersion("com.acme", "orders-value", "AVRO", AVRO_ORDER, "ENABLED", List.of());
        byte[] avroPayload = encodeAvroOrder("ord-1", 42);

        ApicurioSchemaRegistryClient client = clientWithProps(null);
        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(client);

        // 1. Apicurio v3 default: magic byte + 4-byte contentId
        byte[] wire4 = concat(new byte[]{0x00}, ByteBuffer.allocate(4).putInt((int) ver.contentId()).array(), avroPayload);
        Record record4 = buildRecord(null, wire4, new RecordHeaders(), deserializer);
        assertEquals(String.valueOf(ver.contentId()), record4.getValueSchemaId());
        assertEquals("com.acme:orders-value", record4.getValueSubject());
        JsonNode json4 = MAPPER.readTree(record4.getValue());
        assertEquals("ord-1", json4.get("id").asText());
        assertEquals(42, json4.get("amount").asInt());

        // 2. Apicurio v2 / Debezium legacy format: magic byte + 8-byte globalId (with plain string key!)
        byte[] wire8 = concat(new byte[]{0x00}, ByteBuffer.allocate(8).putLong(ver.globalId()).array(), avroPayload);
        Record record8 = buildRecord("plain-key".getBytes(StandardCharsets.UTF_8), wire8, new RecordHeaders(), deserializer);
        assertNull(record8.getKeySchemaId());
        assertEquals("plain-key", record8.getKey());
        assertEquals(String.valueOf(ver.globalId()), record8.getValueSchemaId());
        assertEquals("com.acme:orders-value", record8.getValueSubject());
        JsonNode json8 = MAPPER.readTree(record8.getValue());
        assertEquals("ord-1", json8.get("id").asText());
        assertEquals(42, json8.get("amount").asInt());
    }

    @Test
    void avroExplicit8ByteContentIdAnd4ByteGlobalId() throws Exception {
        var ver = server.seedArtifactVersion("default", "orders-value", "AVRO", AVRO_ORDER, "ENABLED", List.of());
        byte[] avroPayload = encodeAvroOrder("ord-88", 99);

        // 8-byte contentId
        ApicurioRecordDeserializer des8Cid = new ApicurioRecordDeserializer(clientWithProps(Map.of(
            ApicurioConfig.PROP_ID_HANDLER, "8byte",
            ApicurioConfig.PROP_USE_ID, "contentId"
        )));
        byte[] wire8Cid = concat(new byte[]{0x00}, ByteBuffer.allocate(8).putLong(ver.contentId()).array(), avroPayload);
        Record r1 = buildRecord(null, wire8Cid, new RecordHeaders(), des8Cid);
        assertEquals("ord-88", MAPPER.readTree(r1.getValue()).get("id").asText());

        // 4-byte globalId
        ApicurioRecordDeserializer des4Gid = new ApicurioRecordDeserializer(clientWithProps(Map.of(
            ApicurioConfig.PROP_ID_HANDLER, "4byte",
            ApicurioConfig.PROP_USE_ID, "globalId"
        )));
        byte[] wire4Gid = concat(new byte[]{0x00}, ByteBuffer.allocate(4).putInt((int) ver.globalId()).array(), avroPayload);
        Record r2 = buildRecord(null, wire4Gid, new RecordHeaders(), des4Gid);
        assertEquals("ord-88", MAPPER.readTree(r2.getValue()).get("id").asText());
    }

    @Test
    void avroHeadersModeGlobalIdContentIdHashCoordinatesAndJsonEncoding() throws Exception {
        var ver = server.seedArtifactVersion("com.acme", "orders-value", "AVRO", AVRO_ORDER, "ENABLED", List.of());
        var keyVer = server.seedArtifactVersion("default", "orders-key", "AVRO", AVRO_STRING, "ENABLED", List.of());
        byte[] avroPayload = encodeAvroOrder("ord-hdr", 7);

        // Encode primitive string key
        ByteArrayOutputStream keyOut = new ByteArrayOutputStream();
        Encoder keyEnc = EncoderFactory.get().binaryEncoder(keyOut, null);
        new GenericDatumWriter<Object>(new org.apache.avro.Schema.Parser().parse(AVRO_STRING)).write("k-100", keyEnc);
        keyEnc.flush();
        byte[] keyBytes = keyOut.toByteArray();

        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(clientWithProps(null));

        // 1. Key by contentId header + Value by globalId header
        RecordHeaders h1 = new RecordHeaders();
        h1.add(ApicurioConfig.DEFAULT_KEY_CONTENT_ID_HEADER, ByteBuffer.allocate(8).putLong(keyVer.contentId()).array());
        h1.add(ApicurioConfig.DEFAULT_VALUE_GLOBAL_ID_HEADER, ByteBuffer.allocate(8).putLong(ver.globalId()).array());
        Record r1 = buildRecord(keyBytes, avroPayload, h1, deserializer);
        assertEquals("k-100", r1.getKey());
        assertEquals("orders-key", r1.getKeySubject());
        assertEquals(String.valueOf(ver.globalId()), r1.getValueSchemaId());
        assertEquals("ord-hdr", MAPPER.readTree(r1.getValue()).get("id").asText());

        // 2. Value by groupId + artifactId + version headers + JSON Avro encoding
        RecordHeaders h2 = new RecordHeaders();
        h2.add(ApicurioConfig.DEFAULT_VALUE_GROUP_ID_HEADER, "com.acme".getBytes(StandardCharsets.UTF_8));
        h2.add(ApicurioConfig.DEFAULT_VALUE_ARTIFACT_ID_HEADER, "orders-value".getBytes(StandardCharsets.UTF_8));
        h2.add(ApicurioConfig.DEFAULT_VALUE_VERSION_HEADER, "1".getBytes(StandardCharsets.UTF_8));
        h2.add(ApicurioConfig.DEFAULT_VALUE_ENCODING_HEADER, "JSON".getBytes(StandardCharsets.UTF_8));
        byte[] avroJsonBytes = "{\"id\":\"ord-json\",\"amount\":55}".getBytes(StandardCharsets.UTF_8);
        Record r2 = buildRecord(null, avroJsonBytes, h2, deserializer);
        assertEquals("com.acme:orders-value", r2.getValueSubject());
        assertEquals("ord-json", MAPPER.readTree(r2.getValue()).get("id").asText());
        assertEquals(55, MAPPER.readTree(r2.getValue()).get("amount").asInt());

        // 3. Value by contentHash header
        RecordHeaders h3 = new RecordHeaders();
        h3.add(ApicurioConfig.DEFAULT_VALUE_CONTENT_HASH_HEADER, ver.contentHash().getBytes(StandardCharsets.UTF_8));
        Record r3 = buildRecord(null, avroPayload, h3, deserializer);
        assertEquals("ord-hdr", MAPPER.readTree(r3.getValue()).get("id").asText());

        // 4. Inline schema in headers
        RecordHeaders h4 = new RecordHeaders();
        h4.add(ApicurioConfig.DEFAULT_VALUE_SCHEMA_HEADER, AVRO_ORDER.getBytes(StandardCharsets.UTF_8));
        h4.add(ApicurioConfig.DEFAULT_VALUE_SCHEMA_TYPE_HEADER, "AVRO".getBytes(StandardCharsets.UTF_8));
        Record r4 = buildRecord(null, avroPayload, h4, deserializer);
        assertNull(r4.getValueSchemaId());
        assertEquals("ord-hdr", MAPPER.readTree(r4.getValue()).get("id").asText());
    }

    @Test
    void avroWithReferencesRoundTrip() throws Exception {
        server.seedArtifactVersion("com.acme", "address", "AVRO", AVRO_ADDRESS, "ENABLED", List.of());
        var custVer = server.seedArtifactVersion(
            "com.acme",
            "customer-value",
            "AVRO",
            AVRO_CUSTOMER,
            "ENABLED",
            List.of(new InMemoryApicurioV3Server.StoredRef("com.acme", "address", "1", "com.acme.Address"))
        );

        ApicurioSchemaRegistryClient client = clientWithProps(null);
        ResolvedApicurioSchema resolved = client.resolveByContentId(custVer.contentId());
        ApicurioSchemaSerializer serializer = new ApicurioSchemaSerializer(custVer.contentId(), resolved.parsedSchema(), client.getConfig());

        byte[] wire = serializer.serialize("{\"name\":\"Alice\",\"address\":{\"city\":\"Madrid\"}}");
        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(client);
        Record record = buildRecord(null, wire, new RecordHeaders(), deserializer);

        JsonNode json = MAPPER.readTree(record.getValue());
        assertEquals("Alice", json.get("name").asText());
        assertEquals("Madrid", json.get("address").get("city").asText());
    }

    @Test
    void jsonSchemaSerializeValidateAndDeserialize() throws Exception {
        var ver = server.seedArtifactVersion("default", "items-value", "JSON", JSON_ITEM, "ENABLED", List.of());
        ApicurioSchemaRegistryClient client = clientWithProps(null);
        ResolvedApicurioSchema resolved = client.resolveByContentId(ver.contentId());
        ApicurioSchemaSerializer serializer = new ApicurioSchemaSerializer(ver.contentId(), resolved.parsedSchema(), client.getConfig());

        // Validation rejects missing required field "sku"
        assertThrows(IllegalArgumentException.class, () -> serializer.serialize("{\"qty\":3}"));

        // Valid JSON round-trips cleanly (4-byte payload)
        byte[] wire = serializer.serialize("{\"sku\":\"ABC-123\",\"qty\":5}");
        assertEquals(0x00, wire[0]);
        assertEquals((int) ver.contentId(), ByteBuffer.wrap(wire, 1, 4).getInt());

        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(client);
        Record record = buildRecord(null, wire, new RecordHeaders(), deserializer);
        JsonNode json = MAPPER.readTree(record.getValue());
        assertEquals("ABC-123", json.get("sku").asText());
        assertEquals(5, json.get("qty").asInt());

        // Headers mode JSON Schema
        RecordHeaders headers = new RecordHeaders();
        headers.add(ApicurioConfig.DEFAULT_VALUE_CONTENT_ID_HEADER, ByteBuffer.allocate(8).putLong(ver.contentId()).array());
        Record headerRec = buildRecord(null, "{\"sku\":\"HDR-1\",\"qty\":2}".getBytes(StandardCharsets.UTF_8), headers, deserializer);
        assertEquals("HDR-1", MAPPER.readTree(headerRec.getValue()).get("sku").asText());
    }

    @Test
    void protobufApicurioRefConfluentIndexesMsgTypeHeaderAndImports() throws Exception {
        var multiVer = server.seedArtifactVersion("com.acme", "events-value", "PROTOBUF", PROTO_MULTI, "ENABLED", List.of());
        server.seedArtifactVersion("com.acme.common", "money.proto", "PROTOBUF", PROTO_COMMON, "ENABLED", List.of());
        var invoiceVer = server.seedArtifactVersion(
            "com.acme.billing",
            "invoice-value",
            "PROTOBUF",
            PROTO_INVOICE,
            "ENABLED",
            List.of(new InMemoryApicurioV3Server.StoredRef("com.acme.common", "money.proto", "1", "common/money.proto"))
        );

        ApicurioSchemaRegistryClient client = clientWithProps(null);
        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(client);

        // Build SecondEvent protobuf bytes
        ProtobufSchema multiProto = (ProtobufSchema) client.resolveByContentId(multiVer.contentId()).parsedSchema();
        DynamicMessage.Builder secondBuilder = multiProto.newMessageBuilder("SecondEvent");
        JsonFormat.parser().merge("{\"title\":\"launch\",\"count\":3}", secondBuilder);
        byte[] secondBytes = secondBuilder.build().toByteArray();

        // 1. Apicurio default Ref framing (4-byte contentId + Ref{name="SecondEvent"} + proto bytes)
        ByteArrayOutputStream refOut = new ByteArrayOutputStream();
        refOut.write(0x00);
        refOut.write(ByteBuffer.allocate(4).putInt((int) multiVer.contentId()).array());
        ApicurioSchemaSerializer.writeApicurioProtobufRef("SecondEvent", refOut);
        refOut.write(secondBytes);
        Record refRec = buildRecord(null, refOut.toByteArray(), new RecordHeaders(), deserializer);
        JsonNode refJson = MAPPER.readTree(refRec.getValue());
        assertEquals("launch", refJson.get("title").asText());
        assertEquals(3, refJson.get("count").asInt());

        // 2. Confluent message-indexes framing for SecondEvent (index [1] -> zigzag size=1 (0x02), index=1 (0x02))
        ByteArrayOutputStream idxOut = new ByteArrayOutputStream();
        idxOut.write(0x00);
        idxOut.write(ByteBuffer.allocate(4).putInt((int) multiVer.contentId()).array());
        idxOut.write(0x02); // size 1 (zigzag)
        idxOut.write(0x02); // index 1 (zigzag)
        idxOut.write(secondBytes);
        Record idxRec = buildRecord(null, idxOut.toByteArray(), new RecordHeaders(), deserializer);
        JsonNode idxJson = MAPPER.readTree(idxRec.getValue());
        assertEquals("launch", idxJson.get("title").asText());
        assertEquals(3, idxJson.get("count").asInt());

        // 3. Headers mode with apicurio.value.msgType header (raw protobuf bytes)
        RecordHeaders protoHeaders = new RecordHeaders();
        protoHeaders.add(ApicurioConfig.DEFAULT_VALUE_GLOBAL_ID_HEADER, ByteBuffer.allocate(8).putLong(multiVer.globalId()).array());
        protoHeaders.add(ApicurioConfig.DEFAULT_VALUE_MSG_TYPE_HEADER, "com.acme.SecondEvent".getBytes(StandardCharsets.UTF_8));
        Record hdrRec = buildRecord(null, secondBytes, protoHeaders, deserializer);
        JsonNode hdrJson = MAPPER.readTree(hdrRec.getValue());
        assertEquals("launch", hdrJson.get("title").asText());
        assertEquals(3, hdrJson.get("count").asInt());

        // 4. Protobuf with imported .proto reference round-trip via ApicurioSchemaSerializer (8-byte mode!)
        ApicurioSchemaRegistryClient client8 = clientWithProps(Map.of(ApicurioConfig.PROP_ID_HANDLER, "8byte"));
        ResolvedApicurioSchema resolvedInvoice = client8.resolveByGlobalId(invoiceVer.globalId());
        ApicurioSchemaSerializer invoiceSerializer = new ApicurioSchemaSerializer(
            invoiceVer.globalId(), resolvedInvoice.parsedSchema(), client8.getConfig()
        );
        byte[] invoiceWire = invoiceSerializer.serialize("{\"invoiceId\":\"INV-9\",\"total\":{\"currency\":\"EUR\",\"units\":250}}");
        Record invoiceRec = buildRecord(null, invoiceWire, new RecordHeaders(), deserializer);
        JsonNode invoiceJson = MAPPER.readTree(invoiceRec.getValue());
        assertEquals("INV-9", invoiceJson.get("invoiceId").asText());
        assertEquals("EUR", invoiceJson.get("total").get("currency").asText());
        assertEquals("250", invoiceJson.get("total").get("units").asText());
    }

    @Test
    void corruptPayloadFallsBackToRawStringAndRecordsException() {
        ApicurioRecordDeserializer deserializer = new ApicurioRecordDeserializer(clientWithProps(null));
        // Magic byte + unknown schema id 99999
        byte[] unknownIdWire = concat(new byte[]{0x00}, ByteBuffer.allocate(4).putInt(99999).array(), "bad".getBytes(StandardCharsets.UTF_8));
        Record record = buildRecord(null, unknownIdWire, new RecordHeaders(), deserializer);

        assertEquals("99999", record.getValueSchemaId());
        assertNotNull(record.getValue());
        assertFalse(record.getExceptions().isEmpty());
    }

    private Record buildRecord(byte[] key, byte[] value, RecordHeaders headers, ApicurioRecordDeserializer deserializer) {
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
            avroToJsonSerializer,
            null, null,
            value, topic, null, deserializer
        );
    }

    private static byte[] encodeAvroOrder(String id, int amount) throws Exception {
        org.apache.avro.Schema schema = new org.apache.avro.Schema.Parser().parse(AVRO_ORDER);
        GenericRecord record = new GenericData.Record(schema);
        record.put("id", id);
        record.put("amount", amount);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        new GenericDatumWriter<GenericRecord>(schema).write(record, encoder);
        encoder.flush();
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            out.writeBytes(p);
        }
        return out.toByteArray();
    }
}

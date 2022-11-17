package org.akhq.modules;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.modules.schemaregistry.AvroSerializer;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AvroSchemaSerializerTest {
    private static final int SCHEMA_ID = 3;

    private final org.apache.avro.Schema SCHEMA = SchemaBuilder
            .record("schema1").namespace("org.akhq")
            .fields()
            .name("title").type().stringType().noDefault()
            .name("release_year").type().intType().noDefault()
            .name("rating").type().doubleType().noDefault()
            .endRecord();

    public static final String VALID_JSON = "{\n" +
            "  \"title\": \"the-title\",\n" +
            "  \"release_year\": 123,\n" +
            "  \"rating\": 2.5\n" +
            "}";

    public static final String INVALID_JSON = "{\n" +
            "  \"no_title\": 3,\n" +
            "  \"release_year\": 123,\n" +
            "  \"rating\": 2.5\n" +
            "}";

    private AvroSerializer avroSerializer;

    @BeforeEach
    void setUp() {
        avroSerializer = AvroSerializer.newInstance(SCHEMA_ID, new AvroSchema(SCHEMA), SchemaRegistryType.CONFLUENT);
    }

    @Test
    void shouldSerializeSchemaId() {
        byte[] bytes = avroSerializer.serialize(VALID_JSON);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte magicBytes = buffer.get();
        int serializedSchemaId = buffer.getInt();

        assertEquals(0, magicBytes);
        assertEquals(SCHEMA_ID, serializedSchemaId);
    }

    @Test
    void shouldFailIfDoesntMatchSchemaId() {
        assertThrows(NullPointerException.class, () -> {
            int schemaId = 3;
            avroSerializer.serialize(INVALID_JSON);
        });
    }
}

package org.akhq.modules;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.modules.schemaregistry.AvroSerializer;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

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

    private final org.apache.avro.Schema NESTED_SCHEMA =
        new Schema.Parser().parse("{\n" +
            "  \"type\": \"record\",\n" +
            "  \"name\": \"userInfo\",\n" +
            "  \"namespace\": \"org.akhq\",\n" +
            "  \"fields\": [\n" +
            "    {\n" +
            "      \"name\": \"username\",\n" +
            "      \"type\": \"string\",\n" +
            "      \"default\": \"NONE\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"age\",\n" +
            "      \"type\": \"int\",\n" +
            "      \"default\": -1\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"phone\",\n" +
            "      \"type\": \"string\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"address\",\n" +
            "      \"type\": {\n" +
            "        \"type\": \"record\",\n" +
            "        \"name\": \"mailing_address\",\n" +
            "        \"fields\": [\n" +
            "          {\n" +
            "            \"name\": \"street\",\n" +
            "            \"type\": \"string\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"detailaddress\",\n" +
            "            \"type\": {\n" +
            "              \"type\": \"record\",\n" +
            "              \"name\": \"homeaddress\",\n" +
            "              \"fields\": [\n" +
            "                {\n" +
            "                  \"name\": \"houseNo\",\n" +
            "                  \"type\": \"int\",\n" +
            "                  \"default\": 1\n" +
            "                },\n" +
            "                {\n" +
            "                  \"name\": \"roomNo\",\n" +
            "                  \"type\": \"int\"\n" +
            "                }\n" +
            "              ]\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}");

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

    public static final String INVALID_NESTED_JSON = "{\n" +
        "  \"phone\": \"12345\",\n" +
        "  \"address\": {\n" +
        "    \"street\": \"Test Street\",\n" +
        "    \"detailaddress\" : {\n" +
        "        \n" +
        "    }\n" +
        "  }\n" +
        "}";

   public static final String VALID_NESTED_JSON = "{\n" +
       "  \"phone\": \"2312331\",\n" +
       "  \"address\": {\n" +
       "    \"street\": \"Test Street\",\n" +
       "    \"detailaddress\" : {\n" +
       "        \"houseNo\" : 1,\n" +
       "        \"roomNo\" : 2\n" +
       "    }\n" +
       "  }\n" +
       "}";

    private AvroSerializer avroSerializer;
    private AvroSerializer avroDeepSerializer;

    @BeforeEach
    void setUp() {
        avroSerializer = AvroSerializer.newInstance(SCHEMA_ID, new AvroSchema(SCHEMA), SchemaRegistryType.CONFLUENT);
        avroDeepSerializer = AvroSerializer.newInstance(SCHEMA_ID, new AvroSchema(NESTED_SCHEMA), SchemaRegistryType.CONFLUENT);
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
        assertThrows(IllegalArgumentException.class, () -> avroSerializer.serialize(INVALID_JSON));
    }

    @Test
    void shouldThrowForDeepNestedInvalidJSON() {
        assertThrows(IllegalArgumentException.class, () -> avroDeepSerializer.serialize(INVALID_NESTED_JSON));
    }

    @Test
    void shouldNotThrowForValidNestedJSON() {
        assertDoesNotThrow(() -> avroDeepSerializer.serialize(VALID_NESTED_JSON));
    }
}

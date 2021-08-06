package org.akhq.modules;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.akhq.configs.SchemaRegistryType;
import org.apache.avro.SchemaBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvroSchemaSerializerTest {

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

    private AvroSerializer cut;

    @Mock
    private SchemaRegistryClient schemaRegistryClient;

    @BeforeEach
    void setUp() throws IOException, RestClientException {
        cut = new AvroSerializer(schemaRegistryClient, SchemaRegistryType.CONFLUENT);
        when(schemaRegistryClient.getById(anyInt())).thenReturn(SCHEMA);
    }

    @Test
    public void shouldSerializeSchemaId() {
        int schemaId = 3;
        byte[] bytes = cut.toAvro(VALID_JSON, schemaId);

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte magicBytes = buffer.get();
        int serializedSchemaId = buffer.getInt();

        assertEquals(0, magicBytes);
        assertEquals(schemaId, serializedSchemaId);
    }

    @Test
    public void shouldFailIfDoesntMatchSchemaId() {
        assertThrows(NullPointerException.class, () -> {
            int schemaId = 3;
            cut.toAvro(INVALID_JSON, schemaId);
        });
    }

}

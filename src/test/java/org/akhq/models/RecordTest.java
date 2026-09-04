package org.akhq.models;

import org.akhq.configs.SchemaRegistryType;
import org.akhq.modules.schemaregistry.AzureSchemaRegistryDeserializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordTest {

    private static final String TOPIC_NAME = "azure-event-hub-topic";
    private static final String SCHEMA_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Mock
    private AzureSchemaRegistryDeserializer azureSchemaRegistryDeserializer;

    private Topic newTopic() {
        Topic topic = new Topic();
        topic.setName(TOPIC_NAME);
        topic.setInternal(false);
        return topic;
    }

    private Record newAzureRecord(byte[] keyBytes, byte[] valueBytes, String contentTypeHeader) {
        ConsumerRecord<byte[], byte[]> consumerRecord = new ConsumerRecord<>(TOPIC_NAME, 0, 0, keyBytes, valueBytes);
        if (contentTypeHeader != null) {
            consumerRecord.headers().add(new RecordHeader("content-type", contentTypeHeader.getBytes(StandardCharsets.UTF_8)));
        }

        return new Record(
            null,
            consumerRecord,
            SchemaRegistryType.AZURE,
            azureSchemaRegistryDeserializer,
            null,
            null,
            null,
            null,
            null,
            valueBytes,
            newTopic(),
            null
        );
    }

    @Test
    void schemaIdIsExtractedFromContentTypeHeaderForAzureRegistry() {
        Record record = newAzureRecord(
            "key".getBytes(StandardCharsets.UTF_8),
            "value".getBytes(StandardCharsets.UTF_8),
            "avro/binary+" + SCHEMA_ID
        );

        assertEquals(SCHEMA_ID, record.getValueSchemaId());
    }

    @Test
    void getKeyReturnsNullForAzureRegistryEvenWhenSchemaIdIsPresent() {
        Record record = newAzureRecord(
            "key".getBytes(StandardCharsets.UTF_8),
            "value".getBytes(StandardCharsets.UTF_8),
            "avro/binary+" + SCHEMA_ID
        );

        assertNull(record.getKey());
    }

    @Test
    void getValueDelegatesToAzureSchemaRegistryDeserializer() throws IOException {
        byte[] valueBytes = "value".getBytes(StandardCharsets.UTF_8);
        Record record = newAzureRecord(
            "key".getBytes(StandardCharsets.UTF_8),
            valueBytes,
            "avro/binary+" + SCHEMA_ID
        );

        when(azureSchemaRegistryDeserializer.deserialize(TOPIC_NAME, valueBytes, SCHEMA_ID)).thenReturn("decoded-value");

        assertEquals("decoded-value", record.getValue());
    }

}

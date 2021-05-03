package org.akhq.repositories;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchemaUtils;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SchemaRegistryType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class AvroWireFormatConverterTest {

    private AvroWireFormatConverter avroWireFormatConverter;
    private SchemaRegistryClient schemaRegistryClient;

    @Data
    @AllArgsConstructor
    private static class MyRecord {
        private int anInt;
        private String aString;
    }

    @BeforeEach
    @SneakyThrows
    public void before() {
        avroWireFormatConverter = new AvroWireFormatConverter();
        schemaRegistryClient = mock(SchemaRegistryClient.class);


        ReflectData reflectData = ReflectData.get();
        Schema schema = reflectData.getSchema(MyRecord.class);
        int id = 100;
        when(schemaRegistryClient.getById(id)).thenReturn(schema);
        when(schemaRegistryClient.getSchemaById(id)).thenReturn(new AvroSchema(schema, id));
        when(schemaRegistryClient.getSchemaMetadata("mySubject", 1)).thenReturn(new SchemaMetadata(id, 1, ""));
    }

    @Test
    public void convertValueToWireFormatNull() {
        byte[] convertedValue = avroWireFormatConverter.convertValueToWireFormat(new ConsumerRecord<>("topic", 1, 0, new byte[0], null), schemaRegistryClient, SchemaRegistryType.CONFLUENT);
        assertNull(convertedValue);
    }

    @Test
    public void convertValueToWireFormatEmptyValue() {
        byte[] convertedValue = avroWireFormatConverter.convertValueToWireFormat(new ConsumerRecord<>("topic", 1, 0, new byte[0], new byte[0]), schemaRegistryClient, SchemaRegistryType.CONFLUENT);
        assertEquals(0, convertedValue.length);
    }

    @Test
    @SneakyThrows
    public void convertValueToWireFormatWrongContentType() {
        MyRecord record = new MyRecord(42, "leet");
        byte[] avroPayload = serializeAvro(record);

        ConsumerRecord<byte[], byte[]> consumerRecord = new ConsumerRecord<>("topic", 1, 0, new byte[0], avroPayload);
        consumerRecord.headers().add(new RecordHeader("contentType", "mySubject.v1".getBytes()));
        byte[] convertedValue = avroWireFormatConverter.convertValueToWireFormat(consumerRecord, schemaRegistryClient, SchemaRegistryType.CONFLUENT);

        assertEquals(convertedValue, avroPayload);
    }

    @Test
    @SneakyThrows
    public void convertValueToWireFormatWireFormat() {
        MyRecord record = new MyRecord(42, "leet");
        byte[] avroPayload = serializeAvro(record);

        ConsumerRecord<byte[], byte[]> consumerRecord = new ConsumerRecord<>("topic", 1, 0, new byte[0], avroPayload);
        consumerRecord.headers().add(new RecordHeader("contentType", "application/vnd.mySubject.v1+avro".getBytes()));
        byte[] convertedValue = avroWireFormatConverter.convertValueToWireFormat(consumerRecord, schemaRegistryClient, SchemaRegistryType.CONFLUENT);

        KafkaAvroDeserializer kafkaAvroDeserializer = new KafkaAvroDeserializer(schemaRegistryClient);
        GenericData.Record deserializedRecord = (GenericData.Record) kafkaAvroDeserializer.deserialize(null, convertedValue);
        assertEquals(record.getAnInt(), deserializedRecord.get(1));
        assertEquals(record.getAString(), deserializedRecord.get(0).toString());
    }

    @SneakyThrows
    private byte[] serializeAvro(MyRecord record) {
        Schema schema = AvroSchemaUtils.getSchema(record, true, true);
        DatumWriter<MyRecord> writer = new ReflectDatumWriter<>(schema);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(stream, null);
        writer.write(record, encoder);
        encoder.flush();
        return stream.toByteArray();
    }
}

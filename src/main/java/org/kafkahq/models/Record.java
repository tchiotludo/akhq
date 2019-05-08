package org.kafkahq.models;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
@Getter
public class Record {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;
    private final TimestampType timestampType;
    private final byte[] key;
    private final byte[] value;
    private final Map<String, String> headers = new HashMap<>();
    private final KafkaAvroDeserializer kafkaAvroDeserializer;

    public Record(ConsumerRecord<byte[], byte[]> record, KafkaAvroDeserializer kafkaAvroDeserializer) {
        this.topic = record.topic();
        this.partition = record.partition();
        this.offset = record.offset();
        this.timestamp = record.timestamp();
        this.timestampType = record.timestampType();
        this.key = record.key();
        this.value = record.value();
        for (Header header: record.headers()) {
            this.headers.put(header.key(), header.value() != null ? new String(header.value()) : null);
        }

        this.kafkaAvroDeserializer = kafkaAvroDeserializer;
    }

    public String getKeyAsString() {
        return convertToString(key);
    }

    public String getKeyAsBase64() {
        return new String(Base64.getEncoder().encode(value));
    }

    public String getValueAsString() {
        return convertToString(value);
    }

    private String convertToString(byte[] payload) {
        if (payload == null) {
            return null;
        } else  if (isAvroPayload(payload)) {
            try {
                GenericRecord deserialize = (GenericRecord) kafkaAvroDeserializer.deserialize(topic, payload);
                return deserialize.toString();
            } catch (Exception exception) {
                return new String(payload);
            }
        } else {
            return new String(payload);
        }
    }

    private static boolean isAvroPayload(byte[] payload) {
        boolean convert = false;
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte magicBytes = buffer.get();
            int schemaId = buffer.getInt();

            if (magicBytes == 0 && schemaId >= 0) {
                convert = true;
            }
        } catch (Exception ignore) {

        }
        return convert;
    }
}

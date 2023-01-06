package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.micronaut.context.annotation.Value;
import kafka.coordinator.group.GroupMetadataManager;
import kafka.coordinator.transaction.TransactionLog;
import kafka.coordinator.transaction.TxnKey;
import lombok.*;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.utils.AvroToJsonDeserializer;
import org.akhq.utils.AvroToJsonSerializer;
import org.akhq.utils.ProtobufToJsonDeserializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Record {
    private Topic topic;
    private int partition;
    private long offset;
    private ZonedDateTime timestamp;
    @JsonIgnore
    private TimestampType timestampType;
    private Integer keySchemaId;
    private Integer valueSchemaId;
    private List<KeyValue<String, String>> headers = new ArrayList<>();
    @JsonIgnore
    private Deserializer kafkaAvroDeserializer;
    @JsonIgnore
    private Deserializer kafkaProtoDeserializer;
    @JsonIgnore
    private Deserializer kafkaJsonDeserializer;
    @JsonIgnore
    private AvroToJsonSerializer avroToJsonSerializer;

    @JsonIgnore
    private SchemaRegistryClient client;

    @JsonIgnore
    private ProtobufToJsonDeserializer protobufToJsonDeserializer;

    @JsonIgnore
    private AvroToJsonDeserializer avroToJsonDeserializer;

    @Getter(AccessLevel.NONE)
    private byte[] bytesKey;

    @Getter(AccessLevel.NONE)
    private String key;

    @Getter(AccessLevel.NONE)
    private byte[] bytesValue;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String value;

    private final List<String> exceptions = new ArrayList<>();

    private byte MAGIC_BYTE;

    private Boolean truncated;

    public Record(RecordMetadata record, SchemaRegistryType schemaRegistryType, byte[] bytesKey, byte[] bytesValue, List<KeyValue<String, String>> headers, Topic topic) {
        this.MAGIC_BYTE = schemaRegistryType.getMagicByte();
        this.topic = topic;
        this.partition = record.partition();
        this.offset = record.offset();
        this.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault());
        this.bytesKey = bytesKey;
        this.keySchemaId = getAvroSchemaId(this.bytesKey);
        this.bytesValue = bytesValue;
        this.valueSchemaId = getAvroSchemaId(this.bytesValue);
        this.headers = headers;
        this.truncated = false;
    }

    public Record(SchemaRegistryClient client, ConsumerRecord<byte[], byte[]> record, SchemaRegistryType schemaRegistryType, Deserializer kafkaAvroDeserializer,
                  Deserializer kafkaJsonDeserializer, Deserializer kafkaProtoDeserializer, AvroToJsonSerializer avroToJsonSerializer,
                  ProtobufToJsonDeserializer protobufToJsonDeserializer, AvroToJsonDeserializer avroToJsonDeserializer, byte[] bytesValue, Topic topic) {
        if (schemaRegistryType == SchemaRegistryType.TIBCO) {
            this.MAGIC_BYTE = (byte) 0x80;
        } else {
            this.MAGIC_BYTE = 0x0;
        }
        this.client = client;
        this.topic = topic;
        this.partition = record.partition();
        this.offset = record.offset();
        this.timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault());
        this.timestampType = record.timestampType();
        this.bytesKey = record.key();
        this.keySchemaId = getAvroSchemaId(this.bytesKey);
        this.bytesValue = bytesValue;
        this.valueSchemaId = getAvroSchemaId(this.bytesValue);
        for (Header header: record.headers()) {
            String headerValue = header.value() != null ? new String(header.value()) : null;
            this.headers.add(new KeyValue<>(header.key(), headerValue));
        }

        this.kafkaAvroDeserializer = kafkaAvroDeserializer;
        this.protobufToJsonDeserializer = protobufToJsonDeserializer;
        this.avroToJsonDeserializer = avroToJsonDeserializer;
        this.kafkaProtoDeserializer = kafkaProtoDeserializer;
        this.avroToJsonSerializer = avroToJsonSerializer;
        this.kafkaJsonDeserializer = kafkaJsonDeserializer;
        this.truncated = false;
    }

    public String getKey() {
        if (this.key == null) {
            this.key = convertToString(bytesKey, keySchemaId, true);
        }

        return this.key;
    }

    @JsonIgnore
    public String getKeyAsBase64() {
        if (bytesKey == null) {
            return null;
        } else {
            return new String(Base64.getEncoder().encode(bytesKey));
        }
    }

    public String getValue() {
        if (this.value == null) {
            this.value = convertToString(bytesValue, valueSchemaId, false);
        }

        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setTruncated(Boolean truncated) {
        this.truncated = truncated;
    }

    private String convertToString(byte[] payload, Integer schemaId, boolean isKey) {
        if (payload == null) {
            return null;
        } else if (schemaId != null) {
            try {

                Object toType = null;

                if (client != null) {
                    ParsedSchema schema = client.getSchemaById(schemaId);
                    if ( schema.schemaType().equals(ProtobufSchema.TYPE) ) {
                       toType = kafkaProtoDeserializer.deserialize(topic.getName(), payload);
                       if (!(toType instanceof Message)) {
                           return String.valueOf(toType);
                       }

                       Message dynamicMessage = (Message)toType;
                       return avroToJsonSerializer.getMapper().readTree(JsonFormat.printer().print(dynamicMessage)).toString();
                    } else  if ( schema.schemaType().equals(JsonSchema.TYPE) ) {
                      toType = kafkaJsonDeserializer.deserialize(topic.getName(), payload);
                      if ( !(toType instanceof JsonNode) ) {
                          return String.valueOf(toType);
                      }
                      JsonNode node = (JsonNode) toType;
                      return node.toString();
                    }
                }

                toType = kafkaAvroDeserializer.deserialize(topic.getName(), payload);

                //for primitive avro type
                if (!(toType instanceof GenericRecord)) {
                    return String.valueOf(toType);
                }

                GenericRecord record = (GenericRecord) toType;
                return avroToJsonSerializer.toJson(record);

            } catch (Exception exception) {
                this.exceptions.add(exception.getMessage());

                return new String(payload);
            }
        } else if (topic.isInternalTopic() && topic.getName().equals("__consumer_offsets")) {
            try {
                if (isKey) {
                    return GroupMetadataManager.readMessageKey(ByteBuffer.wrap(payload)).key().toString();
                } else {
                    return GroupMetadataManager.readOffsetMessageValue(ByteBuffer.wrap(payload)).toString();
                }
            } catch (Exception exception) {
                this.exceptions.add(Optional.ofNullable(exception.getMessage())
                        .filter(msg -> !msg.isBlank())
                        .orElseGet(() -> exception.getClass().getCanonicalName()));

                return new String(payload);
            }
        } else if (topic.isInternalTopic() && topic.getName().equals("__transaction_state")) {
            try {
                if (isKey) {
                    TxnKey txnKey = TransactionLog.readTxnRecordKey(ByteBuffer.wrap(payload));
                    return avroToJsonSerializer.getMapper().writeValueAsString(
                        Map.of("transactionalId", txnKey.transactionalId(), "version", txnKey.version())
                    );
                } else {
                    TxnKey txnKey = TransactionLog.readTxnRecordKey(ByteBuffer.wrap(this.bytesKey));
                    return avroToJsonSerializer.getMapper().writeValueAsString(TransactionLog.readTxnRecordValue(txnKey.transactionalId(), ByteBuffer.wrap(payload)));
                }
            } catch (Exception exception) {
                this.exceptions.add(Optional.ofNullable(exception.getMessage())
                    .filter(msg -> !msg.isBlank())
                    .orElseGet(() -> exception.getClass().getCanonicalName()));

                return new String(payload);
            }
        } else {
            if (protobufToJsonDeserializer != null) {
                try {
                    String record = protobufToJsonDeserializer.deserialize(topic.getName(), payload, isKey);
                    if (record != null) {
                        return record;
                    }
                } catch (Exception exception) {
                    this.exceptions.add(exception.getMessage());

                    return new String(payload);
                }
            }

            if (avroToJsonDeserializer != null) {
                try {
                    String record = avroToJsonDeserializer.deserialize(topic.getName(), payload, isKey);
                    if (record != null) {
                        return record;
                    }
                } catch (Exception exception) {
                    this.exceptions.add(exception.getMessage());

                    return new String(payload);
                }
            }
            return new String(payload);
        }
    }

    public Collection<String> getHeadersKeySet() {
        return headers
            .stream()
            .map(KeyValue::getKey)
            .collect(Collectors.toList());
    }

    public Collection<String> getHeadersValues() {
        return headers
            .stream()
            .map(KeyValue::getValue)
            .collect(Collectors.toList());
    }

    private Integer getAvroSchemaId(byte[] payload) {
        if (topic.isInternalTopic()) {
            return null;
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte magicBytes = buffer.get();
            int schemaId = buffer.getInt();

            if (magicBytes == MAGIC_BYTE && schemaId >= 0) {
                return schemaId;
            }
        } catch (Exception ignore) {

        }
        return null;
    }

}

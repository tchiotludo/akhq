package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.record.TimestampType;

import java.util.HashMap;
import java.util.Map;

@ToString
@EqualsAndHashCode
@Getter
public class Record<K, V> {
    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;
    private final TimestampType timestampType;
    private final K key;
    private final V value;
    private final Map<String, String> headers = new HashMap<>();

    public Record(ConsumerRecord<K, V> record) {
        this.topic = record.topic();
        this.partition = record.partition();
        this.offset = record.offset();
        this.timestamp = record.timestamp();
        this.timestampType = record.timestampType();
        this.key = record.key();
        this.value = record.value();
        for (Header header: record.headers()) {
            this.headers.put(header.key(), new String(header.value()));
        }
    }
}

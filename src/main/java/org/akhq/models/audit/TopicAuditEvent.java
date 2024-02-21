package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class TopicAuditEvent extends AuditEvent {

    private Type type;
    private String clusterId;
    private String topicName;
    private Integer partitions;
    private Map<String, String> config;

    public static TopicAuditEvent newTopic(String clusterId, String topicName, int partitions, Map<String, String> config) {
        return new TopicAuditEvent(Type.NEW_TOPIC, clusterId, topicName, partitions, config);
    }

    public static TopicAuditEvent deleteTopic(String clusterId, String topicName) {
        return new TopicAuditEvent(Type.DELETE_TOPIC, clusterId, topicName, 0, null);
    }

    public static TopicAuditEvent configChange(String clusterId, String topicName, Map<String, String> config) {
        return new TopicAuditEvent(Type.CONFIG_CHANGE, clusterId, topicName, null, config);
    }

    public static TopicAuditEvent increasePartitions(String clusterId, String topicName, int partitions) {
        return new TopicAuditEvent(Type.INCREASE_PARTITION, clusterId, topicName, partitions, null);
    }

    private enum Type {
        NEW_TOPIC,
        CONFIG_CHANGE,
        INCREASE_PARTITION,
        DELETE_TOPIC
    }
}

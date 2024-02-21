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
    private Boolean deleted;

    public static TopicAuditEvent newTopic(String clusterId, String topicName, int partitions, Map<String, String> config) {
        return new TopicAuditEvent(Type.NEW_TOPIC, clusterId, topicName, partitions, config, false);
    }

    public static TopicAuditEvent deleteTopic(String clusterId, String topicName) {
        return new TopicAuditEvent(Type.NEW_TOPIC, clusterId, topicName, 0, null, true);
    }

    public static TopicAuditEvent configChange(String clusterId, String topicName, Map<String, String> config) {
        return new TopicAuditEvent(Type.CONFIG_CHANGE, clusterId, topicName, null, config, false);
    }

    public static TopicAuditEvent increasePartitions(String clusterId, String topicName, int partitions) {
        return new TopicAuditEvent(Type.CONFIG_CHANGE, clusterId, topicName, partitions, null, false);
    }

    private static enum Type {
        NEW_TOPIC,
        CONFIG_CHANGE
    }
}

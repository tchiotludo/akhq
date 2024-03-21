package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicAuditEvent extends AuditEvent {

    private ActionType actionType;
    private String clusterId;
    private String topicName;
    private Integer partitions;
    private Map<String, String> config;

    public static TopicAuditEvent newTopic(String clusterId, String topicName, int partitions, Map<String, String> config) {
        return new TopicAuditEvent(ActionType.TOPIC_CREATE, clusterId, topicName, partitions, config);
    }

    public static TopicAuditEvent deleteTopic(String clusterId, String topicName) {
        return new TopicAuditEvent(ActionType.TOPIC_DELETE, clusterId, topicName, 0, null);
    }

    public static TopicAuditEvent configChange(String clusterId, String topicName, Map<String, String> config) {
        return new TopicAuditEvent(ActionType.TOPIC_CONFIG_CHANGE, clusterId, topicName, null, config);
    }

    public static TopicAuditEvent increasePartitions(String clusterId, String topicName, int partitions) {
        return new TopicAuditEvent(ActionType.TOPIC_INCREASE_PARTITION, clusterId, topicName, partitions, null);
    }

    @Override
    public String getType() {
        return "TOPIC";
    }
}

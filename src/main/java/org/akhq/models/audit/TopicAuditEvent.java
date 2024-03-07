package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

import static org.akhq.models.audit.AuditEvent.ActionType.NEW_TOPIC;

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
        return new TopicAuditEvent(ActionType.NEW_TOPIC, clusterId, topicName, partitions, config);
    }

    public static TopicAuditEvent deleteTopic(String clusterId, String topicName) {
        return new TopicAuditEvent(ActionType.DELETE_TOPIC, clusterId, topicName, 0, null);
    }

    public static TopicAuditEvent configChange(String clusterId, String topicName, Map<String, String> config) {
        return new TopicAuditEvent(ActionType.CONFIG_CHANGE, clusterId, topicName, null, config);
    }

    public static TopicAuditEvent increasePartitions(String clusterId, String topicName, int partitions) {
        return new TopicAuditEvent(ActionType.INCREASE_PARTITION, clusterId, topicName, partitions, null);
    }

    @Override
    public String getType() {
        return "TOPIC";
    }
}

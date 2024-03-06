package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ConsumerGroupAuditEvent extends AuditEvent {

    private Type type;
    private String clusterId;
    private String consumerGroupName;
    private String topic;

    public static ConsumerGroupAuditEvent updateOffsets(String clusterId, String consumerGroupName) {
        return new ConsumerGroupAuditEvent(Type.UPDATE_OFFSETS_CONSUMER_GROUP, clusterId, consumerGroupName, null);
    }

    public static ConsumerGroupAuditEvent deleteGroup(String clusterId, String consumerGroupName) {
        return new ConsumerGroupAuditEvent(Type.DELETE_CONSUMER_GROUP, clusterId, consumerGroupName, null);
    }

    public static ConsumerGroupAuditEvent deleteGroupOffsets(String clusterId, String consumerGroupName, String topicName) {
        return new ConsumerGroupAuditEvent(Type.DELETE_OFFSETS_CONSUMER_GROUP, clusterId, consumerGroupName, topicName);
    }

    private enum Type {
        UPDATE_OFFSETS_CONSUMER_GROUP,
        DELETE_OFFSETS_CONSUMER_GROUP,
        DELETE_CONSUMER_GROUP
    }

}

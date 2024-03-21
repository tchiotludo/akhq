package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ConsumerGroupAuditEvent extends AuditEvent {

    private ActionType actionType;
    private String clusterId;
    private String consumerGroupName;
    private String topic;

    public static ConsumerGroupAuditEvent updateOffsets(String clusterId, String topicName, String consumerGroupName) {
        return new ConsumerGroupAuditEvent(ActionType.CONSUMER_GROUP_UPDATE_OFFSETS, clusterId, consumerGroupName, topicName);
    }

    public static ConsumerGroupAuditEvent deleteGroup(String clusterId, String consumerGroupName) {
        return new ConsumerGroupAuditEvent(ActionType.CONSUMER_GROUP_DELETE, clusterId, consumerGroupName, null);
    }

    public static ConsumerGroupAuditEvent deleteGroupOffsets(String clusterId, String consumerGroupName, String topicName) {
        return new ConsumerGroupAuditEvent(ActionType.CONSUMER_GROUP_DELETE_OFFSETS, clusterId, consumerGroupName, topicName);
    }

    @Override
    public String getType() {
        return "CONSUMER_GROUP";
    }
}

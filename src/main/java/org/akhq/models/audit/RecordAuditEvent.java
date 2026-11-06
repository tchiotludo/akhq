package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordAuditEvent extends AuditEvent {

    private ActionType actionType;
    private String clusterId;
    private String topicName;
    private Integer partition;
    private Integer recordCount;

    public static RecordAuditEvent produce(String clusterId, String topicName, Integer recordCount) {
        return new RecordAuditEvent(ActionType.RECORD_PRODUCE, clusterId, topicName, null, recordCount);
    }

    public static RecordAuditEvent delete(String clusterId, String topicName, Integer partition) {
        return new RecordAuditEvent(ActionType.RECORD_DELETE, clusterId, topicName, partition, null);
    }

    public static RecordAuditEvent empty(String clusterId, String topicName) {
        return new RecordAuditEvent(ActionType.RECORD_EMPTY, clusterId, topicName, null, null);
    }

    @Override
    public String getType() {
        return "RECORD";
    }
}

package org.akhq.models.audit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TopicAuditEvent.class, name = "TOPIC"),
    @JsonSubTypes.Type(value = ConsumerGroupAuditEvent.class, name = "CONSUMER_GROUP"),
    @JsonSubTypes.Type(value = SchemaAuditEvent.class, name = "SCHEMA"),
    @JsonSubTypes.Type(value = ConnectAuditEvent.class, name = "CONNECT")
})
@Data
@NoArgsConstructor
public abstract class AuditEvent {
    private String type;
    private String userName;
    private ActionType actionType;

    public enum ActionType {
        CONSUMER_GROUP_UPDATE_OFFSETS,
        CONSUMER_GROUP_DELETE_OFFSETS,
        CONSUMER_GROUP_DELETE,
        TOPIC_CREATE,
        TOPIC_CONFIG_CHANGE,
        TOPIC_INCREASE_PARTITION,
        TOPIC_DELETE,
        SCHEMA_CREATE,
        SCHEMA_UPDATE,
        SCHEMA_COMPATIBILITY_UPDATE,
        SCHEMA_DELETE,
        CONNECT_CREATE,
        CONNECT_UPDATE,
        CONNECT_DELETE,
        CONNECT_RESTART,
        CONNECT_TASK_RESTART,
        CONNECT_PAUSE,
        CONNECT_RESUME,
    }

}

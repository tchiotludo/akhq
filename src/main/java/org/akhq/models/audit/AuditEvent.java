package org.akhq.models.audit;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TopicAuditEvent.class, name = "TOPIC"),
    @JsonSubTypes.Type(value = ConsumerGroupAuditEvent.class, name = "CONSUMER_GROUP")
})
@Data
@NoArgsConstructor
public abstract class AuditEvent {
    private String type;
    private String userName;
    private ActionType actionType;

    public enum ActionType {
        UPDATE_OFFSETS_CONSUMER_GROUP,
        DELETE_OFFSETS_CONSUMER_GROUP,
        DELETE_CONSUMER_GROUP,
        NEW_TOPIC,
        CONFIG_CHANGE,
        INCREASE_PARTITION,
        DELETE_TOPIC
    }

}

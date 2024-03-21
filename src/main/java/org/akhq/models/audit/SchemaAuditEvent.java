package org.akhq.models.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SchemaAuditEvent extends AuditEvent {

    private String subject;
    private Integer schemaId;
    private Integer version;
    private ActionType actionType;
    private String newCompatibility;

    public static SchemaAuditEvent createOrUpdateSchema(String subject, Integer schemaId, Integer version) {
        ActionType type;

        if (version == null) {
            type = ActionType.SCHEMA_CREATE;
        } else {
            if (version == 1) {
                type = ActionType.SCHEMA_CREATE;
            } else {
                type = ActionType.SCHEMA_UPDATE;
            }
        }

        return new SchemaAuditEvent(subject, schemaId, version, type, null);
    }

    public static SchemaAuditEvent deleteSchema(String subject, Integer schemaId) {
        return new SchemaAuditEvent(subject, schemaId, null, ActionType.SCHEMA_DELETE, null);
    }

    public static SchemaAuditEvent updateSchemaCompatibility(String subject, Integer schemaId, String newCompatibility) {
        return new SchemaAuditEvent(subject, schemaId, null, ActionType.SCHEMA_UPDATE, newCompatibility);
    }

}

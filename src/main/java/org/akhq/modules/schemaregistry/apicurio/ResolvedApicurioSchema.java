package org.akhq.modules.schemaregistry.apicurio;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;

import java.util.List;

/**
 * Resolved Apicurio Registry schema artifact version with its parsed representation and coordinates.
 */
public record ResolvedApicurioSchema(
    Long contentId,
    Long globalId,
    String groupId,
    String artifactId,
    String subject,
    String version,
    String artifactType,
    String rawContent,
    ParsedSchema parsedSchema,
    List<SchemaReference> references
) {
    public Long preferredId(ApicurioConfig.UseId useId) {
        if (useId == ApicurioConfig.UseId.GLOBAL_ID) {
            return globalId != null ? globalId : contentId;
        }
        return contentId != null ? contentId : globalId;
    }
}

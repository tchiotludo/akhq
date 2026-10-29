package org.akhq.mcp.services;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import jakarta.inject.Singleton;
import org.akhq.mcp.model.GetSchemaResult;
import org.akhq.mcp.model.SearchSchemasResult;
import org.akhq.models.Schema;
import org.akhq.repositories.SchemaRegistryRepository;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Singleton
public class AkhqSchemaToolService {
    private final SchemaRegistryRepository repository;

    public AkhqSchemaToolService(SchemaRegistryRepository repository) {
        this.repository = repository;
    }

    public SearchSchemasResult searchSchemas(String cluster, List<String> filters, String search, Integer maxResults)
        throws IOException, RestClientException {
        int limit = McpLimits.maxResults(maxResults);
        List<String> subjects = repository.all(cluster, McpLimits.search(search), filters);
        List<String> returned = subjects.subList(0, Math.min(limit, subjects.size()));
        boolean truncated = subjects.size() > returned.size();

        return new SearchSchemasResult(
            cluster, subjects.size(), truncated, returned,
            McpLimits.listMessage("schema subject", subjects.size(), returned.size(), truncated)
        );
    }

    public GetSchemaResult getSchema(String cluster, String subject, Integer version) throws IOException, RestClientException {
        if (version != null && version < 1) {
            throw new IllegalArgumentException("`arguments.version` must be greater than 0");
        }

        List<Schema> versions;
        try {
            versions = repository.getAllVersions(cluster, subject);
        } catch (RestClientException e) {
            throw notFoundOrRethrow(e, subject);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Cluster '" + cluster + "' has no schema registry", e);
        }

        Schema schema = versions.stream()
            .filter(candidate -> version == null || Objects.equals(candidate.getVersion(), version))
            .max(Comparator.comparing(Schema::getVersion))
            .orElseThrow(() -> new IllegalArgumentException(
                "Version " + version + " of subject '" + subject + "' doesn't exist"
            ));

        String text = schema.getSchema();

        return new GetSchemaResult(
            cluster,
            schema.getSubject(),
            schema.getId(),
            schema.getVersion(),
            versions.stream().map(Schema::getVersion).sorted().toList(),
            schema.getSchemaType(),
            schema.getCompatibilityLevel() == null ? null : schema.getCompatibilityLevel().name(),
            McpLimits.truncate(text, McpLimits.MAX_TEXT_LENGTH),
            text != null && text.length() > McpLimits.MAX_TEXT_LENGTH,
            schema.getReferences().stream()
                .map(reference -> new GetSchemaResult.Reference(reference.getName(), reference.getSubject(), reference.getVersion()))
                .toList(),
            schema.getException()
        );
    }

    private static RuntimeException notFoundOrRethrow(RestClientException e, String subject) throws RestClientException {
        if (e.getErrorCode() == SchemaRegistryRepository.ERROR_NOT_FOUND) {
            return new IllegalArgumentException("Subject '" + subject + "' doesn't exist", e);
        }

        throw e;
    }
}

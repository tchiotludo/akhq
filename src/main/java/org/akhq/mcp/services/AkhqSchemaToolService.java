package org.akhq.mcp.services;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import jakarta.inject.Singleton;
import org.akhq.mcp.model.FindSchemasForTopicResult;
import org.akhq.mcp.model.GetSchemaByIdResult;
import org.akhq.mcp.model.GetSchemaResult;
import org.akhq.mcp.model.SearchSchemasResult;
import org.akhq.models.Schema;
import org.akhq.repositories.SchemaRegistryRepository;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Looks up the subjects of a topic with the TopicNameStrategy names, among the subjects the caller can see.
     */
    public FindSchemasForTopicResult findSchemasForTopic(String cluster, List<String> filters, String topic)
        throws IOException, RestClientException {
        List<String> visible = repository.all(cluster, Optional.of(topic), filters);
        String keySubject = topic + "-key";
        String valueSubject = topic + "-value";

        GetSchemaResult key = visible.contains(keySubject) ? getSchema(cluster, keySubject, null) : null;
        GetSchemaResult value = visible.contains(valueSubject) ? getSchema(cluster, valueSubject, null) : null;
        List<String> others = visible.stream()
            .filter(subject -> !subject.equals(keySubject) && !subject.equals(valueSubject))
            .limit(McpLimits.DEFAULT_MAX_RESULTS)
            .toList();

        return new FindSchemasForTopicResult(
            cluster, topic, key, value, others,
            key == null && value == null
                ? "No `" + keySubject + "` or `" + valueSubject + "` subject found. `otherSubjects` lists the visible subjects whose name contains the topic name."
                : "Latest versions of the subjects of the topic. `otherSubjects` lists other visible subjects whose name contains the topic name."
        );
    }

    /**
     * Resolves a global schema id to its subjects (restricted to the ones the caller can see) and to the schema
     * of the subject matching the topic when given, or else of the first visible subject.
     */
    public GetSchemaByIdResult getSchemaById(String cluster, List<String> filters, int id, String topic)
        throws IOException, RestClientException {
        if (id < 0) {
            throw new IllegalArgumentException("`arguments.id` must not be negative");
        }

        List<Schema> candidates;
        try {
            candidates = repository.getSubjectsBySchemaId(cluster, id);
        } catch (RestClientException e) {
            if (e.getStatus() == 404) {
                return new GetSchemaByIdResult(cluster, id, List.of(), null, "No schema found with the id " + id + ".");
            }
            throw e;
        }

        Set<String> visible = new HashSet<>(repository.all(cluster, Optional.empty(), filters));
        List<Schema> allowed = candidates.stream().filter(schema -> visible.contains(schema.getSubject())).toList();
        String topicFilter = topic == null || topic.isBlank() ? null : topic.trim();
        Schema preferred = allowed.stream()
            .filter(schema -> topicFilter != null && schema.getSubject().contains(topicFilter))
            .findFirst()
            .orElse(allowed.isEmpty() ? null : allowed.get(0));

        return new GetSchemaByIdResult(
            cluster, id,
            allowed.stream().map(schema -> new GetSchemaByIdResult.SubjectVersion(schema.getSubject(), schema.getVersion())).toList(),
            preferred == null ? null : getSchema(cluster, preferred.getSubject(), preferred.getVersion()),
            preferred == null
                ? "No schema found with the id " + id + ", or you are not allowed to see its subjects."
                : "Schema " + id + " is used by " + allowed.size() + " subject(s). `schema` is the one of `" + preferred.getSubject() + "`."
        );
    }

    private static RuntimeException notFoundOrRethrow(RestClientException e, String subject) throws RestClientException {
        if (e.getErrorCode() == SchemaRegistryRepository.ERROR_NOT_FOUND) {
            return new IllegalArgumentException("Subject '" + subject + "' doesn't exist", e);
        }

        throw e;
    }
}

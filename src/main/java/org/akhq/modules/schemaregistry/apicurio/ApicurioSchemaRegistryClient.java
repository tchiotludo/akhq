package org.akhq.modules.schemaregistry.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.microsoft.kiota.ApiException;
import io.apicurio.registry.client.RegistryClientFactory;
import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.models.ArtifactReference;
import io.apicurio.registry.rest.client.models.ArtifactSearchResults;
import io.apicurio.registry.rest.client.models.CreateArtifact;
import io.apicurio.registry.rest.client.models.CreateArtifactResponse;
import io.apicurio.registry.rest.client.models.CreateRule;
import io.apicurio.registry.rest.client.models.CreateVersion;
import io.apicurio.registry.rest.client.models.IfArtifactExists;
import io.apicurio.registry.rest.client.models.ProblemDetails;
import io.apicurio.registry.rest.client.models.Rule;
import io.apicurio.registry.rest.client.models.RuleType;
import io.apicurio.registry.rest.client.models.RuleViolationCause;
import io.apicurio.registry.rest.client.models.RuleViolationProblemDetails;
import io.apicurio.registry.rest.client.models.SearchedArtifact;
import io.apicurio.registry.rest.client.models.SearchedVersion;
import io.apicurio.registry.rest.client.models.VersionContent;
import io.apicurio.registry.rest.client.models.VersionMetaData;
import io.apicurio.registry.rest.client.models.VersionSearchResults;
import io.apicurio.registry.rest.client.models.VersionState;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.akhq.models.Schema;
import org.akhq.repositories.SchemaRegistryRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Native client adapter for Apicurio Registry v3 REST API, backed by the official
 * Apicurio Registry Java SDK ({@link RegistryClient}) using the JDK HTTP adapter.
 */
@Slf4j
public class ApicurioSchemaRegistryClient {
    private static final int PAGE_LIMIT = 500;
    private static final String LATEST_BRANCH_EXPRESSION = "branch=latest";
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public enum IdLookupHint {
        CONTENT_ID,
        GLOBAL_ID,
        PREFER_CONTENT_ID,
        PREFER_GLOBAL_ID
    }

    @Getter
    private final ApicurioConfig config;
    private final RegistryClient client;
    private final Cache<String, ResolvedApicurioSchema> immutableSchemaCache;
    private final Cache<String, ResolvedApicurioSchema> mutableSchemaCache;
    private final Cache<String, Boolean> missingIdCache;
    private final Map<String, String> rawVersionCache = new ConcurrentHashMap<>();

    public ApicurioSchemaRegistryClient(ApicurioConfig config) {
        this(config, RegistryClientFactory.create(config.buildClientOptions()));
    }

    public ApicurioSchemaRegistryClient(ApicurioConfig config, RegistryClient client) {
        this.config = config;
        this.client = client;
        this.immutableSchemaCache = Caffeine.newBuilder()
            .maximumSize(config.getCacheMaxSize())
            .build();
        this.mutableSchemaCache = Caffeine.newBuilder()
            .maximumSize(config.getCacheMaxSize())
            .expireAfterWrite(Duration.ofSeconds(config.getCacheTtlSeconds()))
            .build();
        this.missingIdCache = Caffeine.newBuilder()
            .maximumSize(config.getCacheMaxSize())
            .expireAfterWrite(Duration.ofSeconds(Math.min(config.getCacheTtlSeconds(), 30L)))
            .build();
    }

    public List<String> listSubjects() throws RestClientException, IOException {
        try {
            Set<String> subjects = new LinkedHashSet<>();
            int offset = 0;
            while (true) {
                final int currentOffset = offset;
                ArtifactSearchResults results = client.search().artifacts().get(req -> {
                    req.queryParameters.offset = currentOffset;
                    req.queryParameters.limit = PAGE_LIMIT;
                    if (config.getGroupId() != null && !config.getGroupId().isBlank()) {
                        req.queryParameters.groupId = config.getGroupId();
                    }
                });
                if (results == null || results.getArtifacts() == null || results.getArtifacts().isEmpty()) {
                    break;
                }
                List<SearchedArtifact> batch = results.getArtifacts();
                for (SearchedArtifact artifact : batch) {
                    if (config.isArtifactTypeIncluded(artifact.getArtifactType())) {
                        subjects.add(formatSubject(artifact.getGroupId(), artifact.getArtifactId()));
                    }
                }
                offset += batch.size();
                Integer total = results.getCount();
                if (batch.size() < PAGE_LIMIT || (total != null && offset >= total)) {
                    break;
                }
            }
            return new ArrayList<>(subjects);
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public Schema getLatestVersion(String subject) throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        try {
            VersionMetaData meta = fetchLatestActiveVersionMeta(coords);
            Schema.Config compatibility = getConfig(subject);
            if (meta == null) {
                return new Schema(null, subject, null, null, compatibility, "No active version available for '" + subject + "'");
            }
            return buildSchemaFromVersion(subject, coords, meta, compatibility);
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public List<Schema> getAllVersions(String subject) throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        try {
            List<SearchedVersion> versions = fetchAllVersionsList(coords);
            Schema.Config compatibility = getConfig(subject);

            List<Schema> schemas = new ArrayList<>();
            int fallbackIndex = 1;
            for (SearchedVersion sv : versions) {
                if (sv.getState() == VersionState.DISABLED || sv.getState() == VersionState.DRAFT) {
                    continue;
                }
                String versionStr = sv.getVersion();
                int versionNum = parseVersionNumber(versionStr, fallbackIndex);
                fallbackIndex = Math.max(fallbackIndex + 1, versionNum + 1);
                rememberVersion(coords, versionNum, versionStr);

                String rawContent = readStreamToString(
                    client.groups().byGroupId(coords.groupId())
                        .artifacts().byArtifactId(coords.artifactId())
                        .versions().byVersionExpression(versionStr)
                        .content().get()
                );
                List<ArtifactReference> outboundRefs = safeGetReferences(
                    () -> client.groups().byGroupId(coords.groupId())
                        .artifacts().byArtifactId(coords.artifactId())
                        .versions().byVersionExpression(versionStr)
                        .references().get()
                );
                schemas.add(buildSchema(
                    resolveNumericId(sv.getContentId(), sv.getGlobalId()),
                    subject,
                    versionNum,
                    sv.getArtifactType(),
                    rawContent,
                    outboundRefs,
                    compatibility
                ));
            }
            schemas.sort(Comparator.comparing(Schema::getVersion));
            return schemas;
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public List<Schema> getSubjectsBySchemaId(int id) throws IOException, RestClientException {
        try {
            List<SearchedVersion> versions = searchVersionsById(id);
            if (versions.isEmpty()) {
                return List.of();
            }
            List<Schema> result = new ArrayList<>();
            int fallbackIndex = 1;
            for (SearchedVersion v : versions) {
                if (v.getState() == VersionState.DISABLED || v.getState() == VersionState.DRAFT) {
                    continue;
                }
                if (config.getGroupId() != null && !config.getGroupId().isBlank()
                    && !ApicurioSubjectCoordinates.normalizeGroup(config.getGroupId())
                        .equals(ApicurioSubjectCoordinates.normalizeGroup(v.getGroupId()))) {
                    continue;
                }
                String subject = formatSubject(v.getGroupId(), v.getArtifactId());
                int versionNum = parseVersionNumber(v.getVersion(), fallbackIndex++);
                result.add(new Schema(id, subject, versionNum));
            }
            return result;
        } catch (ApiException e) {
            if (isNotFound(e)) {
                return List.of();
            }
            throw translateException(e);
        }
    }

    public Schema register(String subject, String type, String schemaContent, List<SchemaReference> references)
        throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        String artifactType = normalizeArtifactType(type);
        String contentType = contentTypeFor(artifactType);

        try {
            List<ArtifactReference> apicurioRefs = toApicurioReferences(references);

            VersionContent versionContent = new VersionContent();
            versionContent.setContent(schemaContent);
            versionContent.setContentType(contentType);
            if (!apicurioRefs.isEmpty()) {
                versionContent.setReferences(apicurioRefs);
            }

            CreateVersion createVersion = new CreateVersion();
            createVersion.setContent(versionContent);

            CreateArtifact createArtifact = new CreateArtifact();
            createArtifact.setArtifactId(coords.artifactId());
            createArtifact.setArtifactType(artifactType);
            createArtifact.setFirstVersion(createVersion);

            CreateArtifactResponse response = client.groups().byGroupId(coords.groupId())
                .artifacts()
                .post(createArtifact, req -> req.queryParameters.ifExists = IfArtifactExists.FIND_OR_CREATE_VERSION);

            invalidateCaches();

            if (response == null || response.getVersion() == null) {
                return getLatestVersion(subject);
            }

            VersionMetaData createdVersion = response.getVersion();
            int versionNum = parseVersionNumber(createdVersion.getVersion(), 1);
            rememberVersion(coords, versionNum, createdVersion.getVersion());

            Schema.Config compatibility = getConfig(subject);
            return buildSchema(
                resolveNumericId(createdVersion.getContentId(), createdVersion.getGlobalId()),
                subject,
                versionNum,
                createdVersion.getArtifactType() != null ? createdVersion.getArtifactType() : artifactType,
                schemaContent,
                apicurioRefs,
                compatibility
            );
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public int delete(String subject) throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        try {
            List<SearchedVersion> versions = fetchAllVersionsList(coords);
            int firstVersion = versions.isEmpty() ? 0 : parseVersionNumber(versions.get(0).getVersion(), 1);
            client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .delete();
            invalidateCaches();
            return firstVersion;
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public int deleteVersion(String subject, int version) throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        String rawVersion = rawVersionCache.getOrDefault(versionCacheKey(coords, version), String.valueOf(version));
        try {
            client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions().byVersionExpression(rawVersion)
                .delete();
            invalidateCaches();
            return version;
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public Schema.Config getDefaultConfig() throws IOException, RestClientException {
        try {
            Rule globalRule = client.admin().rules().byRuleType(RuleType.COMPATIBILITY.getValue()).get();
            return toSchemaConfig(globalRule);
        } catch (ApiException e) {
            if (isNotFound(e)) {
                return new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE);
            }
            throw translateException(e);
        }
    }

    public Schema.Config getConfig(String subject) throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        try {
            Rule artifactRule = client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .rules().byRuleType(RuleType.COMPATIBILITY.getValue())
                .get();
            return toSchemaConfig(artifactRule);
        } catch (ApiException e) {
            if (!isNotFound(e)) {
                throw translateException(e);
            }
        }

        try {
            Rule groupRule = client.groups().byGroupId(coords.groupId())
                .rules().byRuleType(RuleType.COMPATIBILITY.getValue())
                .get();
            return toSchemaConfig(groupRule);
        } catch (ApiException e) {
            if (!isNotFound(e)) {
                throw translateException(e);
            }
        }

        return getDefaultConfig();
    }

    public void updateConfig(String subject, Schema.Config compatibilityConfig) throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        String level = compatibilityConfig.getCompatibilityLevel().name();

        Rule rule = new Rule();
        rule.setRuleType(RuleType.COMPATIBILITY);
        rule.setConfig(level);

        try {
            client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .rules().byRuleType(RuleType.COMPATIBILITY.getValue())
                .put(rule);
        } catch (ApiException e) {
            if (isNotFound(e)) {
                CreateRule createRule = new CreateRule();
                createRule.setRuleType(RuleType.COMPATIBILITY);
                createRule.setConfig(level);
                try {
                    client.groups().byGroupId(coords.groupId())
                        .artifacts().byArtifactId(coords.artifactId())
                        .rules()
                        .post(createRule);
                } catch (ApiException createEx) {
                    throw translateException(createEx);
                }
            } else {
                throw translateException(e);
            }
        }
    }

    public Schema lookUpSubjectVersion(String subject, org.apache.avro.Schema avroSchema)
        throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        byte[] contentBytes = avroSchema.toString().getBytes(StandardCharsets.UTF_8);
        try {
            VersionSearchResults results = client.search().versions()
                .post(new ByteArrayInputStream(contentBytes), "application/json", req -> {
                    req.queryParameters.groupId = coords.groupId();
                    req.queryParameters.artifactId = coords.artifactId();
                    req.queryParameters.canonical = true;
                    req.queryParameters.artifactType = AvroSchema.TYPE;
                });
            if (results == null || results.getVersions() == null || results.getVersions().isEmpty()) {
                throw new RestClientException("Schema version not found for subject '" + subject + "'",
                    404, SchemaRegistryRepository.ERROR_NOT_FOUND);
            }
            SearchedVersion match = results.getVersions().get(0);
            VersionMetaData meta = client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions().byVersionExpression(match.getVersion())
                .get();
            return buildSchemaFromVersion(subject, coords, meta, getConfig(subject));
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public List<String> testCompatibility(String subject, org.apache.avro.Schema avroSchema)
        throws IOException, RestClientException {
        ApicurioSubjectCoordinates coords = parseSubject(subject);
        try {
            VersionContent versionContent = new VersionContent();
            versionContent.setContent(avroSchema.toString());
            versionContent.setContentType("application/json");

            CreateVersion createVersion = new CreateVersion();
            createVersion.setContent(versionContent);

            CreateArtifact createArtifact = new CreateArtifact();
            createArtifact.setArtifactId(coords.artifactId());
            createArtifact.setArtifactType(AvroSchema.TYPE);
            createArtifact.setFirstVersion(createVersion);

            client.groups().byGroupId(coords.groupId())
                .artifacts()
                .post(createArtifact, req -> {
                    req.queryParameters.ifExists = IfArtifactExists.CREATE_VERSION;
                    req.queryParameters.dryRun = true;
                });
            return List.of();
        } catch (RuleViolationProblemDetails e) {
            return extractRuleViolationMessages(e);
        } catch (ProblemDetails e) {
            if ("RuleViolationException".equals(e.getName())) {
                return List.of(extractApiErrorMessage(e));
            }
            throw translateException(e);
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    private static List<String> extractRuleViolationMessages(RuleViolationProblemDetails e) {
        List<String> causes = new ArrayList<>();
        if (e.getCauses() != null && !e.getCauses().isEmpty()) {
            for (RuleViolationCause cause : e.getCauses()) {
                String desc = cause.getDescription();
                if (cause.getContext() != null && !cause.getContext().isBlank()) {
                    desc = desc + " (" + cause.getContext() + ")";
                }
                causes.add(desc);
            }
        } else {
            causes.add(extractApiErrorMessage(e));
        }
        return causes;
    }

    public ResolvedApicurioSchema resolveById(long id, IdLookupHint hint) throws IOException, RestClientException {
        return switch (hint) {
            case CONTENT_ID -> resolveByContentId(id);
            case GLOBAL_ID -> resolveByGlobalId(id);
            case PREFER_CONTENT_ID -> {
                try {
                    yield resolveByContentId(id);
                } catch (RestClientException e) {
                    if (e.getStatus() == 404) {
                        yield resolveByGlobalId(id);
                    }
                    throw e;
                }
            }
            case PREFER_GLOBAL_ID -> {
                try {
                    yield resolveByGlobalId(id);
                } catch (RestClientException e) {
                    if (e.getStatus() == 404) {
                        yield resolveByContentId(id);
                    }
                    throw e;
                }
            }
        };
    }

    public ResolvedApicurioSchema resolveByContentId(long contentId) throws IOException, RestClientException {
        String cacheKey = "cid:" + contentId;
        ResolvedApicurioSchema cached = immutableSchemaCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(missingIdCache.getIfPresent(cacheKey))) {
            throw new RestClientException("Apicurio contentId " + contentId + " not found", 404, SchemaRegistryRepository.ERROR_NOT_FOUND);
        }
        try {
            String rawContent = readStreamToString(client.ids().contentIds().byContentId(contentId).get());
            List<ArtifactReference> outboundRefs = safeGetReferences(
                () -> client.ids().contentIds().byContentId(contentId).references().get()
            );
            SearchedVersion meta = findVersionMetadataByContentId(contentId);
            ResolvedApicurioSchema resolved = toResolvedSchema(contentId, meta, rawContent, outboundRefs);
            immutableSchemaCache.put(cacheKey, resolved);
            return resolved;
        } catch (ApiException e) {
            if (isNotFound(e)) {
                missingIdCache.put(cacheKey, Boolean.TRUE);
            }
            throw translateException(e);
        }
    }

    public ResolvedApicurioSchema resolveByGlobalId(long globalId) throws IOException, RestClientException {
        String cacheKey = "gid:" + globalId;
        ResolvedApicurioSchema cached = immutableSchemaCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (Boolean.TRUE.equals(missingIdCache.getIfPresent(cacheKey))) {
            throw new RestClientException("Apicurio globalId " + globalId + " not found", 404, SchemaRegistryRepository.ERROR_NOT_FOUND);
        }
        try {
            String rawContent = readStreamToString(client.ids().globalIds().byGlobalId(globalId).get());
            List<ArtifactReference> outboundRefs = safeGetReferences(
                () -> client.ids().globalIds().byGlobalId(globalId).references().get()
            );
            SearchedVersion meta = findVersionMetadataByGlobalId(globalId);
            Long contentId = meta != null ? meta.getContentId() : null;
            ResolvedApicurioSchema resolved = toResolvedSchema(contentId, globalId, meta, rawContent, outboundRefs);
            immutableSchemaCache.put(cacheKey, resolved);
            return resolved;
        } catch (ApiException e) {
            if (isNotFound(e)) {
                missingIdCache.put(cacheKey, Boolean.TRUE);
            }
            throw translateException(e);
        }
    }

    public ResolvedApicurioSchema resolveByContentHash(String contentHash) throws IOException, RestClientException {
        String cacheKey = "hash:" + contentHash;
        ResolvedApicurioSchema cached = immutableSchemaCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            String rawContent = readStreamToString(client.ids().contentHashes().byContentHash(contentHash).get());
            List<ArtifactReference> outboundRefs = safeGetReferences(
                () -> client.ids().contentHashes().byContentHash(contentHash).references().get()
            );
            SearchedVersion meta = findVersionMetadataByContent(rawContent);
            ResolvedApicurioSchema resolved = toResolvedSchema(
                meta != null ? meta.getContentId() : null,
                meta != null ? meta.getGlobalId() : null,
                meta,
                rawContent,
                outboundRefs
            );
            immutableSchemaCache.put(cacheKey, resolved);
            return resolved;
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public ResolvedApicurioSchema resolveByCoordinates(String groupId, String artifactId, String version)
        throws IOException, RestClientException {
        String normalizedGroup = ApicurioSubjectCoordinates.normalizeGroup(
            (groupId == null || groupId.isBlank()) ? config.getGroupId() : groupId
        );
        String versionExpr = (version == null || version.isBlank()) ? LATEST_BRANCH_EXPRESSION : version;
        String cacheKey = "gav:" + normalizedGroup + ":" + artifactId + ":" + versionExpr;
        ResolvedApicurioSchema cached = mutableSchemaCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            VersionMetaData meta = client.groups().byGroupId(normalizedGroup)
                .artifacts().byArtifactId(artifactId)
                .versions().byVersionExpression(versionExpr)
                .get();
            String resolvedVersion = meta.getVersion() != null ? meta.getVersion() : versionExpr;
            String rawContent = readStreamToString(
                client.groups().byGroupId(normalizedGroup)
                    .artifacts().byArtifactId(artifactId)
                    .versions().byVersionExpression(resolvedVersion)
                    .content().get()
            );
            List<ArtifactReference> outboundRefs = safeGetReferences(
                () -> client.groups().byGroupId(normalizedGroup)
                    .artifacts().byArtifactId(artifactId)
                    .versions().byVersionExpression(resolvedVersion)
                    .references().get()
            );
            String artifactType = meta.getArtifactType() != null ? meta.getArtifactType() : inferArtifactType(rawContent);
            ReferenceResolutionResult refResult = resolveReferencesRecursively(outboundRefs);
            ParsedSchema parsed = parseSchema(artifactType, rawContent, refResult.schemaReferences, refResult.resolvedContents);
            String subject = formatSubject(normalizedGroup, artifactId);
            ResolvedApicurioSchema resolved = new ResolvedApicurioSchema(
                meta.getContentId(),
                meta.getGlobalId(),
                normalizedGroup,
                artifactId,
                subject,
                resolvedVersion,
                artifactType,
                rawContent,
                parsed,
                refResult.schemaReferences
            );
            mutableSchemaCache.put(cacheKey, resolved);
            return resolved;
        } catch (ApiException e) {
            throw translateException(e);
        }
    }

    public ResolvedApicurioSchema parseInlineSchema(String rawContent, String schemaType) {
        String artifactType = (schemaType != null && !schemaType.isBlank())
            ? schemaType.trim().toUpperCase(Locale.ROOT)
            : inferArtifactType(rawContent);
        ParsedSchema parsed = parseSchema(artifactType, rawContent, List.of(), Map.of());
        return new ResolvedApicurioSchema(
            null,
            null,
            null,
            null,
            null,
            null,
            artifactType,
            rawContent,
            parsed,
            List.of()
        );
    }

    public void invalidateCaches() {
        immutableSchemaCache.invalidateAll();
        mutableSchemaCache.invalidateAll();
        missingIdCache.invalidateAll();
    }

    public ApicurioSubjectCoordinates parseSubject(String subject) {
        return ApicurioSubjectCoordinates.parse(subject, config.getGroupId());
    }

    public String formatSubject(String groupId, String artifactId) {
        return ApicurioSubjectCoordinates.format(groupId, artifactId, config.getGroupId());
    }

    public IdLookupHint defaultLookupHintForConfiguredUseId() {
        return switch (config.getUseId()) {
            case GLOBAL_ID -> IdLookupHint.GLOBAL_ID;
            case CONTENT_ID -> IdLookupHint.CONTENT_ID;
            case AUTO -> IdLookupHint.PREFER_CONTENT_ID;
        };
    }

    private VersionMetaData fetchLatestActiveVersionMeta(ApicurioSubjectCoordinates coords) throws RestClientException {
        try {
            return client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions().byVersionExpression(LATEST_BRANCH_EXPRESSION)
                .get();
        } catch (ApiException e) {
            if (!isNotFound(e)) {
                throw translateException(e);
            }
            // Fall back to searching versions list in case the latest branch is absent or disabled
            List<SearchedVersion> versions = fetchAllVersionsList(coords);
            Optional<SearchedVersion> latestActive = versions.stream()
                .filter(v -> v.getState() != VersionState.DISABLED && v.getState() != VersionState.DRAFT)
                .max(Comparator.comparingInt(v -> parseVersionNumber(v.getVersion(), 0)));
            if (latestActive.isEmpty()) {
                // Artifact exists in registry, but all of its versions are disabled/draft (or it has none).
                return null;
            }
            return client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions().byVersionExpression(latestActive.get().getVersion())
                .get();
        }
    }

    private List<SearchedVersion> fetchAllVersionsList(ApicurioSubjectCoordinates coords) {
        List<SearchedVersion> all = new ArrayList<>();
        int offset = 0;
        while (true) {
            final int currentOffset = offset;
            VersionSearchResults results = client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions()
                .get(req -> {
                    req.queryParameters.offset = currentOffset;
                    req.queryParameters.limit = PAGE_LIMIT;
                });
            if (results == null || results.getVersions() == null || results.getVersions().isEmpty()) {
                break;
            }
            List<SearchedVersion> batch = results.getVersions();
            all.addAll(batch);
            offset += batch.size();
            Integer total = results.getCount();
            if (batch.size() < PAGE_LIMIT || (total != null && offset >= total)) {
                break;
            }
        }
        return all;
    }

    private List<SearchedVersion> searchVersionsById(long id) {
        ApicurioConfig.UseId useId = config.getUseId();
        if (useId == ApicurioConfig.UseId.GLOBAL_ID) {
            return queryVersionsByGlobalId(id);
        }
        if (useId == ApicurioConfig.UseId.CONTENT_ID) {
            return queryVersionsByContentId(id);
        }
        // AUTO mode: include contentId matches first (v3 default), followed by any distinct globalId matches
        // so both contentId-encoded and legacy globalId-encoded messages resolve when filtered by topic.
        List<SearchedVersion> byContentId = queryVersionsByContentId(id);
        List<SearchedVersion> byGlobalId = queryVersionsByGlobalId(id);
        if (byGlobalId.isEmpty()) {
            return byContentId;
        }
        if (byContentId.isEmpty()) {
            return byGlobalId;
        }
        Map<String, SearchedVersion> merged = new LinkedHashMap<>();
        for (SearchedVersion v : byContentId) {
            merged.put(versionIdentityKey(v), v);
        }
        for (SearchedVersion v : byGlobalId) {
            merged.putIfAbsent(versionIdentityKey(v), v);
        }
        return new ArrayList<>(merged.values());
    }

    private static String versionIdentityKey(SearchedVersion v) {
        return ApicurioSubjectCoordinates.normalizeGroup(v.getGroupId()) + ":" + v.getArtifactId() + ":" + v.getVersion();
    }

    private List<SearchedVersion> queryVersionsByContentId(long contentId) {
        VersionSearchResults results = client.search().versions().get(req -> {
            req.queryParameters.contentId = contentId;
            req.queryParameters.limit = PAGE_LIMIT;
        });
        return results != null && results.getVersions() != null ? results.getVersions() : List.of();
    }

    private List<SearchedVersion> queryVersionsByGlobalId(long globalId) {
        VersionSearchResults results = client.search().versions().get(req -> {
            req.queryParameters.globalId = globalId;
            req.queryParameters.limit = PAGE_LIMIT;
        });
        return results != null && results.getVersions() != null ? results.getVersions() : List.of();
    }

    private SearchedVersion findVersionMetadataByContentId(long contentId) {
        try {
            return selectPreferredVersion(queryVersionsByContentId(contentId));
        } catch (ApiException e) {
            log.debug("Could not fetch version metadata for contentId {}", contentId, e);
            return null;
        }
    }

    private SearchedVersion findVersionMetadataByGlobalId(long globalId) {
        try {
            return selectPreferredVersion(queryVersionsByGlobalId(globalId));
        } catch (ApiException e) {
            log.debug("Could not fetch version metadata for globalId {}", globalId, e);
            return null;
        }
    }

    private SearchedVersion findVersionMetadataByContent(String rawContent) {
        try {
            String contentType = rawContent.trim().startsWith("{") ? "application/json" : "application/x-protobuf";
            VersionSearchResults results = client.search().versions()
                .post(new ByteArrayInputStream(rawContent.getBytes(StandardCharsets.UTF_8)), contentType, req -> {
                    req.queryParameters.limit = 10;
                });
            if (results != null && results.getVersions() != null) {
                return selectPreferredVersion(results.getVersions());
            }
        } catch (ApiException e) {
            log.debug("Could not fetch version metadata by content hash", e);
        }
        return null;
    }

    private SearchedVersion selectPreferredVersion(List<SearchedVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        List<SearchedVersion> active = versions.stream()
            .filter(v -> v.getState() != VersionState.DISABLED && v.getState() != VersionState.DRAFT)
            .toList();
        List<SearchedVersion> candidates = active.isEmpty() ? versions : active;
        if (config.getGroupId() != null && !config.getGroupId().isBlank()) {
            String expectedGroup = ApicurioSubjectCoordinates.normalizeGroup(config.getGroupId());
            for (SearchedVersion candidate : candidates) {
                if (expectedGroup.equals(ApicurioSubjectCoordinates.normalizeGroup(candidate.getGroupId()))) {
                    return candidate;
                }
            }
        }
        return candidates.get(0);
    }

    private ResolvedApicurioSchema toResolvedSchema(
        Long contentId,
        SearchedVersion meta,
        String rawContent,
        List<ArtifactReference> outboundRefs
    ) throws IOException {
        Long globalId = meta != null ? meta.getGlobalId() : null;
        return toResolvedSchema(contentId, globalId, meta, rawContent, outboundRefs);
    }

    private ResolvedApicurioSchema toResolvedSchema(
        Long contentId,
        Long globalId,
        SearchedVersion meta,
        String rawContent,
        List<ArtifactReference> outboundRefs
    ) throws IOException {
        String groupId = meta != null ? ApicurioSubjectCoordinates.normalizeGroup(meta.getGroupId()) : null;
        String artifactId = meta != null ? meta.getArtifactId() : null;
        String subject = artifactId != null ? formatSubject(groupId, artifactId) : null;
        String version = meta != null ? meta.getVersion() : null;
        String artifactType = (meta != null && meta.getArtifactType() != null)
            ? meta.getArtifactType().toUpperCase(Locale.ROOT)
            : inferArtifactType(rawContent);

        ReferenceResolutionResult refResult = resolveReferencesRecursively(outboundRefs);
        ParsedSchema parsed = parseSchema(artifactType, rawContent, refResult.schemaReferences, refResult.resolvedContents);

        return new ResolvedApicurioSchema(
            contentId,
            globalId,
            groupId,
            artifactId,
            subject,
            version,
            artifactType,
            rawContent,
            parsed,
            refResult.schemaReferences
        );
    }

    private Schema buildSchemaFromVersion(
        String subject,
        ApicurioSubjectCoordinates coords,
        VersionMetaData meta,
        Schema.Config compatibility
    ) throws IOException {
        String versionStr = meta.getVersion();
        int versionNum = parseVersionNumber(versionStr, 1);
        rememberVersion(coords, versionNum, versionStr);

        String rawContent = readStreamToString(
            client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions().byVersionExpression(versionStr)
                .content().get()
        );
        List<ArtifactReference> outboundRefs = safeGetReferences(
            () -> client.groups().byGroupId(coords.groupId())
                .artifacts().byArtifactId(coords.artifactId())
                .versions().byVersionExpression(versionStr)
                .references().get()
        );
        return buildSchema(
            resolveNumericId(meta.getContentId(), meta.getGlobalId()),
            subject,
            versionNum,
            meta.getArtifactType(),
            rawContent,
            outboundRefs,
            compatibility
        );
    }

    private Schema buildSchema(
        Integer id,
        String subject,
        Integer version,
        String artifactType,
        String rawContent,
        List<ArtifactReference> outboundRefs,
        Schema.Config compatibility
    ) throws IOException {
        String normalizedType = artifactType != null ? artifactType.toUpperCase(Locale.ROOT) : inferArtifactType(rawContent);
        ReferenceResolutionResult refResult = resolveReferencesRecursively(outboundRefs);
        ParsedSchema parsedSchema = parseSchema(normalizedType, rawContent, refResult.schemaReferences, refResult.resolvedContents);
        return new Schema(id, subject, version, normalizedType, parsedSchema, compatibility);
    }

    private ReferenceResolutionResult resolveReferencesRecursively(List<ArtifactReference> outboundRefs) throws IOException {
        if (outboundRefs == null || outboundRefs.isEmpty()) {
            return new ReferenceResolutionResult(List.of(), Map.of());
        }
        List<SchemaReference> directRefs = new ArrayList<>();
        // Post-order insertion ensures leaf references come before parent references for Avro schema parsing.
        Map<String, String> resolvedContents = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();

        for (ArtifactReference ref : outboundRefs) {
            String refGroup = ApicurioSubjectCoordinates.normalizeGroup(ref.getGroupId());
            String refArtifact = ref.getArtifactId();
            String refVersion = (ref.getVersion() == null || ref.getVersion().isBlank())
                ? LATEST_BRANCH_EXPRESSION
                : ref.getVersion();

            directRefs.add(new SchemaReference(
                ref.getName(),
                formatSubject(refGroup, refArtifact),
                parseVersionNumber(ref.getVersion(), 1)
            ));
            resolveReferenceTree(refGroup, refArtifact, refVersion, ref.getName(), resolvedContents, visited);
        }
        return new ReferenceResolutionResult(directRefs, resolvedContents);
    }

    private void resolveReferenceTree(
        String groupId,
        String artifactId,
        String version,
        String refName,
        Map<String, String> resolvedContents,
        Set<String> visited
    ) throws IOException {
        String key = groupId + ":" + artifactId + ":" + version;
        if (!visited.add(key)) {
            return;
        }
        List<ArtifactReference> nestedRefs = safeGetReferences(
            () -> client.groups().byGroupId(groupId)
                .artifacts().byArtifactId(artifactId)
                .versions().byVersionExpression(version)
                .references().get()
        );
        for (ArtifactReference nested : nestedRefs) {
            String nestedGroup = ApicurioSubjectCoordinates.normalizeGroup(nested.getGroupId());
            String nestedVersion = (nested.getVersion() == null || nested.getVersion().isBlank())
                ? LATEST_BRANCH_EXPRESSION
                : nested.getVersion();
            resolveReferenceTree(nestedGroup, nested.getArtifactId(), nestedVersion, nested.getName(), resolvedContents, visited);
        }
        String content = readStreamToString(
            client.groups().byGroupId(groupId)
                .artifacts().byArtifactId(artifactId)
                .versions().byVersionExpression(version)
                .content().get()
        );
        if (refName != null) {
            resolvedContents.put(refName, content);
        }
    }

    private List<ArtifactReference> toApicurioReferences(List<SchemaReference> references) {
        if (references == null || references.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArtifactReference> result = new ArrayList<>(references.size());
        for (SchemaReference ref : references) {
            ApicurioSubjectCoordinates refCoords = parseSubject(ref.getSubject());
            String version;
            if (ref.getVersion() == null || ref.getVersion() <= 0) {
                VersionMetaData latest = client.groups().byGroupId(refCoords.groupId())
                    .artifacts().byArtifactId(refCoords.artifactId())
                    .versions().byVersionExpression(LATEST_BRANCH_EXPRESSION)
                    .get();
                version = latest.getVersion();
            } else {
                version = rawVersionCache.getOrDefault(
                    versionCacheKey(refCoords, ref.getVersion()),
                    String.valueOf(ref.getVersion())
                );
            }
            ArtifactReference ar = new ArtifactReference();
            ar.setGroupId(refCoords.groupId());
            ar.setArtifactId(refCoords.artifactId());
            ar.setVersion(version);
            ar.setName(ref.getName());
            result.add(ar);
        }
        return result;
    }

    static ParsedSchema parseSchema(
        String artifactType,
        String rawContent,
        List<SchemaReference> references,
        Map<String, String> resolvedReferences
    ) {
        if (rawContent == null) {
            return null;
        }
        String type = artifactType != null ? artifactType.toUpperCase(Locale.ROOT) : inferArtifactType(rawContent);
        try {
            return switch (type) {
                case JsonSchema.TYPE -> new JsonSchema(rawContent, references, resolvedReferences, null);
                case ProtobufSchema.TYPE -> new ProtobufSchema(rawContent, references, resolvedReferences, null, null);
                case AvroSchema.TYPE -> new AvroSchema(rawContent, references, resolvedReferences, null, false);
                default -> {
                    log.debug("Unsupported Apicurio artifact type '{}' for ParsedSchema parsing", type);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.warn("Failed to parse Apicurio {} schema: {}", type, e.getMessage());
            return null;
        }
    }

    static String inferArtifactType(String rawContent) {
        if (rawContent == null) {
            return AvroSchema.TYPE;
        }
        String trimmed = rawContent.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")) {
            try {
                JsonNode node = JSON_MAPPER.readTree(trimmed);
                if (node.isObject()) {
                    if (node.has("$schema") || node.has("properties") || node.has("definitions") || node.has("$defs")) {
                        return JsonSchema.TYPE;
                    }
                    if (node.has("type")) {
                        JsonNode typeNode = node.get("type");
                        if (typeNode.isTextual() && "object".equals(typeNode.asText())) {
                            return JsonSchema.TYPE;
                        }
                        return AvroSchema.TYPE;
                    }
                }
            } catch (Exception ignored) {
                // Not valid JSON, fall through to protobuf check
            }
        }
        if (trimmed.contains("syntax") || trimmed.contains("message ")) {
            return ProtobufSchema.TYPE;
        }
        return AvroSchema.TYPE;
    }

    private Integer resolveNumericId(Long contentId, Long globalId) {
        Long preferred = config.getUseId() == ApicurioConfig.UseId.GLOBAL_ID
            ? (globalId != null ? globalId : contentId)
            : (contentId != null ? contentId : globalId);
        if (preferred == null) {
            return null;
        }
        return preferred.intValue();
    }

    private static Schema.Config toSchemaConfig(Rule rule) {
        if (rule == null || rule.getConfig() == null || rule.getConfig().isBlank()) {
            return new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE);
        }
        try {
            return new Schema.Config(Schema.Config.CompatibilityLevelConfig.valueOf(rule.getConfig().trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown Apicurio compatibility level '{}', falling back to NONE", rule.getConfig());
            return new Schema.Config(Schema.Config.CompatibilityLevelConfig.NONE);
        }
    }

    private static String normalizeArtifactType(String type) {
        if (type == null || type.isBlank()) {
            return AvroSchema.TYPE;
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private static String contentTypeFor(String artifactType) {
        if (ProtobufSchema.TYPE.equals(artifactType)) {
            return "application/x-protobuf";
        }
        return "application/json";
    }

    private static int parseVersionNumber(String version, int fallback) {
        if (version == null || version.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(version.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void rememberVersion(ApicurioSubjectCoordinates coords, int versionNum, String rawVersion) {
        if (rawVersion != null && !rawVersion.equals(String.valueOf(versionNum))) {
            rawVersionCache.put(versionCacheKey(coords, versionNum), rawVersion);
        }
    }

    private static String versionCacheKey(ApicurioSubjectCoordinates coords, int versionNum) {
        return coords.groupId() + ":" + coords.artifactId() + ":" + versionNum;
    }

    private List<ArtifactReference> safeGetReferences(ReferenceSupplier supplier) {
        try {
            List<ArtifactReference> refs = supplier.get();
            return refs != null ? refs : List.of();
        } catch (ApiException e) {
            if (isNotFound(e)) {
                return List.of();
            }
            throw e;
        }
    }

    private static String readStreamToString(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean isNotFound(ApiException e) {
        if (e.getResponseStatusCode() == 404) {
            return true;
        }
        if (e instanceof ProblemDetails pd && pd.getStatus() != null && pd.getStatus() == 404) {
            return true;
        }
        return false;
    }

    private static RestClientException translateException(ApiException e) {
        int status = e.getResponseStatusCode();
        if (status <= 0 && e instanceof ProblemDetails pd && pd.getStatus() != null) {
            status = pd.getStatus();
        }
        if (status <= 0 && e instanceof RuleViolationProblemDetails rv && rv.getStatus() != null) {
            status = rv.getStatus();
        }
        int errorCode = (status == 404) ? SchemaRegistryRepository.ERROR_NOT_FOUND : status;
        return new RestClientException(extractApiErrorMessage(e), status, errorCode);
    }

    private static String extractApiErrorMessage(ApiException e) {
        if (e instanceof RuleViolationProblemDetails rv) {
            String title = rv.getTitle() != null ? rv.getTitle().trim() : "";
            String detail = rv.getDetail() != null ? rv.getDetail().trim() : "";
            StringBuilder sb = new StringBuilder();
            if (!detail.isEmpty()) {
                if (!title.isEmpty() && !detail.startsWith(title)) {
                    sb.append(title).append(": ");
                }
                sb.append(detail);
            } else if (!title.isEmpty()) {
                sb.append(title);
            }
            if (rv.getCauses() != null && !rv.getCauses().isEmpty() && !detail.contains("Causes:")) {
                String causes = rv.getCauses().stream()
                    .map(c -> c.getContext() != null && !c.getContext().isBlank()
                        ? c.getDescription() + " (" + c.getContext() + ")"
                        : c.getDescription())
                    .collect(Collectors.joining("; "));
                if (!sb.isEmpty()) {
                    sb.append(" - ");
                }
                sb.append(causes);
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }
        if (e instanceof ProblemDetails pd) {
            if (pd.getDetail() != null && !pd.getDetail().isBlank()) {
                return pd.getDetail();
            }
            if (pd.getTitle() != null && !pd.getTitle().isBlank()) {
                return pd.getTitle();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Apicurio Registry error (HTTP " + e.getResponseStatusCode() + ")";
    }

    @FunctionalInterface
    private interface ReferenceSupplier {
        List<ArtifactReference> get();
    }

    private record ReferenceResolutionResult(
        List<SchemaReference> schemaReferences,
        Map<String, String> resolvedContents
    ) {}
}

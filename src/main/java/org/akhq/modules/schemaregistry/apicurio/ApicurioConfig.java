package org.akhq.modules.schemaregistry.apicurio;

import io.apicurio.registry.client.common.HttpAdapterType;
import io.apicurio.registry.client.common.RegistryClientOptions;
import lombok.Getter;
import org.akhq.configs.Connection;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configuration model for Apicurio Schema Registry connections.
 * Reads top-level {@link Connection.SchemaRegistry} settings together with optional
 * Apicurio-specific keys inside {@code schema-registry.properties}.
 */
@Getter
public class ApicurioConfig {
    public static final String PROP_GROUP_ID = "apicurio.registry.group-id";
    public static final String PROP_USE_ID = "apicurio.registry.use-id";
    public static final String PROP_ID_HANDLER = "apicurio.registry.id-handler";
    public static final String PROP_SEND_TYPE_REF = "apicurio.registry.serde.send-type-ref";
    public static final String PROP_SEND_INDEXES = "apicurio.registry.serde.send-indexes";
    public static final String PROP_ARTIFACT_TYPES = "apicurio.registry.artifact-types";
    public static final String PROP_CACHE_MAX_SIZE = "apicurio.registry.cache.max-size";
    public static final String PROP_CACHE_TTL_SECONDS = "apicurio.registry.cache.ttl-seconds";

    // Auth properties
    public static final String PROP_AUTH_TOKEN_ENDPOINT = "apicurio.auth.token-endpoint";
    public static final String PROP_AUTH_CLIENT_ID = "apicurio.auth.client-id";
    public static final String PROP_AUTH_CLIENT_SECRET = "apicurio.auth.client-secret";
    public static final String PROP_AUTH_SCOPE = "apicurio.auth.scope";
    public static final String PROP_AUTH_BEARER_TOKEN = "apicurio.auth.bearer-token";
    public static final String PROP_AUTH_USERNAME = "apicurio.auth.username";
    public static final String PROP_AUTH_PASSWORD = "apicurio.auth.password";

    // TLS properties
    public static final String PROP_TLS_TRUST_ALL = "apicurio.tls.trust-all";
    public static final String PROP_TLS_VERIFY_HOST = "apicurio.tls.verify-host";
    public static final String PROP_TLS_TRUSTSTORE_LOCATION = "apicurio.tls.truststore.location";
    public static final String PROP_TLS_TRUSTSTORE_PASSWORD = "apicurio.tls.truststore.password";
    public static final String PROP_TLS_TRUSTSTORE_TYPE = "apicurio.tls.truststore.type";
    public static final String PROP_TLS_KEYSTORE_LOCATION = "apicurio.tls.keystore.location";
    public static final String PROP_TLS_KEYSTORE_PASSWORD = "apicurio.tls.keystore.password";
    public static final String PROP_TLS_KEYSTORE_TYPE = "apicurio.tls.keystore.type";
    public static final String PROP_TLS_CLIENT_KEY = "apicurio.tls.client.key";

    // Proxy & HTTP properties
    public static final String PROP_PROXY_HOST = "apicurio.proxy.host";
    public static final String PROP_PROXY_PORT = "apicurio.proxy.port";
    public static final String PROP_PROXY_USERNAME = "apicurio.proxy.username";
    public static final String PROP_PROXY_PASSWORD = "apicurio.proxy.password";
    public static final String PROP_CONNECT_TIMEOUT_MS = "apicurio.http.connect-timeout-ms";
    public static final String PROP_READ_TIMEOUT_MS = "apicurio.http.read-timeout-ms";

    // Standard Apicurio Kafka SerDe header names & overrides
    public static final String DEFAULT_KEY_GLOBAL_ID_HEADER = "apicurio.key.globalId";
    public static final String DEFAULT_VALUE_GLOBAL_ID_HEADER = "apicurio.value.globalId";
    public static final String DEFAULT_KEY_CONTENT_ID_HEADER = "apicurio.key.contentId";
    public static final String DEFAULT_VALUE_CONTENT_ID_HEADER = "apicurio.value.contentId";
    public static final String DEFAULT_KEY_CONTENT_HASH_HEADER = "apicurio.key.contentHash";
    public static final String DEFAULT_VALUE_CONTENT_HASH_HEADER = "apicurio.value.contentHash";
    public static final String DEFAULT_KEY_GROUP_ID_HEADER = "apicurio.key.groupId";
    public static final String DEFAULT_VALUE_GROUP_ID_HEADER = "apicurio.value.groupId";
    public static final String DEFAULT_KEY_ARTIFACT_ID_HEADER = "apicurio.key.artifactId";
    public static final String DEFAULT_VALUE_ARTIFACT_ID_HEADER = "apicurio.value.artifactId";
    public static final String DEFAULT_KEY_VERSION_HEADER = "apicurio.key.version";
    public static final String DEFAULT_VALUE_VERSION_HEADER = "apicurio.value.version";
    public static final String DEFAULT_KEY_MSG_TYPE_HEADER = "apicurio.key.msgType";
    public static final String DEFAULT_VALUE_MSG_TYPE_HEADER = "apicurio.value.msgType";
    public static final String DEFAULT_KEY_ENCODING_HEADER = "apicurio.key.encoding";
    public static final String DEFAULT_VALUE_ENCODING_HEADER = "apicurio.value.encoding";
    public static final String DEFAULT_KEY_SCHEMA_HEADER = "apicurio.key.schema";
    public static final String DEFAULT_VALUE_SCHEMA_HEADER = "apicurio.value.schema";
    public static final String DEFAULT_KEY_SCHEMA_TYPE_HEADER = "apicurio.key.schemaType";
    public static final String DEFAULT_VALUE_SCHEMA_TYPE_HEADER = "apicurio.value.schemaType";

    private static final Set<String> DEFAULT_ARTIFACT_TYPES = Set.of("AVRO", "JSON", "PROTOBUF");

    public enum UseId {
        AUTO,
        CONTENT_ID,
        GLOBAL_ID
    }

    public enum IdHandlerMode {
        AUTO,
        FOUR_BYTE,
        EIGHT_BYTE
    }

    private final String url;
    private final String basicAuthUsername;
    private final String basicAuthPassword;
    private final String groupId;
    private final UseId useId;
    private final IdHandlerMode idHandlerMode;
    private final boolean protobufSendTypeRef;
    private final boolean protobufSendIndexes;
    private final Set<String> artifactTypes;
    private final long cacheMaxSize;
    private final long cacheTtlSeconds;
    private final Map<String, String> properties;

    // Header names
    private final String keyGlobalIdHeader;
    private final String valueGlobalIdHeader;
    private final String keyContentIdHeader;
    private final String valueContentIdHeader;
    private final String keyContentHashHeader;
    private final String valueContentHashHeader;
    private final String keyGroupIdHeader;
    private final String valueGroupIdHeader;
    private final String keyArtifactIdHeader;
    private final String valueArtifactIdHeader;
    private final String keyVersionHeader;
    private final String valueVersionHeader;
    private final String keyMsgTypeHeader;
    private final String valueMsgTypeHeader;
    private final String keyEncodingHeader;
    private final String valueEncodingHeader;
    private final String keySchemaHeader;
    private final String valueSchemaHeader;
    private final String keySchemaTypeHeader;
    private final String valueSchemaTypeHeader;

    public ApicurioConfig(Connection.SchemaRegistry schemaRegistry) {
        if (schemaRegistry == null) {
            throw new IllegalArgumentException("SchemaRegistry configuration cannot be null");
        }
        this.url = schemaRegistry.getUrl();
        this.properties = schemaRegistry.getProperties() != null
            ? Collections.unmodifiableMap(schemaRegistry.getProperties())
            : Collections.emptyMap();

        this.basicAuthUsername = firstNonBlank(schemaRegistry.getBasicAuthUsername(), getProperty(PROP_AUTH_USERNAME));
        this.basicAuthPassword = firstNonBlank(schemaRegistry.getBasicAuthPassword(), getProperty(PROP_AUTH_PASSWORD));

        String configuredGroup = firstNonBlank(schemaRegistry.getApicurioGroupId(), getProperty(PROP_GROUP_ID));
        this.groupId = configuredGroup != null ? configuredGroup.trim() : null;

        this.useId = parseUseId(getProperty(PROP_USE_ID));
        this.idHandlerMode = parseIdHandlerMode(getProperty(PROP_ID_HANDLER));
        this.protobufSendTypeRef = parseBoolean(getProperty(PROP_SEND_TYPE_REF), true);
        this.protobufSendIndexes = parseBoolean(getProperty(PROP_SEND_INDEXES), false);
        this.artifactTypes = parseArtifactTypes(getProperty(PROP_ARTIFACT_TYPES));
        this.cacheMaxSize = parseLong(getProperty(PROP_CACHE_MAX_SIZE), 1000L);
        this.cacheTtlSeconds = parseLong(getProperty(PROP_CACHE_TTL_SECONDS), 300L);

        this.keyGlobalIdHeader = getPropertyOrDefault("apicurio.registry.headers.key.globalId.name", DEFAULT_KEY_GLOBAL_ID_HEADER);
        this.valueGlobalIdHeader = getPropertyOrDefault("apicurio.registry.headers.value.globalId.name", DEFAULT_VALUE_GLOBAL_ID_HEADER);
        this.keyContentIdHeader = getPropertyOrDefault("apicurio.registry.headers.key.contentId.name", DEFAULT_KEY_CONTENT_ID_HEADER);
        this.valueContentIdHeader = getPropertyOrDefault("apicurio.registry.headers.value.contentId.name", DEFAULT_VALUE_CONTENT_ID_HEADER);
        this.keyContentHashHeader = getPropertyOrDefault("apicurio.registry.headers.key.contentHash.name", DEFAULT_KEY_CONTENT_HASH_HEADER);
        this.valueContentHashHeader = getPropertyOrDefault("apicurio.registry.headers.value.contentHash.name", DEFAULT_VALUE_CONTENT_HASH_HEADER);
        this.keyGroupIdHeader = getPropertyOrDefault("apicurio.registry.headers.key.groupId.name", DEFAULT_KEY_GROUP_ID_HEADER);
        this.valueGroupIdHeader = getPropertyOrDefault("apicurio.registry.headers.value.groupId.name", DEFAULT_VALUE_GROUP_ID_HEADER);
        this.keyArtifactIdHeader = getPropertyOrDefault("apicurio.registry.headers.key.artifactId.name", DEFAULT_KEY_ARTIFACT_ID_HEADER);
        this.valueArtifactIdHeader = getPropertyOrDefault("apicurio.registry.headers.value.artifactId.name", DEFAULT_VALUE_ARTIFACT_ID_HEADER);
        this.keyVersionHeader = getPropertyOrDefault("apicurio.registry.headers.key.version.name", DEFAULT_KEY_VERSION_HEADER);
        this.valueVersionHeader = getPropertyOrDefault("apicurio.registry.headers.value.version.name", DEFAULT_VALUE_VERSION_HEADER);
        this.keyMsgTypeHeader = getPropertyOrDefault("apicurio.registry.headers.key.msgType.name", DEFAULT_KEY_MSG_TYPE_HEADER);
        this.valueMsgTypeHeader = getPropertyOrDefault("apicurio.registry.headers.value.msgType.name", DEFAULT_VALUE_MSG_TYPE_HEADER);
        this.keyEncodingHeader = getPropertyOrDefault("apicurio.registry.headers.key.encoding.name", DEFAULT_KEY_ENCODING_HEADER);
        this.valueEncodingHeader = getPropertyOrDefault("apicurio.registry.headers.value.encoding.name", DEFAULT_VALUE_ENCODING_HEADER);
        this.keySchemaHeader = getPropertyOrDefault("apicurio.registry.headers.key.schema.name", DEFAULT_KEY_SCHEMA_HEADER);
        this.valueSchemaHeader = getPropertyOrDefault("apicurio.registry.headers.value.schema.name", DEFAULT_VALUE_SCHEMA_HEADER);
        this.keySchemaTypeHeader = getPropertyOrDefault("apicurio.registry.headers.key.schemaType.name", DEFAULT_KEY_SCHEMA_TYPE_HEADER);
        this.valueSchemaTypeHeader = getPropertyOrDefault("apicurio.registry.headers.value.schemaType.name", DEFAULT_VALUE_SCHEMA_TYPE_HEADER);
    }

    public boolean isArtifactTypeIncluded(String artifactType) {
        if (artifactTypes.isEmpty()) {
            return true;
        }
        if (artifactType == null) {
            return false;
        }
        return artifactTypes.contains(artifactType.toUpperCase(Locale.ROOT));
    }

    public int getWriteIdSize() {
        return idHandlerMode == IdHandlerMode.EIGHT_BYTE ? 8 : 4;
    }

    public RegistryClientOptions buildClientOptions() {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Apicurio schema registry URL must be configured");
        }
        RegistryClientOptions options = RegistryClientOptions.create(url)
            .httpAdapter(HttpAdapterType.JDK);

        configureAuth(options);
        configureTls(options);
        configureProxy(options);
        configureTimeouts(options);

        return options;
    }

    private void configureAuth(RegistryClientOptions options) {
        String tokenEndpoint = getProperty(PROP_AUTH_TOKEN_ENDPOINT);
        String bearerToken = getProperty(PROP_AUTH_BEARER_TOKEN);
        if (tokenEndpoint != null && !tokenEndpoint.isBlank()) {
            String clientId = getProperty(PROP_AUTH_CLIENT_ID);
            String clientSecret = getProperty(PROP_AUTH_CLIENT_SECRET);
            String scope = getProperty(PROP_AUTH_SCOPE);
            options.oauth2(tokenEndpoint, clientId, clientSecret, scope);
        } else if (bearerToken != null && !bearerToken.isBlank()) {
            options.bearerToken(bearerToken);
        } else if (basicAuthUsername != null && !basicAuthUsername.isBlank()) {
            options.basicAuth(basicAuthUsername, basicAuthPassword);
        }
    }

    private void configureTls(RegistryClientOptions options) {
        if (!parseBoolean(getProperty(PROP_TLS_VERIFY_HOST), true)) {
            options.verifyHost(false);
        }

        if (parseBoolean(getProperty(PROP_TLS_TRUST_ALL), false)) {
            options.trustAll(true);
        } else {
            String truststoreLocation = firstNonBlank(
                getProperty(PROP_TLS_TRUSTSTORE_LOCATION),
                getProperty("ssl.truststore.location"),
                getProperty("schema.registry.ssl.truststore.location")
            );
            if (truststoreLocation != null) {
                String truststorePassword = firstNonBlank(
                    getProperty(PROP_TLS_TRUSTSTORE_PASSWORD),
                    getProperty("ssl.truststore.password"),
                    getProperty("schema.registry.ssl.truststore.password")
                );
                String truststoreType = firstNonBlank(
                    getProperty(PROP_TLS_TRUSTSTORE_TYPE),
                    getProperty("ssl.truststore.type"),
                    getProperty("schema.registry.ssl.truststore.type"),
                    "JKS"
                );
                applyTrustStore(options, truststoreLocation, truststorePassword, truststoreType);
            }
        }

        String keystoreLocation = firstNonBlank(
            getProperty(PROP_TLS_KEYSTORE_LOCATION),
            getProperty("ssl.keystore.location"),
            getProperty("schema.registry.ssl.keystore.location")
        );
        if (keystoreLocation != null) {
            String keystorePassword = firstNonBlank(
                getProperty(PROP_TLS_KEYSTORE_PASSWORD),
                getProperty("ssl.keystore.password"),
                getProperty("schema.registry.ssl.keystore.password")
            );
            String keystoreType = firstNonBlank(
                getProperty(PROP_TLS_KEYSTORE_TYPE),
                getProperty("ssl.keystore.type"),
                getProperty("schema.registry.ssl.keystore.type"),
                "JKS"
            );
            String clientKey = getProperty(PROP_TLS_CLIENT_KEY);
            applyKeyStore(options, keystoreLocation, keystorePassword, keystoreType, clientKey);
        }
    }

    private void applyTrustStore(RegistryClientOptions options, String location, String password, String type) {
        switch (type.toUpperCase(Locale.ROOT)) {
            case "JKS" -> options.trustStoreJks(location, password);
            case "PKCS12", "P12" -> options.trustStorePkcs12(location, password);
            case "PEM" -> options.trustStorePem(location);
            default -> throw new IllegalArgumentException(
                "Unsupported Apicurio TLS truststore type '" + type + "'. Supported types: JKS, PKCS12, PEM"
            );
        }
    }

    private void applyKeyStore(RegistryClientOptions options, String location, String password, String type, String clientKey) {
        switch (type.toUpperCase(Locale.ROOT)) {
            case "JKS" -> options.keystoreJks(location, password);
            case "PKCS12", "P12" -> options.keystorePkcs12(location, password);
            case "PEM" -> options.keystorePem(location, clientKey);
            default -> throw new IllegalArgumentException(
                "Unsupported Apicurio TLS keystore type '" + type + "'. Supported types: JKS, PKCS12, PEM"
            );
        }
    }

    private void configureProxy(RegistryClientOptions options) {
        String proxyHost = getProperty(PROP_PROXY_HOST);
        String proxyPortStr = getProperty(PROP_PROXY_PORT);
        if (proxyHost != null && !proxyHost.isBlank() && proxyPortStr != null && !proxyPortStr.isBlank()) {
            int proxyPort = Integer.parseInt(proxyPortStr.trim());
            options.proxy(proxyHost.trim(), proxyPort);
            String proxyUser = getProperty(PROP_PROXY_USERNAME);
            String proxyPass = getProperty(PROP_PROXY_PASSWORD);
            if (proxyUser != null && !proxyUser.isBlank() && proxyPass != null && !proxyPass.isBlank()) {
                options.proxyAuth(proxyUser, proxyPass);
            }
        }
    }

    private void configureTimeouts(RegistryClientOptions options) {
        String connectStr = getProperty(PROP_CONNECT_TIMEOUT_MS);
        String readStr = getProperty(PROP_READ_TIMEOUT_MS);
        if (connectStr != null || readStr != null) {
            long connectMs = parseLong(connectStr, 10_000L);
            long readMs = parseLong(readStr, 30_000L);
            options.requestTimeout(connectMs, readMs);
        }
    }

    public String getProperty(String key) {
        return properties.get(key);
    }

    private String getPropertyOrDefault(String key, String defaultValue) {
        String value = properties.get(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static UseId parseUseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return UseId.AUTO;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "auto" -> UseId.AUTO;
            case "contentid", "content_id", "content-id" -> UseId.CONTENT_ID;
            case "globalid", "global_id", "global-id" -> UseId.GLOBAL_ID;
            default -> throw new IllegalArgumentException(
                "Invalid " + PROP_USE_ID + " value '" + raw + "'. Expected one of: auto, contentId, globalId"
            );
        };
    }

    private static IdHandlerMode parseIdHandlerMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return IdHandlerMode.AUTO;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("auto") || normalized.endsWith("optimisticfallbackidhandler")) {
            return IdHandlerMode.AUTO;
        }
        if (normalized.equals("4byte") || normalized.equals("4") || normalized.endsWith("default4byteidhandler")) {
            return IdHandlerMode.FOUR_BYTE;
        }
        if (normalized.equals("8byte") || normalized.equals("8") || normalized.endsWith("legacy8byteidhandler")) {
            return IdHandlerMode.EIGHT_BYTE;
        }
        throw new IllegalArgumentException(
            "Invalid " + PROP_ID_HANDLER + " value '" + raw + "'. Expected one of: auto, 4byte, 8byte"
        );
    }

    private static Set<String> parseArtifactTypes(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_ARTIFACT_TYPES;
        }
        String trimmed = raw.trim();
        if (trimmed.equals("*") || trimmed.equalsIgnoreCase("ALL")) {
            return Collections.emptySet();
        }
        return Arrays.stream(trimmed.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.toUpperCase(Locale.ROOT))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean parseBoolean(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static long parseLong(String raw, long defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(raw.trim());
    }
}

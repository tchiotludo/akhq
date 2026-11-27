package org.akhq.modules.schemaregistry.apicurio;

import io.apicurio.registry.client.common.HttpAdapterType;
import io.apicurio.registry.client.common.RegistryClientOptions;
import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApicurioConfigTest {

    @Test
    void defaultsAreApplied() {
        Connection.SchemaRegistry sr = createSchemaRegistry("http://localhost:8080/apis/registry/v3", null, null, null, null);
        ApicurioConfig config = new ApicurioConfig(sr);

        assertEquals("http://localhost:8080/apis/registry/v3", config.getUrl());
        assertNull(config.getGroupId());
        assertEquals(ApicurioConfig.UseId.AUTO, config.getUseId());
        assertEquals(ApicurioConfig.IdHandlerMode.AUTO, config.getIdHandlerMode());
        assertEquals(4, config.getWriteIdSize());
        assertTrue(config.isProtobufSendTypeRef());
        assertFalse(config.isProtobufSendIndexes());
        assertTrue(config.isArtifactTypeIncluded("AVRO"));
        assertTrue(config.isArtifactTypeIncluded("JSON"));
        assertTrue(config.isArtifactTypeIncluded("PROTOBUF"));
        assertFalse(config.isArtifactTypeIncluded("OPENAPI"));
        assertEquals(ApicurioConfig.DEFAULT_VALUE_GLOBAL_ID_HEADER, config.getValueGlobalIdHeader());

        RegistryClientOptions options = config.buildClientOptions();
        assertEquals(HttpAdapterType.JDK, options.getHttpAdapterType());
        assertEquals(RegistryClientOptions.AuthType.ANONYMOUS, options.getAuthType());
    }

    @Test
    void basicAuthFromTopLevelFields() {
        Connection.SchemaRegistry sr = createSchemaRegistry(
            "http://localhost:8080", "alice", "secret", "com.acme", null
        );
        ApicurioConfig config = new ApicurioConfig(sr);

        assertEquals("com.acme", config.getGroupId());
        RegistryClientOptions options = config.buildClientOptions();
        assertEquals(RegistryClientOptions.AuthType.BASIC, options.getAuthType());
        assertEquals("alice", options.getUsername());
        assertEquals("secret", options.getPassword());
    }

    @Test
    void oauth2AndBearerAuthAndTlsAndProxy() {
        Connection.SchemaRegistry oauthSr = createSchemaRegistry(
            "https://registry.example.com",
            null,
            null,
            null,
            Map.ofEntries(
                Map.entry(ApicurioConfig.PROP_AUTH_TOKEN_ENDPOINT, "https://idp.example.com/token"),
                Map.entry(ApicurioConfig.PROP_AUTH_CLIENT_ID, "akhq"),
                Map.entry(ApicurioConfig.PROP_AUTH_CLIENT_SECRET, "s3cr3t"),
                Map.entry(ApicurioConfig.PROP_AUTH_SCOPE, "registry.read"),
                Map.entry(ApicurioConfig.PROP_TLS_TRUSTSTORE_LOCATION, "/etc/certs/truststore.p12"),
                Map.entry(ApicurioConfig.PROP_TLS_TRUSTSTORE_PASSWORD, "changeit"),
                Map.entry(ApicurioConfig.PROP_TLS_TRUSTSTORE_TYPE, "PKCS12"),
                Map.entry(ApicurioConfig.PROP_TLS_KEYSTORE_LOCATION, "/etc/certs/keystore.jks"),
                Map.entry(ApicurioConfig.PROP_TLS_KEYSTORE_PASSWORD, "keypass"),
                Map.entry(ApicurioConfig.PROP_TLS_KEYSTORE_TYPE, "JKS"),
                Map.entry(ApicurioConfig.PROP_TLS_VERIFY_HOST, "false"),
                Map.entry(ApicurioConfig.PROP_PROXY_HOST, "proxy.internal"),
                Map.entry(ApicurioConfig.PROP_PROXY_PORT, "3128"),
                Map.entry(ApicurioConfig.PROP_PROXY_USERNAME, "puser"),
                Map.entry(ApicurioConfig.PROP_PROXY_PASSWORD, "ppass"),
                Map.entry(ApicurioConfig.PROP_CONNECT_TIMEOUT_MS, "5000"),
                Map.entry(ApicurioConfig.PROP_READ_TIMEOUT_MS, "15000")
            )
        );
        RegistryClientOptions oauthOpts = new ApicurioConfig(oauthSr).buildClientOptions();
        assertEquals(RegistryClientOptions.AuthType.OAUTH2, oauthOpts.getAuthType());
        assertEquals("https://idp.example.com/token", oauthOpts.getTokenEndpoint());
        assertEquals("akhq", oauthOpts.getClientId());
        assertEquals("s3cr3t", oauthOpts.getClientSecret());
        assertEquals("registry.read", oauthOpts.getScope());
        assertEquals(RegistryClientOptions.TrustStoreType.PKCS12, oauthOpts.getTrustStoreType());
        assertEquals(RegistryClientOptions.KeyStoreType.JKS, oauthOpts.getKeyStoreType());
        assertFalse(oauthOpts.isVerifyHost());
        assertEquals("proxy.internal", oauthOpts.getProxyHost());
        assertEquals(3128, oauthOpts.getProxyPort());
        assertEquals("puser", oauthOpts.getProxyUsername());
        assertEquals(5000L, oauthOpts.getConnectTimeoutMs());
        assertEquals(15000L, oauthOpts.getReadIdleTimeoutMs());

        Connection.SchemaRegistry bearerSr = createSchemaRegistry(
            "https://registry.example.com",
            null,
            null,
            null,
            Map.of(
                ApicurioConfig.PROP_AUTH_BEARER_TOKEN, "jwt-token-value",
                ApicurioConfig.PROP_TLS_TRUST_ALL, "true"
            )
        );
        RegistryClientOptions bearerOpts = new ApicurioConfig(bearerSr).buildClientOptions();
        assertEquals(RegistryClientOptions.AuthType.BEARER, bearerOpts.getAuthType());
        assertEquals("jwt-token-value", bearerOpts.getBearerToken());
        assertTrue(bearerOpts.isTrustAll());
    }

    @Test
    void parsesUseIdIdHandlerArtifactTypesAndHeaderOverrides() {
        Connection.SchemaRegistry sr = createSchemaRegistry(
            "http://localhost:8080",
            null,
            null,
            null,
            Map.of(
                ApicurioConfig.PROP_GROUP_ID, "analytics",
                ApicurioConfig.PROP_USE_ID, "globalId",
                ApicurioConfig.PROP_ID_HANDLER, "io.apicurio.registry.serde.Legacy8ByteIdHandler",
                ApicurioConfig.PROP_ARTIFACT_TYPES, "*",
                ApicurioConfig.PROP_SEND_TYPE_REF, "false",
                ApicurioConfig.PROP_SEND_INDEXES, "true",
                "apicurio.registry.headers.value.globalId.name", "x-custom-gid"
            )
        );
        ApicurioConfig config = new ApicurioConfig(sr);
        assertEquals("analytics", config.getGroupId());
        assertEquals(ApicurioConfig.UseId.GLOBAL_ID, config.getUseId());
        assertEquals(ApicurioConfig.IdHandlerMode.EIGHT_BYTE, config.getIdHandlerMode());
        assertEquals(8, config.getWriteIdSize());
        assertFalse(config.isProtobufSendTypeRef());
        assertTrue(config.isProtobufSendIndexes());
        assertTrue(config.isArtifactTypeIncluded("OPENAPI"));
        assertEquals("x-custom-gid", config.getValueGlobalIdHeader());
    }

    @Test
    void rejectsInvalidValues() {
        Connection.SchemaRegistry badUseId = createSchemaRegistry(
            "http://localhost:8080", null, null, null, Map.of(ApicurioConfig.PROP_USE_ID, "bogus")
        );
        assertThrows(IllegalArgumentException.class, () -> new ApicurioConfig(badUseId));

        Connection.SchemaRegistry badHandler = createSchemaRegistry(
            "http://localhost:8080", null, null, null, Map.of(ApicurioConfig.PROP_ID_HANDLER, "16byte")
        );
        assertThrows(IllegalArgumentException.class, () -> new ApicurioConfig(badHandler));

        Connection.SchemaRegistry noUrl = createSchemaRegistry(null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> new ApicurioConfig(noUrl).buildClientOptions());
    }

    static Connection.SchemaRegistry createSchemaRegistry(
        String url,
        String basicAuthUsername,
        String basicAuthPassword,
        String apicurioGroupId,
        Map<String, String> properties
    ) {
        try {
            Connection.SchemaRegistry sr = new Connection.SchemaRegistry();
            setField(sr, "url", url);
            setField(sr, "type", SchemaRegistryType.APICURIO);
            setField(sr, "basicAuthUsername", basicAuthUsername);
            setField(sr, "basicAuthPassword", basicAuthPassword);
            setField(sr, "apicurioGroupId", apicurioGroupId);
            setField(sr, "properties", properties);
            return sr;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

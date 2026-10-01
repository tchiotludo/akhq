package org.akhq.security.claim;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MCP clients authenticate against the identity provider, while the AKHQ groups come from the external REST API
 * claim provider, exactly as for the UI. Only the identity provider is stubbed: the token is really signed and
 * validated against the JWKS endpoint, and the claim provider is called through the real HTTP client.
 */
@MicronautTest(environments = {"mcp", "mcp-rest-api"})
class McpOauthRestApiClaimProviderTest extends AbstractTest {
    private static final String ENVIRONMENT = "mcp-rest-api";
    private static final String ISSUER = "https://idp.mcp-rest-api.test";
    private static final String AUDIENCE = "akhq-mcp";
    private static final String SCOPE = "akhq.mcp.read";
    private static final String AUTHORIZED_USER = "mcp-alice";
    private static final String USER_WITHOUT_GROUP = "mcp-bob";

    private static final RSAKey SIGNING_KEY = generateSigningKey();
    private static final List<ClaimRequest> CLAIM_REQUESTS = new CopyOnWriteArrayList<>();

    private final int port = freePort();

    @NonNull
    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>(super.getProperties());
        properties.put("micronaut.server.port", String.valueOf(port));
        properties.put("akhq.security.mcp-oauth.jwks-url", "http://localhost:" + port + "/mcp-test-idp/jwks");
        return properties;
    }

    @Test
    void externalClaimProviderReceivesTheMcpIdentity() {
        callTool(AUTHORIZED_USER, "akhq.search_topics", Map.of("cluster", KafkaTestCluster.CLUSTER_ID));

        ClaimRequest request = CLAIM_REQUESTS.stream()
            .filter(claimRequest -> AUTHORIZED_USER.equals(claimRequest.getUsername()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("The external claim provider was not called: " + CLAIM_REQUESTS));

        assertEquals(ClaimProviderType.MCP_OAUTH, request.getProviderType());
        assertNull(request.getProviderName());
        assertEquals(List.of("idp-kafka-readers"), request.getGroups());
    }

    @Test
    void externalGroupsRestrictTheVisibleTopics() {
        Map<String, Object> result = result(callTool(
            AUTHORIZED_USER,
            "akhq.search_topics",
            Map.of("cluster", KafkaTestCluster.CLUSTER_ID)
        ));

        String content = text(result);
        assertTrue(content.contains("\"name\":\"" + KafkaTestCluster.TOPIC_RANDOM + "\""), content);
        assertTrue(content.contains("\"totalMatches\":1"), content);
    }

    @Test
    void externalGroupsGrantTopicDataOnAllowedTopicsOnly() {
        Map<String, Object> allowed = result(callTool(
            AUTHORIZED_USER,
            "akhq.get_topic_last_record_timestamp",
            Map.of("cluster", KafkaTestCluster.CLUSTER_ID, "topic", KafkaTestCluster.TOPIC_RANDOM)
        ));
        assertTrue(text(allowed).contains("\"found\":true"), text(allowed));

        Map<String, Object> denied = callTool(
            AUTHORIZED_USER,
            "akhq.get_topic_last_record_timestamp",
            Map.of("cluster", KafkaTestCluster.CLUSTER_ID, "topic", KafkaTestCluster.TOPIC_COMPACTED)
        );
        assertForbidden(denied);
    }

    @Test
    void userWithoutExternalGroupIsDenied() {
        Map<String, Object> response = callTool(
            USER_WITHOUT_GROUP,
            "akhq.search_topics",
            Map.of("cluster", KafkaTestCluster.CLUSTER_ID)
        );

        assertForbidden(response);
        assertTrue(
            CLAIM_REQUESTS.stream().anyMatch(request -> USER_WITHOUT_GROUP.equals(request.getUsername())),
            "The external claim provider was not called: " + CLAIM_REQUESTS
        );
    }

    private Map<String, Object> callTool(String username, String tool, Map<String, Object> arguments) {
        Map<String, Object> payload = Map.of(
            "jsonrpc", "2.0",
            "id", "call-" + tool,
            "method", "tools/call",
            "params", Map.of(
                "name", tool,
                "arguments", Map.of("arguments", arguments)
            )
        );

        HttpRequest<?> request = HttpRequest.POST("/mcp", payload)
            .bearerAuth(accessToken(username))
            .accept(MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_EVENT_STREAM_TYPE);

        try {
            return client.toBlocking().retrieve(request, Map.class);
        } catch (HttpClientResponseException e) {
            return e.getResponse().getBody(Map.class)
                .orElseThrow(() -> new AssertionError("MCP call failed with HTTP " + e.getStatus(), e));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(Map<String, Object> response) {
        assertNull(response.get("error"), String.valueOf(response));
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertNotNull(result, String.valueOf(response));
        assertTrue(!Boolean.TRUE.equals(result.get("isError")), String.valueOf(result));
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void assertForbidden(Map<String, Object> response) {
        assertNull(response.get("error"), String.valueOf(response));
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertNotNull(result, String.valueOf(response));
        assertEquals(true, result.get("isError"), String.valueOf(result));
        assertTrue(text(result).startsWith("Forbidden"), String.valueOf(result));
    }

    @SuppressWarnings("unchecked")
    private static String text(Map<String, Object> result) {
        Object structured = result.get("structuredContent");
        if (structured != null) {
            return String.valueOf(structured);
        }
        List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
        return String.valueOf(content.getFirst().get("text"));
    }

    private static String accessToken(String username) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .subject(username + "-subject")
                .claim("preferred_username", username)
                .claim("groups", List.of("idp-kafka-readers"))
                .claim("scope", "openid " + SCOPE)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
            SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(SIGNING_KEY.getKeyID()).type(JOSEObjectType.JWT).build(),
                claims
            );
            token.sign(new RSASSASigner(SIGNING_KEY));
            return token.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey generateSigningKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("mcp-rest-api-test-key").generate();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Requires(env = ENVIRONMENT)
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Controller("/mcp-test-idp")
    static class IdentityProviderStub {
        @Get(value = "/jwks", produces = MediaType.APPLICATION_JSON)
        Map<String, Object> jwks() {
            return new JWKSet(SIGNING_KEY.toPublicJWK()).toJSONObject();
        }
    }

    @Requires(env = ENVIRONMENT)
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Controller("/mcp-external-claims")
    static class ExternalClaimProviderStub {
        @Post
        Map<String, Object> generateClaim(@Body ClaimRequest request) {
            CLAIM_REQUESTS.add(request);

            if (!AUTHORIZED_USER.equals(request.getUsername())) {
                return Map.of("groups", Map.of());
            }

            return Map.of("groups", Map.of(
                "mcp-random-readers", List.of(Map.of(
                    "role", "topic-read",
                    "patterns", List.of(KafkaTestCluster.TOPIC_RANDOM),
                    "clusters", List.of(KafkaTestCluster.CLUSTER_ID)
                ))
            ));
        }
    }
}

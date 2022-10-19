package org.akhq.middlewares;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomHttpResponseHeadersFilterTest {

    private static EmbeddedServer server;
    private static HttpClient client;

    protected static final String REQUIRED_HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    protected static final String REQUIRED_HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES = "X-Permitted-Cross-Domain-Policies";
    protected static final String REQUIRED_HEADER_CROSS_ORIGIN_EMBEDDER_POLICY = "Cross-Origin-Embedder-Policy";
    protected static final String REQUIRED_HEADER_CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy";
    protected static final String REQUIRED_HEADER_CROSS_ORIGIN_RESOURCE_POLICY = "Cross-Origin-Resource-Policy";
    protected static final String REQUIRED_HEADER_FEATURE_POLICY = "Feature-Policy";
    protected static final String REQUIRED_HEADER_PERMISSIONS_POLICY = "Permissions-Policy";
    protected static final String REQUIRED_HEADER_CONTENT_SECURITY_POLICY_VALUE = "default-src 'none'; frame-src 'self'; script-src 'self'; connect-src 'self'; img-src 'self'; style-src 'self'; frame-ancestors 'self'; form-action 'self'; upgrade-insecure-requests";
    protected static final String REQUIRED_HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES_VALUE = "none";
    protected static final String REQUIRED_HEADER_CROSS_ORIGIN_EMBEDDER_POLICY_VALUE = "require-corp";
    protected static final String REQUIRED_HEADER_CROSS_ORIGIN_OPENER_POLICY_VALUE = "same-origin";
    protected static final String REQUIRED_HEADER_CROSS_ORIGIN_RESOURCE_POLICY_VALUE = "same-origin";
    protected static final String REQUIRED_HEADER_FEATURE_POLICY_VALUE = "microphone 'none'; geolocation 'none'; usb 'none'; payment 'none'; document-domain 'none'; camera 'none'; display-capture 'none'; ambient-light-sensor 'none'";
    protected static final String REQUIRED_HEADER_PERMISSIONS_POLICY_VALUE = "microphone=(), geolocation=(), usb=(), payment=(), document-domain=(), camera=(), display-capture=(), ambient-light-sensor=()";
    protected static final String FORBIDDEN_HEADER_X_POWERED_BY = "X-Powered-By";
    protected static final String FORBIDDEN_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    protected static final String FORBIDDEN_HEADER_VIA = "Via";
    protected static final String FORBIDDEN_HEADER_SERVER = "Server";

    @BeforeAll
    public static void setupServer() {
        server = ApplicationContext.run(EmbeddedServer.class, "custom-http-response-headers");
        client = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
    }

    @AfterAll
    public static void stopServer() {
        if (client != null) {
            client.stop();
        }
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Test the {@link CustomHttpResponseHeadersFilter} for a valid URI. Check the response for existence of the custom headers.
     */
    @Test
    @Order(1)
    void testFilterCheckHeaders() {
        HttpResponse<?> response = client.toBlocking().exchange("/issues/66");
        assertEquals(HttpStatus.OK.getCode(), response.getStatus().getCode());
        assertHeaders(response);
    }

    /**
     * Test the {@link CustomHttpResponseHeadersFilter} for an invalid URI. But the response must nevertheless be added.
     */
    @Test
    @Order(2)
    void testFilterCheckHeadersForInvalidURI() {
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> client.toBlocking().exchange("/issues/"));
        assertEquals(HttpStatus.NOT_FOUND.getCode(), exception.getStatus().getCode());
        assertHeaders(exception.getResponse());
    }

    private static void assertHeaders(HttpResponse<?> response) {

        Assertions.assertEquals(REQUIRED_HEADER_CONTENT_SECURITY_POLICY_VALUE, response.getHeaders().get(REQUIRED_HEADER_CONTENT_SECURITY_POLICY),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_CONTENT_SECURITY_POLICY) + " for " + REQUIRED_HEADER_CONTENT_SECURITY_POLICY);

        Assertions.assertEquals(REQUIRED_HEADER_FEATURE_POLICY_VALUE, response.getHeaders().get(REQUIRED_HEADER_FEATURE_POLICY),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_FEATURE_POLICY) + " for " + REQUIRED_HEADER_FEATURE_POLICY);

        Assertions.assertEquals(REQUIRED_HEADER_CROSS_ORIGIN_OPENER_POLICY_VALUE, response.getHeaders().get(REQUIRED_HEADER_CROSS_ORIGIN_OPENER_POLICY),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_CROSS_ORIGIN_OPENER_POLICY) + " for " + REQUIRED_HEADER_CROSS_ORIGIN_OPENER_POLICY);

        Assertions.assertEquals(REQUIRED_HEADER_CROSS_ORIGIN_EMBEDDER_POLICY_VALUE, response.getHeaders().get(REQUIRED_HEADER_CROSS_ORIGIN_EMBEDDER_POLICY),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_CROSS_ORIGIN_EMBEDDER_POLICY) + " for " + REQUIRED_HEADER_CROSS_ORIGIN_EMBEDDER_POLICY);

        Assertions.assertEquals(REQUIRED_HEADER_CROSS_ORIGIN_RESOURCE_POLICY_VALUE, response.getHeaders().get(REQUIRED_HEADER_CROSS_ORIGIN_RESOURCE_POLICY),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_CROSS_ORIGIN_RESOURCE_POLICY) + " for " + REQUIRED_HEADER_CROSS_ORIGIN_RESOURCE_POLICY);

        Assertions.assertEquals(REQUIRED_HEADER_PERMISSIONS_POLICY_VALUE, response.getHeaders().get(REQUIRED_HEADER_PERMISSIONS_POLICY),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_PERMISSIONS_POLICY) + " for " + REQUIRED_HEADER_PERMISSIONS_POLICY);

        Assertions.assertEquals(REQUIRED_HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES_VALUE, response.getHeaders().get(REQUIRED_HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES),
                "Wrong value " + response.getHeaders().get(REQUIRED_HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES) + " for " + REQUIRED_HEADER_X_PERMITTED_CROSS_DOMAIN_POLICIES);

        assertFalse(response.getHeaders().contains(FORBIDDEN_HEADER_SERVER), FORBIDDEN_HEADER_SERVER + " erroneously exists as header");
        assertFalse(response.getHeaders().contains(FORBIDDEN_HEADER_VIA), FORBIDDEN_HEADER_VIA + " erroneously exists as header");
        assertFalse(response.getHeaders().contains(FORBIDDEN_HEADER_X_POWERED_BY), FORBIDDEN_HEADER_X_POWERED_BY + " erroneously exists as header");
        assertFalse(response.getHeaders().contains(FORBIDDEN_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN), FORBIDDEN_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN + " erroneously exists as header");
    }

}
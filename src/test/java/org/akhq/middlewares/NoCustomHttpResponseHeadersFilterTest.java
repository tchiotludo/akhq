package org.akhq.middlewares;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NoCustomHttpResponseHeadersFilterTest {

    private static EmbeddedServer server;
    private static HttpClient client;

    @BeforeAll
    public static void setupServer() {
        server = ApplicationContext.run(EmbeddedServer.class, "no-custom-http-response-headers");
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
    void testFilterCheckNoHeaders() {
        HttpResponse<?> response = client.toBlocking().exchange("/issues/66");
        assertEquals(HttpStatus.OK.getCode(), response.getStatus().getCode());
    }
}


package org.akhq.clients.connect;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.ssl.SslConfiguration;
import io.micronaut.http.uri.UriBuilder;
import lombok.extern.slf4j.Slf4j;
import org.akhq.clients.connect.dto.*;
import org.akhq.clients.connect.error.ConnectBadRequestException;
import org.akhq.clients.connect.error.ConnectConflictException;
import org.akhq.clients.connect.error.ConnectNotFoundException;
import org.akhq.clients.connect.error.ConnectRestException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Thin HTTP client for the Kafka Connect REST API.
 */
@Slf4j
public class KafkaConnectApiClient {

    private final BlockingHttpClient httpClient;
    private final URI baseUri;
    private final String basicAuthUser;
    private final String basicAuthPassword;

    private KafkaConnectApiClient(BlockingHttpClient httpClient, URI baseUri, String basicAuthUser, String basicAuthPassword) {
        this.httpClient = httpClient;
        this.baseUri = baseUri;
        this.basicAuthUser = basicAuthUser;
        this.basicAuthPassword = basicAuthPassword;
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder(String baseUrl) {
        return new Builder(baseUrl);
    }

    public static class Builder {
        private final String baseUrl;
        private String basicAuthUsername;
        private String basicAuthPassword;
        private String trustStorePath;
        private String trustStorePassword;
        private String keyStorePath;
        private String keyStorePassword;

        private Builder(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Builder basicAuth(String username, String password) {
            this.basicAuthUsername = username;
            this.basicAuthPassword = password;
            return this;
        }

        public Builder trustStore(String path, String password) {
            this.trustStorePath = path;
            this.trustStorePassword = password;
            return this;
        }

        public Builder keyStore(String path, String password) {
            this.keyStorePath = path;
            this.keyStorePassword = password;
            return this;
        }

        public KafkaConnectApiClient build() {
            try {
                DefaultHttpClientConfiguration config = new DefaultHttpClientConfiguration();

                if (trustStorePath != null || keyStorePath != null) {
                    SslConfiguration ssl = config.getSslConfiguration();
                    ssl.setEnabled(true);

                    if (trustStorePath != null) {
                        SslConfiguration.TrustStoreConfiguration ts = new SslConfiguration.TrustStoreConfiguration();
                        ts.setPath(trustStorePath);
                        ts.setPassword(trustStorePassword);
                        ssl.setTrustStore(ts);
                    }

                    if (keyStorePath != null) {
                        SslConfiguration.KeyStoreConfiguration ks = new SslConfiguration.KeyStoreConfiguration();
                        ks.setPath(keyStorePath);
                        ks.setPassword(keyStorePassword);
                        ssl.setKeyStore(ks);
                    }
                }

                URI uri = new URI(baseUrl);
                BlockingHttpClient client = HttpClient.create(uri.toURL(), config).toBlocking();

                return new KafkaConnectApiClient(client, new URI(baseUrl), basicAuthUsername, basicAuthPassword);
            } catch (MalformedURLException | URISyntaxException e) {
                throw new IllegalArgumentException("Invalid Kafka Connect URL: " + baseUrl, e);
            }
        }
    }

    private URI uri(String path) {
        return UriBuilder.of(baseUri).path(path).build();
    }

    // -------------------------------------------------------------------------
    // API methods
    // -------------------------------------------------------------------------

    /**
     * GET /connectors/{name}
     */
    public ConnectorInfo getConnector(String name) {
        return call(HttpRequest.GET(uri("/connectors/" + encode(name))), Argument.of(ConnectorInfo.class));
    }

    /**
     * GET /connectors/{name}/status
     */
    public ConnectorStatus getConnectorStatus(String name) {
        return call(HttpRequest.GET(uri("/connectors/" + encode(name) + "/status")), Argument.of(ConnectorStatus.class));
    }

    /**
     * GET /connectors?expand=info&expand=status
     * Returns a map of connector name -> expanded info+status.
     */
    public Map<String, ConnectorExpanded> getConnectorsExpanded() {
        return call(HttpRequest.GET(uri("/connectors?expand=info&expand=status")),
            Argument.mapOf(String.class, ConnectorExpanded.class));
    }

    /**
     * GET /connector-plugins
     */
    public List<ConnectorPluginInfo> getConnectorPlugins() {
        return call(HttpRequest.GET(uri("/connector-plugins")), Argument.listOf(ConnectorPluginInfo.class));
    }

    /**
     * PUT /connector-plugins/{pluginName}/config/validate
     */
    public ConnectorPluginValidation validateConnectorPluginConfig(String pluginName, Map<String, String> config) {
        return call(HttpRequest.PUT(uri("/connector-plugins/" + encode(pluginName) + "/config/validate"), config),
            Argument.of(ConnectorPluginValidation.class));
    }

    /**
     * POST /connectors
     */
    public ConnectorInfo createConnector(String name, Map<String, String> config) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("config", config);
        return call(HttpRequest.POST(uri("/connectors"), body), Argument.of(ConnectorInfo.class));
    }

    /**
     * PUT /connectors/{name}/config
     */
    public ConnectorInfo updateConnectorConfig(String name, Map<String, String> config) {
        return call(HttpRequest.PUT(uri("/connectors/" + encode(name) + "/config"), config), Argument.of(ConnectorInfo.class));
    }

    /**
     * DELETE /connectors/{name}
     */
    public void deleteConnector(String name) {
        callVoid(HttpRequest.DELETE(uri("/connectors/" + encode(name))));
    }

    /**
     * PUT /connectors/{name}/pause
     */
    public void pauseConnector(String name) {
        callVoid(HttpRequest.PUT(uri("/connectors/" + encode(name) + "/pause"), null));
    }

    /**
     * PUT /connectors/{name}/resume
     */
    public void resumeConnector(String name) {
        callVoid(HttpRequest.PUT(uri("/connectors/" + encode(name) + "/resume"), null));
    }

    /**
     * POST /connectors/{name}/restart
     */
    public void restartConnector(String name) {
        callVoid(HttpRequest.POST(uri("/connectors/" + encode(name) + "/restart"), null));
    }

    /**
     * POST /connectors/{name}/tasks/{taskId}/restart
     */
    public void restartConnectorTask(String name, int taskId) {
        callVoid(HttpRequest.POST(uri("/connectors/" + encode(name) + "/tasks/" + taskId + "/restart"), null));
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------
    private <B, T> T call(MutableHttpRequest<B> request, Argument<T> type) {
        try {
            log.debug("Kafka Connect REST: {} {}", request.getMethod(), request.getUri());
            return httpClient.retrieve(withAuth(request), type);
        } catch (HttpClientResponseException e) {
            throw mapException(e);
        }
    }

    private <B> void callVoid(MutableHttpRequest<B> request) {
        try {
            log.debug("Kafka Connect REST: {} {}", request.getMethod(), request.getUri());
            httpClient.exchange(withAuth(request));
        } catch (HttpClientResponseException e) {
            throw mapException(e);
        }
    }

    private <B> MutableHttpRequest<B> withAuth(MutableHttpRequest<B> request) {
        if (basicAuthUser != null) {
            return request.basicAuth(basicAuthUser, basicAuthPassword);
        }
        return request;
    }

    private RuntimeException mapException(HttpClientResponseException e) {
        String body = e.getResponse().getBody(String.class).orElse(e.getMessage());
        return switch (e.getStatus().getCode()) {
            case 400 -> new ConnectBadRequestException(body);
            case 404 -> new ConnectNotFoundException(body);
            case 409 -> new ConnectConflictException(body);
            default -> new ConnectRestException(e.getStatus().getCode(),
                "Kafka Connect returned HTTP " + e.getStatus().getCode() + ": " + body, e);
        };
    }

    private static String encode(String segment) {
        return java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8)
            .replace("+", "%20");
    }
}


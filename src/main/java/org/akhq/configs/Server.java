package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for AKHQ server configuration (mainly for {@code akhq.server.custom-http-response-headers}).
 *
 * This does NOT include settings for {@link org.akhq.middlewares.HttpServerAccessLogFilter} like
 * {@code akhq.server.access-log.enabled} or {@code akhq.server.access-log.format} which are directly referenced in that class.
 */
@ConfigurationProperties("akhq.server")
public class Server {

    private static final String HEADER_KEY = "name";
    private static final String HEADER_VALUE = "value";
    private final Map<String,String> customHttpResponseHeaders = new HashMap<>();

    public Map<String, String> getCustomHttpResponseHeaders() {
        return customHttpResponseHeaders;
    }

    /**
     * Convert the property entries to a {@code Map} as they are read from configuration as an {@link ArrayList}.
     *
     * @param customHttpResponseHeaders the list of maps from application configuration definied by property
     * {@code akhq.server.custom-http-response-headers}.
     */
    public void setCustomHttpResponseHeaders(List<Map<String, String>> customHttpResponseHeaders) {
        customHttpResponseHeaders.forEach(header ->
                this.customHttpResponseHeaders.put(header.get(HEADER_KEY), header.get(HEADER_VALUE))
        );
    }
}

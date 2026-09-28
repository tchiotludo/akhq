package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties("akhq.mcp")
public class Mcp {
    private boolean enabled;

    /**
     * Browser origins allowed to call the MCP endpoint. Requests without an {@code Origin} header, sent by non
     * browser MCP clients, are always allowed.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Maximum duration of a topic search, after which the matches found so far are returned.
     */
    private Duration searchTimeout = Duration.ofSeconds(30);
}

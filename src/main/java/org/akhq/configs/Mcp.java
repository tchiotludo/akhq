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
    /**
     * Budget, in characters, of the message values or projected fields returned by a single topic search. Values are
     * returned in full when they fit and are truncated evenly otherwise, to keep tool results within the context of
     * an LLM.
     */
    private int maxResultLength = 100_000;
}

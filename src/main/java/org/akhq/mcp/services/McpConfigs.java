package org.akhq.mcp.services;

import org.akhq.mcp.model.GetConfigsResult;
import org.akhq.models.Config;

import java.util.List;
import java.util.Locale;

/**
 * Builds the bounded config listing shared by the topic and broker config tools.
 */
final class McpConfigs {
    private McpConfigs() {
    }

    static GetConfigsResult result(
        String cluster,
        String resourceType,
        String resource,
        List<Config> configs,
        String search,
        Boolean onlyOverridden,
        Integer maxResults
    ) {
        int limit = McpLimits.maxItems(maxResults);
        List<String> terms = McpLimits.search(search)
            .map(value -> List.of(value.toLowerCase(Locale.ROOT).split("\\s+")))
            .orElse(List.of());
        boolean overriddenOnly = Boolean.TRUE.equals(onlyOverridden);

        List<Config> matches = (configs == null ? List.<Config>of() : configs).stream()
            .filter(config -> terms.stream().allMatch(term -> config.getName().toLowerCase(Locale.ROOT).contains(term)))
            .filter(config -> !overriddenOnly || (config.getSource() != null
                && config.getSource() != Config.Source.DEFAULT_CONFIG
                && config.getSource() != Config.Source.STATIC_BROKER_CONFIG))
            .toList();
        List<GetConfigsResult.ConfigEntry> returned = matches.stream()
            .limit(limit)
            .map(config -> new GetConfigsResult.ConfigEntry(
                config.getName(),
                config.isSensitive() ? null : config.getValue(),
                config.getSource() == null ? null : config.getSource().name(),
                config.isReadOnly(),
                config.isSensitive()
            ))
            .toList();
        boolean truncated = matches.size() > returned.size();

        return new GetConfigsResult(
            cluster, resourceType, resource, matches.size(), truncated, returned,
            McpLimits.listMessage("config", matches.size(), returned.size(), truncated)
                + " Values of sensitive configs are never returned."
        );
    }
}

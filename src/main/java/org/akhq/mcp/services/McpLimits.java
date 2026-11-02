package org.akhq.mcp.services;

/**
 * Bounds of the list and search tools, so a response always fits in a model context.
 */
final class McpLimits {
    static final int DEFAULT_MAX_RESULTS = 50;
    static final int MAX_ALLOWED_RESULTS = 200;
    static final int DEFAULT_MAX_ITEMS = 100;
    static final int MAX_ALLOWED_ITEMS = 500;
    static final int MAX_TEXT_LENGTH = 100_000;
    static final int MAX_TRACE_LENGTH = 2_000;

    private McpLimits() {
    }

    static int maxResults(Integer requested) {
        return bound(requested, DEFAULT_MAX_RESULTS, MAX_ALLOWED_RESULTS);
    }

    static int maxItems(Integer requested) {
        return bound(requested, DEFAULT_MAX_ITEMS, MAX_ALLOWED_ITEMS);
    }

    static int bound(Integer requested, int defaultValue, int maxValue) {
        if (requested == null) {
            return defaultValue;
        }
        if (requested < 1) {
            throw new IllegalArgumentException("`arguments.maxResults` must be greater than 0");
        }

        return Math.min(requested, maxValue);
    }

    static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    static java.util.Optional<String> search(String search) {
        return java.util.Optional.ofNullable(search)
            .map(String::trim)
            .filter(value -> !value.isEmpty());
    }

    static String listMessage(String what, int total, int returned, boolean truncated) {
        if (total == 0) {
            return "No " + what + " found. There may be no match, or you may not be allowed to see it.";
        }
        if (truncated) {
            return "Found " + total + " " + what + "(s), returning the first " + returned
                + ". Refine `search` or raise `maxResults` to see the others.";
        }

        return "Found " + total + " " + what + "(s).";
    }
}

package org.akhq.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Versioned, structured message-search expression made of groups of conditions.
 * The first group and the first condition of a group have no join; every later one is joined to the previous one.
 */
public record SearchExpression(Integer version, List<Group> groups) {
    public static final int CURRENT_VERSION = 1;

    public enum Join {
        AND,
        OR
    }

    public enum Field {
        @JsonProperty("value") VALUE,
        @JsonProperty("key") KEY,
        @JsonProperty("headerKey") HEADER_KEY,
        @JsonProperty("headerValue") HEADER_VALUE,
        @JsonProperty("keySubject") KEY_SUBJECT,
        @JsonProperty("valueSubject") VALUE_SUBJECT
    }

    public enum Operator {
        @JsonProperty("contains") CONTAINS,
        @JsonProperty("notContains") NOT_CONTAINS,
        @JsonProperty("equals") EQUALS,
        @JsonProperty("matchesRegex") MATCHES_REGEX
    }

    public record Group(Join join, List<Condition> conditions) {
    }

    public record Condition(Join join, Field field, Operator operator, String value) {
    }
}

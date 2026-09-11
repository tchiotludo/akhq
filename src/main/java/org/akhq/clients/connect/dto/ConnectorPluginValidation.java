package org.akhq.clients.connect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

/**
 * Represents the response from PUT /connector-plugins/{plugin}/config/validate
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConnectorPluginValidation {
    private String name;
    @JsonProperty("error_count")
    private int errorCount;
    private List<Config> configs;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Config {
        private Definition definition;
        private Value value;

        @Data
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Definition {
            private String name;
            private String type;
            private boolean required;
            @JsonProperty("default_value")
            private String defaultValue;
            private String importance;
            private String documentation;
            private String group;
            private String width;
            @JsonProperty("display_name")
            private String displayName;
            private Collection<String> dependents;
            private int order;
        }

        @Data
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Value {
            private String name;
            private String value;
            @JsonProperty("recommended_values")
            private List<String> recommendedValues;
            private List<String> errors;
            private boolean visible;
        }
    }
}


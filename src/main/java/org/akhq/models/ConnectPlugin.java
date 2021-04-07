package org.akhq.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPlugin;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class ConnectPlugin {
    private String className;
    private String type;
    private String version;
    private List<Definition> definitions;

    public ConnectPlugin(ConnectorPlugin connectorPlugin, ConnectorPluginConfigValidationResults results) {
        this.className = connectorPlugin.getClassName();
        this.type = connectorPlugin.getType();
        this.version = connectorPlugin.getVersion();
        this.definitions = Stream.concat(
            results.getConfigs()
                .stream()
                .map(config -> new Definition(config.getDefinition())),
            registryDefinition()
        )
            .sorted(Comparator.comparing(Definition::getGroup, (s1, s2) -> s1.equals("Others") ? 1 : s1.compareTo(s2))
                .thenComparing(Definition::getOrder)
            )
            .collect(Collectors.toList());
    }

    public Stream<Definition> registryDefinition() {
        return Stream.of(
            Definition.builder()
                .name("schema.registry.url")
                .group("Schema Registry")
                .displayName("Schema registry Url")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("schema.registry.basic.auth.user.info")
                .group("Schema Registry")
                .displayName("Schema registry basic auth as user:password")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("basic.auth.credentials.source")
                .group("Schema Registry")
                .displayName("Schema registry basic auth source")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("key.converter.schemas.enable")
                .group("Schema Registry")
                .displayName("Enable key schemas")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("key.converter.schema.registry.url")
                .group("Schema Registry")
                .displayName("Key schema registry Url")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("key.converter.schema.registry.basic.auth.user.info")
                .group("Schema Registry")
                .displayName("Key schema registry basic auth as user:password")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("key.converter.basic.auth.credentials.source")
                .group("Schema Registry")
                .displayName("Key schema registry basic auth source")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("value.converter.schemas.enable")
                .group("Schema Registry")
                .displayName("Enable value schemas")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("value.converter.schema.registry.url")
                .group("Schema Registry")
                .displayName("Value schema registry Url")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("value.converter.schema.registry.basic.auth.user.info")
                .group("Schema Registry")
                .displayName("Value schema registry basic auth as user:password")
                .importance("MEDIUM")
                .build(),
            Definition.builder()
                .name("value.converter.basic.auth.credentials.source")
                .group("Schema Registry")
                .displayName("Value schema registry basic auth source")
                .importance("MEDIUM")
                .build()
        );
    }

    public String getShortClassName() {
        String[] split = className.split("\\.");

        return split[split.length - 1];
    }

    @Builder
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    @Getter
    @NoArgsConstructor
    public static final class Definition {
        private String name;
        private String type;
        private boolean required;
        private String defaultValue;
        private String importance;
        private String documentation;
        private String group;
        private String width;
        private String displayName;
        private Collection<String> dependents;
        private int order;

        public Definition(ConnectorPluginConfigValidationResults.Config.Definition definition) {
            String displayCompare = null;

            if (definition.getDisplayName() != null) {
                displayCompare = definition.getDisplayName().toLowerCase().replaceAll("\\.", " ");
            }

            this.name = definition.getName();
            this.type = definition.getType();
            this.required = definition.isRequired();
            this.defaultValue = definition.getDefaultValue();
            this.importance = definition.getImportance();
            this.documentation = definition.getDocumentation();
            this.group = definition.getGroup() == null ? "Others" : definition.getGroup();
            this.width = definition.getWidth();
            this.displayName = displayCompare != null && displayCompare.equals(definition.getName().toLowerCase()) ? null : definition.getDisplayName();
            this.dependents = definition.getDependents();
            this.order = definition.getOrder();
        }

        @JsonIgnore
        public String getDependentsAsString() {
            return String.join(", ", this.getDependents());
        }
    }
}

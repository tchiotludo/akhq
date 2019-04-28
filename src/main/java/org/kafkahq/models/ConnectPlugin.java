package org.kafkahq.models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPlugin;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorPluginConfigValidationResults;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@EqualsAndHashCode
@Getter
public class ConnectPlugin {
    private final String className;
    private final String type;
    private final String version;
    private final List<Definition> definitions;

    public ConnectPlugin(ConnectorPlugin connectorPlugin, ConnectorPluginConfigValidationResults results) {
        this.className = connectorPlugin.getClassName();
        this.type = connectorPlugin.getType();
        this.version = connectorPlugin.getVersion();
        this.definitions = results.getConfigs()
            .stream()
            .map(config -> new Definition(config.getDefinition()))
            .sorted(Comparator.comparing(Definition::getGroup, (s1, s2) -> s1.equals("Others") ? 1 : s1.compareTo(s2))
                .thenComparing(Definition::getOrder)
            )
            .collect(Collectors.toList());
    }

    public String getShortClassName() {
        String[] split = className.split("\\.");

        return split[split.length - 1];
    }

    @ToString
    @EqualsAndHashCode
    @Getter
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

        public String getDependentsAsString() {
            return String.join(", ", this.getDependents());
        }
    }
}

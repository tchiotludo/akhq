package org.akhq.models;

import lombok.*;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.config.internals.BrokerSecurityConfigs;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ToString
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Config {
    private String name;
    @With
    private  String value;
    private String description;
    private Source source;
    private boolean sensitive;
    private boolean readOnly;
    private final List<Synonym> synonyms = new ArrayList<>();

    public Config(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Config(ConfigEntry entry) {
        this.name = entry.name();
        this.value = entry.value();
        this.description = findDescription(this.name);
        this.source = Source.valueOf(entry.source().name());
        this.sensitive = entry.isSensitive();
        this.readOnly = entry.isReadOnly();

        for (ConfigEntry.ConfigSynonym item: entry.synonyms()) {
            this.synonyms.add(new Synonym(item));
        }
    }

    private String findDescription(String name) {
        String docName = name.toUpperCase().replace(".", "_") + "_DOC";

        List<Class<?>> classes = Arrays.asList(
            TopicConfig.class,
            BrokerSecurityConfigs.class,
            SslConfigs.class,
            SaslConfigs.class
        );

        for(Class<?> cls : classes) {
            try {
                Field declaredField = cls.getDeclaredField(docName);
                return declaredField.get(cls.getDeclaredConstructor().newInstance()).toString();
            } catch (NoSuchFieldException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) { }
        }

        return null;
    }

    @ToString
    @EqualsAndHashCode
    @Getter
    public static class Synonym {
        private final String name;
        private final String value;
        private final Source source;

        public Synonym(ConfigEntry.ConfigSynonym synonym) {
            this.name = synonym.name();
            this.value = synonym.value();
            this.source = Source.valueOf(synonym.source().name());
        }
    }

    public enum Source {
        DYNAMIC_TOPIC_CONFIG,
        DYNAMIC_BROKER_CONFIG,
        DYNAMIC_DEFAULT_BROKER_CONFIG,
        STATIC_BROKER_CONFIG,
        DEFAULT_CONFIG,
        UNKNOWN
    }
}

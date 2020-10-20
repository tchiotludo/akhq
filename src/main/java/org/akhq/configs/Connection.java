package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EachProperty("akhq.connections")
@Getter
public class Connection extends AbstractProperties {
    SchemaRegistry schemaRegistry;
    List<Connect> connect;
    ProtobufDeserializationTopicsMapping deserialization;

    public Connection(@Parameter String name) {
        super(name);
    }

    @Getter
    @ConfigurationProperties("schema-registry")
    public static class SchemaRegistry {
        String url;
        String basicAuthUsername;
        String basicAuthPassword;

        @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
        Map<String, String> properties;
    }

    @Getter
    @Data
    @ConfigurationProperties("deserialization.protobuf")
    public static class ProtobufDeserializationTopicsMapping {
        List<TopicsMapping> topicsMapping = new ArrayList<>();
    }
}


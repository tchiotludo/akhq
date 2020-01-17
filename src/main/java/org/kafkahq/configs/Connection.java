package org.kafkahq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Getter;

import java.net.URL;
import java.util.List;
import java.util.Map;

@EachProperty("kafkahq.connections")
@Getter
public class Connection extends AbstractProperties {
    SchemaRegistry schemaRegistry;
    List<Connect> connect;

    public Connection(@Parameter String name) {
        super(name);
    }

    @Getter
    @ConfigurationProperties("schema-registry")
    public static class SchemaRegistry {
        URL url;
        String basicAuthUsername;
        String basicAuthPassword;

        @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
        Map<String, String> properties;
    }
}


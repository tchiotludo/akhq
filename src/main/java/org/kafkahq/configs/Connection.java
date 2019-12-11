package org.kafkahq.configs;

import io.micronaut.context.annotation.*;
import lombok.Getter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
    }


}


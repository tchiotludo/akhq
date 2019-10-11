package org.kafkahq.configs;

import io.micronaut.context.annotation.*;
import lombok.Getter;

import java.net.URL;

@EachProperty("kafkahq.connections")
@Getter
public class Connection extends AbstractProperties {
    SchemaRegistry schemaRegistry;
    Connect connect;

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

    @Getter
    @ConfigurationProperties("connect")
    public static class Connect {
        URL url;
        String basicAuthUsername;
        String basicAuthPassword;
        String sslTrustStore;
        String sslTrustStorePassword;
        String sslKeyStore;
        String sslKeyStorePassword;
    }
}


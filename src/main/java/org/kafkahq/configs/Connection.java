package org.kafkahq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
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
        BasicAuth basicAuth;

        @Getter
        @ConfigurationProperties("basic-auth")
        public static class BasicAuth {
            String username;
            String password;
        }
    }

    @Getter
    @ConfigurationProperties("connect")
    public static class Connect {
        URL url;
        BasicAuth basicAuth;
        Ssl ssl;

        @Getter
        @ConfigurationProperties("basic-auth")
        public static class BasicAuth {
            String username;
            String password;
        }

        @Getter
        @ConfigurationProperties("ssl")
        public static class Ssl {
            String trustStore;
            String trustStorePassword;
            String keyStore;
            String keyStorePassword;
        }
    }
}


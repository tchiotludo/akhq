package org.kafkahq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.net.URL;
import java.util.Optional;

@EachProperty("kafkahq.connections")
@Getter
public class Connection extends AbstractProperties {
    Optional<URL> schemaRegistry = Optional.empty();
    Optional<URL> connect = Optional.empty();

    public Connection(@Parameter  String name) {
        super(name);
    }
}


package org.kafkahq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.net.URL;
import java.util.Optional;

@EachProperty("kafka.connections")
@Getter
public class Connection extends AbstractProperties {
    Optional<URL> registry = Optional.empty();

    public Connection(@Parameter  String name) {
        super(name);
    }
}


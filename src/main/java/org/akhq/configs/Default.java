package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

@EachProperty("akhq.clients-defaults")
@Getter
public class Default extends AbstractProperties  {
    public Default(@Parameter  String name) {
        super(name);
    }
}


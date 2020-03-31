package org.akhq.configs;

import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Getter;

import java.util.Map;

@Getter
abstract public class AbstractProperties {
    private final String name;

    @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
    Map<String, String> properties;

    public AbstractProperties(@Parameter String name) {
        this.name = name;
    }
}


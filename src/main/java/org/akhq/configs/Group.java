package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@EachProperty("akhq.security.groups")
@Getter
public class Group {

    String name;
    List<String> roles;
    Map<String, Object> attributes;

    public Group(@Parameter String name) {
        this.name = name;
    }
}

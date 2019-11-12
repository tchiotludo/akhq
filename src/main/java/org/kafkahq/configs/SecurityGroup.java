package org.kafkahq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.util.Set;

@EachProperty("kafkahq.security.groups")
@Getter
public class SecurityGroup {

    String name;
    String ldapGroup;
    Set<String> roles;

    public SecurityGroup(@Parameter String name) {
        this.name = name;
    }
}

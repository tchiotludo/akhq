package org.kafkahq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.util.List;

@EachProperty("kafkahq.security.ldap.group")
@Getter
public class LdapGroup {
    String name;
    List<String> groups;

    public LdapGroup(@Parameter String name) {
        this.name = name;
    }
}

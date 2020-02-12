package org.kafkahq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import lombok.Getter;

import java.util.List;

@EachProperty("kafkahq.security.ldap.user")
@Getter
public class LdapUser {
    String username;
    List<String> groups;

    public LdapUser(@Parameter String username) {
	this.username = username;
    }
}

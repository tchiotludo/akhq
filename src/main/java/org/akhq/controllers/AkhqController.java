package org.akhq.controllers;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.akhq.configs.*;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;


@Controller("${akhq.server.base-path:}/")
public class AkhqController extends AbstractController {
    @Inject
    private List<Connection> connections;

    @Inject
    private ApplicationContext applicationContext;
    @Inject
    private List<LdapGroup> ldapAuths;

    @Inject
    private List<LdapUser> ldapUsers;

    @Inject
    private List<BasicAuth> basicAuths;

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("api/cluster")
    public List<ClusterDefinition> list() {
        return this.connections
            .stream()
            .map(connection -> new ClusterDefinition(
                connection.getName(),
                connection.getSchemaRegistry() != null,
                connection
                    .getConnect()
                    .stream()
                    .map(Connect::getName)
                    .collect(Collectors.toList())

            ))
            .collect(Collectors.toList());
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("api/auths")
    public AuthDefinition auths() {
        AuthDefinition authDefinition = new AuthDefinition();

        if (applicationContext.containsBean(SecurityService.class)) {
            authDefinition.loginEnabled = basicAuths.size() > 0 || ldapAuths.size() > 0 || ldapUsers.size() > 0;
        } else {
            authDefinition.loginEnabled = false;
        }

        return authDefinition;
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("api/me")
    public AuthUser users() {
        AuthUser authUser = new AuthUser();

        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);

            securityService
                .getAuthentication()
                .ifPresent(authentication -> {
                    authUser.logged = true;
                    authUser.username = authentication.getName();
                    authUser.roles = this.getRights();
                });
        }

        return authUser;
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class AuthDefinition {
        private boolean loginEnabled;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class AuthUser {
        private boolean logged = false;
        private String username;
        private List<String> roles;
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class ClusterDefinition {
        private String id;
        private boolean registry;
        private List<String> connects;
    }
}

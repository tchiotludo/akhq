package org.akhq.controllers;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;
import io.micronaut.views.View;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.akhq.configs.*;
import org.akhq.modules.HasAnyPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @HasAnyPermission()
    @Get("api/cluster")
    @Operation(tags = {"AKHQ"}, summary = "Get all cluster for current instance")
    public List<ClusterDefinition> list() {
        return this.connections
            .stream()
            .map(connection -> new ClusterDefinition(
                connection.getName(),
                connection.getSchemaRegistry() != null,
                (connection.getConnect() != null ? connection.getConnect() : new ArrayList<Connect>())
                    .stream()
                    .map(Connect::getName)
                    .collect(Collectors.toList())

            ))
            .collect(Collectors.toList());
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("api/auths")
    @Operation(tags = {"AKHQ"}, summary = "Get all auth details for current instance")
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
    @Operation(tags = {"AKHQ"}, summary = "Get current user")
    public AuthUser users() {
        AuthUser authUser = new AuthUser();

        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);

            securityService
                    .getAuthentication()
                    .ifPresent(authentication -> {
                        authUser.logged = true;
                        authUser.username = authentication.getName();
                    });
        }

        authUser.roles = this.getRights();

        return authUser;
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @View("rapidoc")
    @Get("api")
    @Hidden
    public HttpResponse<?> rapidoc() {
        MutableHttpResponse<Map<String, Object>> response = HttpResponse.ok();

        return response.body(templateData(Optional.empty()));
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
    @Introspected
    public static class ClusterDefinition {
        private String id;
        private boolean registry;
        private List<String> connects;
    }
}

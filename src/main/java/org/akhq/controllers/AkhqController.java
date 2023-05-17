package org.akhq.controllers;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.ldap.configuration.LdapConfiguration;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.utils.SecurityService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.akhq.configs.*;
import org.akhq.configs.security.*;
import org.akhq.security.annotation.HasAnyPermission;
import org.akhq.utils.VersionProvider;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AkhqController extends AbstractController {
    @Inject
    private List<Connection> connections;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private Oidc oidc;

    @Inject
    private Oauth oauth;

    @Inject
    private UIOptions uIOptions;

    @Inject
    @Nullable
    private HeaderAuth headerAuth;

    @Inject
    private VersionProvider versionProvider;

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
                    .collect(Collectors.toList()),
                (connection.getKsqldb() != null ? connection.getKsqldb() : new ArrayList<KsqlDb>())
                    .stream()
                    .map(KsqlDb::getName)
                    .collect(Collectors.toList())

            ))
            .collect(Collectors.toList());
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("api/auths")
    @Operation(tags = {"AKHQ"}, summary = "Get all auth details for current instance")
    public AuthDefinition auths() {
        AuthDefinition authDefinition = new AuthDefinition();

        if (oidc.isEnabled()) {
            authDefinition.oidcAuths = oidc.getProviders().entrySet()
                .stream()
                .map(e -> new OidcAuth(e.getKey(), e.getValue().getLabel()))
                .collect(Collectors.toList());
        }

        if (oauth.isEnabled()) {
            authDefinition.oauthAuths = oauth.getProviders().entrySet()
                .stream()
                .map(e -> new OauthAuth(e.getKey(), e.getValue().getLabel()))
                .collect(Collectors.toList());
        }

        if (applicationContext.containsBean(SecurityService.class)) {
            authDefinition.loginEnabled = true;
            // Display login form if there are LocalUsers OR Ldap is enabled
            authDefinition.formEnabled = securityProperties.getBasicAuth().size() > 0 ||
                    applicationContext.containsBean(LdapConfiguration.class);

            if (!authDefinition.formEnabled &&
                authDefinition.oidcAuths == null &&
                headerAuth != null && headerAuth.getUserHeader() != null
            ) {
                authDefinition.loginEnabled = false;
            }
        }
        authDefinition.version = versionProvider.getVersion();

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
    @Get("api")
    @Hidden
    public HttpResponse<?> rapidoc() {
        String doc = "<!doctype html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <title>Api | AKHQ</title>\n" +
            "  <meta charset='utf-8'/>\n" +
            "  <link rel=\"shortcut icon\" type=\"image/png\" href=\"/static/icon_black.png\" />\n" +
            "  <meta name='viewport' content='width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes'/>\n" +
            "  <link href=\"https://fonts.googleapis.com/css?family=Open+Sans:300,600&display=swap\" rel=\"stylesheet\">\n" +
            "  <script src='https://unpkg.com/rapidoc/dist/rapidoc-min.js'></script>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <rapi-doc id='rapidoc'\n" +
            "            layout=\"row\"\n" +
            "            sort-tags=\"true\"\n" +
            "            sort-endpoints-by=\"method\"\n" +
            "            show-header=\"false\"\n" +
            "            theme=\"dark\"\n" +
            "            header-color=\"#005f81\"\n" +
            "            primary-color=\"#33b5e5\"\n" +
            "            render-style=\"read\"\n" +
            "            schema-style=\"table\"\n" +
            "            regular-font='Open Sans'\n" +
            "  >\n" +
            "    <img src=\"" + getBasePath() + "/static/logo.svg\" slot=\"nav-logo\" alt=\"logo\" />\n" +
            "\n" +
            "  </rapi-doc>\n" +
            "  <script>\n" +
            "      const rapidoc = document.getElementById('rapidoc');\n" +
            "      rapidoc.setAttribute('spec-url', '" + getBasePath() + "/swagger/akhq.yml');\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>\n";

        return HttpResponse
            .ok()
            .contentType(MediaType.TEXT_HTML_TYPE)
            .body(doc);
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("api/{cluster}/ui-options")
    @Operation(tags = {"AKHQ"}, summary = "Get ui options for cluster")
    public Connection.UiOptions options(String cluster) {
        return this.connections.stream().filter(conn -> cluster.equals(conn.getName()))
                .map(conn -> conn.mergeOptions(this.uIOptions))
                .findAny()
                .orElseThrow(() -> new RuntimeException("No cluster found"));
    }

    private List<AuthUser.AuthPermissions> expandRoles(List<Group> groupBindings) {
        SecurityProperties securityProperties = applicationContext.getBean(SecurityProperties.class);

        return groupBindings.stream()
            .map(binding -> securityProperties.getRoles().entrySet().stream()
                    .filter(role -> role.getKey().equals(binding.getRole()))
                .map(Map.Entry::getValue)
                .flatMap(Collection::stream)
                .map(roleBinding -> new AuthUser.AuthPermissions(roleBinding, binding.getRestriction()))
                .collect(Collectors.toList()))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    protected List<AuthUser.AuthPermissions> getRights() {
        SecurityService securityService = applicationContext.getBean(SecurityService.class);

        if(securityService.getAuthentication().isEmpty())
            return List.of();

        List<Group> groupBindings = ((Map<String, List<?>>)securityService.getAuthentication().get().getAttributes().get("groups"))
            .values().stream()
            .flatMap(Collection::stream)
            .map(gb -> new ObjectMapper().convertValue(gb, Group.class))
            .collect(Collectors.toList());

        return expandRoles(groupBindings);
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class AuthDefinition {
        private boolean loginEnabled;
        private boolean formEnabled;
        private List<OidcAuth> oidcAuths;
        private List<OauthAuth> oauthAuths;
        private String version;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class OidcAuth {
        private String key;
        private String label;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class OauthAuth {
        private String key;
        private String label;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class AuthUser {
        private boolean logged = false;
        private String username;
        private List<AuthPermissions> roles = new ArrayList<>();

        @AllArgsConstructor
        @NoArgsConstructor
        @Getter
        public static class AuthPermissions {
            @JsonUnwrapped
            private Role role;

            @JsonUnwrapped
            private Group.Restriction restriction;
        }
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Introspected
    public static class ClusterDefinition {
        private String id;
        private boolean registry;
        private List<String> connects;
        private List<String> ksqldbs;
    }

}

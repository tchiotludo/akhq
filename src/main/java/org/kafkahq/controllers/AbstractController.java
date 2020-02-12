package org.kafkahq.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.cookie.Cookie;
import io.micronaut.security.utils.SecurityService;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Wither;
import org.kafkahq.configs.BasicAuth;
import org.kafkahq.configs.LdapGroup;
import org.kafkahq.configs.LdapUser;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.utils.UserGroupUtils;
import org.kafkahq.utils.VersionProvider;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

abstract public class AbstractController {
    private static final String SESSION_TOAST = "TOAST";
    private static Gson gson = new GsonBuilder()
        .enableComplexMapKeySerialization()
        .create();

    @Value("${kafkahq.server.base-path}")
    protected String basePath;

    @Inject
    private VersionProvider versionProvider;

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Value("${kafkahq.security.default-groups}")
    private List<String> defaultGroups;

    @Inject
    private List<BasicAuth> basicAuths;

    @Inject
    private List<LdapGroup> ldapAuths;

    @Inject
    private List<LdapUser> ldapUsers;

    @SuppressWarnings("unchecked")
    protected Map templateData(Optional<String> cluster, Object... values) {
        Map datas = CollectionUtils.mapOf(values);

        datas.put("tag", versionProvider.getTag());
        datas.put("clusters", this.kafkaModule.getClustersList());
        datas.put("basePath", getBasePath());
        datas.put("roles", getRights());

        cluster.ifPresent(s -> {
            datas.put("clusterId", s);
            datas.put("registryEnabled", this.kafkaModule.getRegistryRestClient(s) != null);

            Map<String, KafkaConnectClient> connectClientMap = this.kafkaModule.getConnectRestClient(s);
            if (connectClientMap != null) {
                Set<String> filteredConnects = filterConnectList(connectClientMap.keySet());
                datas.put("connectList", filteredConnects);
            }
        });

        if (applicationContext.containsBean(SecurityService.class)) {
            datas.put("loginEnabled", basicAuths.size() > 0 || ldapAuths.size() > 0 || ldapUsers.size() > 0);

            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            securityService
                .getAuthentication()
                .ifPresent(authentication -> datas.put("username", authentication.getName()));
        } else {
            datas.put("loginEnabled", false);
        }

        return datas;
    }

    private Set<String> filterConnectList(Set<String> keySet) {
        keySet.removeIf(key -> key.equals(""));
        return keySet;
    }

    @SuppressWarnings("unchecked")
    protected HttpResponse template(HttpRequest request, String cluster, Object... values) {
        Map datas = templateData(Optional.of(cluster), values);

        MutableHttpResponse<Map> response = HttpResponse.ok();

        request.getCookies()
            .findCookie(SESSION_TOAST)
            .ifPresent(s -> {
                datas.put("toast", s.getValue());
                response.cookie(Cookie.of(SESSION_TOAST, "").maxAge(0).path("/"));
            });

        return response.body(datas);
    }

    protected String getBasePath() {
        return basePath.replaceAll("/$","");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
    }

    protected <T> Toast toast(MutableHttpResponse<T> response, Toast toast) {
        Cookie cookie = Cookie
            .of(SESSION_TOAST, gson.toJson(toast
                .withTitle(toast.getTitle() != null ? toast.getTitle().replaceAll(";", ",") : null)
                .withMessage(toast.getMessage() != null ? toast.getMessage().replaceAll(";", ",") : null)
            ))
            .path("/");

        response.cookie(cookie);

        return toast;
    }

    private static List<String> expandRoles(List<String> roles) {
        return roles
            .stream()
            .map(s -> {
                ArrayList<String> rolesExpanded = new ArrayList<>();

                ArrayList<String> split = new ArrayList<>(Arrays.asList(s.split("/")));

                while (split.size() > 0) {
                    rolesExpanded.add(String.join("/", split));
                    split.remove(split.size() - 1);
                }

                return rolesExpanded;
            })
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected List<String> getRights() {
        if (!applicationContext.containsBean(SecurityService.class)) {
            return expandRoles(this.userGroupUtils.getUserRoles(this.defaultGroups));
        }

        SecurityService securityService = applicationContext.getBean(SecurityService.class);

        return expandRoles(
            securityService
                .getAuthentication()
                .map(authentication -> (List<String>) authentication.getAttributes().get("roles"))
                .orElseGet(() -> this.userGroupUtils.getUserRoles(this.defaultGroups))
        );
    }

    @Builder
    @Getter
    public static class Toast {
        public enum Type {
            success,
            error,
            warning,
            info,
            question
        }

        @Wither
        private String title;

        @Wither
        private String message;

        @Builder.Default
        private Type type = Type.info;
    }
}

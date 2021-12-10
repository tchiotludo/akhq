package org.akhq.controllers;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.utils.SecurityService;
import org.akhq.utils.DefaultGroupUtils;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

abstract public class AbstractController {
    @Value("${micronaut.server.context-path:}")
    protected String basePath;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private DefaultGroupUtils defaultGroupUtils;

    protected String getBasePath() {
        return basePath.replaceAll("/$","");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
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

    protected boolean isAllowed(String role) {
        return this.getRights()
            .stream()
            .anyMatch(s -> s.equals(role));
    }

    @SuppressWarnings("unchecked")
    protected List<String> getRights() {
        if (!applicationContext.containsBean(SecurityService.class)) {
            return expandRoles(this.defaultGroupUtils.getDefaultRoles());
        }

        SecurityService securityService = applicationContext.getBean(SecurityService.class);

        return expandRoles(
            securityService
                .getAuthentication()
                .map(authentication -> (List<String>) authentication.getAttributes().get("roles"))
                .orElseGet(() -> this.defaultGroupUtils.getDefaultRoles())
        );
    }
}

package org.akhq.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Inject;
import org.akhq.configs.security.Group;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.security.annotation.AKHQSecured;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

abstract public class AbstractController {

    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Inject
    SecurityService securityService;

    @Inject
    protected SecurityProperties securityProperties;

    @Value("${micronaut.server.context-path:}")
    protected String basePath;

    protected String getBasePath() {
        return basePath.replaceAll("/$","");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
    }

    protected void checkIfClusterAllowed(String cluster) {
        checkIfClusterAndResourceAllowed(cluster, null);
    }

    protected void checkIfClusterAndResourceAllowed(String cluster, String resource) {
        if(securityService.getAuthentication().isEmpty())
            throw new RuntimeException();

        StackWalker.StackFrame sf = walker.walk(frames ->
            frames.filter(frame -> frame.getDeclaringClass().equals(getClass()))
                .findFirst()
                .orElseThrow());

        boolean isAllowed;
        try {
            Method method = sf.getDeclaringClass().getDeclaredMethod(sf.getMethodName(), sf.getMethodType().parameterArray());
            AKHQSecured annotation;

            // Take the method annotation is present
            if(method.isAnnotationPresent(AKHQSecured.class)) {
                annotation = method.getAnnotation(AKHQSecured.class);
            } else {
                // Otherwise take the class annotation
                annotation = sf.getDeclaringClass().getAnnotation(AKHQSecured.class);
            }

            isAllowed = ((Map<String, List<?>>)securityService.getAuthentication().get().getAttributes().get("groups")).values().stream()
                .flatMap(Collection::stream)
                .map(gb -> new ObjectMapper().convertValue(gb, Group.class))
                // Get only group with role matching the method annotation resource and action
                .filter(groupBinding -> securityProperties.getRoles().entrySet().stream()
                    .filter(role -> groupBinding.getRole().equals(role.getKey()))
                    .flatMap(role -> role.getValue().stream())
                    .anyMatch(roleBinding -> roleBinding.getResources().contains(annotation.resource())
                        && roleBinding.getActions().contains(annotation.action())))
                // Check that resource and cluster patterns match
                .anyMatch(group -> {
                    boolean allowed = group.getRestriction().getClusters().stream()
                        .anyMatch(pattern -> Pattern.matches(pattern, cluster));

                    if(resource != null)
                        allowed = allowed && group.getRestriction().getPatterns().stream()
                                .anyMatch(pattern -> Pattern.matches(pattern, resource));

                    return allowed;
                });
        } catch (NoSuchMethodException e) {
            isAllowed = false;
        }

        if (!isAllowed)
            // Throw appropriate exception
            throw new RuntimeException();
    }
}

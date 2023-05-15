package org.akhq.controllers;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.security.annotation.AKHQSecured;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

abstract public class AbstractController {

    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

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

            isAllowed = securityProperties.getGroups().entrySet()
                .stream()
                .flatMap(groups -> groups.getValue().stream())
                // Get only group with role matching the method annotation resource and action
                .filter(group -> securityProperties.getRoles().get(group.getRole()).stream()
                    .anyMatch(role -> role.getResources().contains(annotation.resource())
                        && role.getActions().contains(annotation.action())))
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
            throw new RuntimeException();
    }
}

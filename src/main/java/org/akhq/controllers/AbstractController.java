package org.akhq.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthorizationException;
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
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

abstract public class AbstractController {

    private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    protected SecurityProperties securityProperties;

    @Value("${micronaut.server.context-path:}")
    protected String basePath;

    protected String getBasePath() {
        return basePath.replaceAll("/$", "");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
    }

    protected List<Group> getUserGroups() {
        Optional<Authentication> authentication = applicationContext.containsBean(SecurityService.class) ?
            applicationContext.getBean(SecurityService.class).getAuthentication() :
            Optional.empty();

        if (authentication.isEmpty()) {
            return List.of();
        }

        List<Group> groupBindings = ((Map<String, List<?>>) authentication.get().getAttributes().get("groups"))
            .values()
            .stream()
            .flatMap(Collection::stream)
            .map(gb -> new ObjectMapper().convertValue(gb, Group.class))
            .collect(Collectors.toList());

        // Add the default group if there is one
        if (groupBindings.isEmpty() && StringUtils.isNotEmpty(securityProperties.getDefaultGroup())
            && securityProperties.getGroups().get(securityProperties.getDefaultGroup()) != null) {
            groupBindings.addAll(securityProperties.getGroups().get(securityProperties.getDefaultGroup()));
        }

        return groupBindings;
    }

    /**
     * Build a list of regex based on the user's groups patterns attribute and the current cluster
     *
     * @param cluster
     * @return
     */
    protected List<String> buildUserBasedResourceFilters(String cluster) {
        if (!applicationContext.containsBean(SecurityService.class)
            || applicationContext.getBean(SecurityService.class).getAuthentication().isEmpty())
            return List.of();

        AKHQSecured annotation;
        try {
            annotation = getCallingAKHQSecuredAnnotation();
        } catch (NoSuchMethodException e) {
            return List.of();
        }

        return getUserGroups().stream()
            // Keep only group matching the cluster
            .filter(group -> group.getClusters()
                .stream()
                .anyMatch(c -> Pattern.matches(c, cluster)))
            // Iterate over all the roles of the user remaining groups to extract the restriction attribute for the
            // given cluster and resource
            .map(gb -> securityProperties.getRoles().get(gb.getRole())
                .stream()
                // Find roles with a resource and action matching the calling method AKHQSecured annotation
                .filter(role -> role.getResources().contains(annotation.resource())
                    && role.getActions().contains(annotation.action()))
                // Keep only the restriction attribute containing the patterns
                .map(role -> gb.getPatterns())
                .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .flatMap(Collection::stream)
            .distinct()
            .collect(Collectors.toList());
    }

    private AKHQSecured getCallingAKHQSecuredAnnotation() throws NoSuchMethodException {
        StackWalker.StackFrame sf = walker.walk(frames ->
            frames.filter(frame -> frame.getDeclaringClass().equals(getClass()))
                .findFirst()
                .orElseThrow());

        Method method = sf.getDeclaringClass().getDeclaredMethod(sf.getMethodName(), sf.getMethodType().parameterArray());
        AKHQSecured annotation;

        // Take the method annotation is present
        if (method.isAnnotationPresent(AKHQSecured.class)) {
            annotation = method.getAnnotation(AKHQSecured.class);
        } else {
            // Otherwise take the class annotation
            annotation = sf.getDeclaringClass().getAnnotation(AKHQSecured.class);
        }

        return annotation;
    }

    protected void checkIfClusterAllowed(String cluster) {
        checkIfClusterAndResourceAllowed(cluster, StringUtils.EMPTY_STRING);
    }

    protected void checkIfClusterAndResourceAllowed(String cluster, List<String> resources) {
        for(String resource : resources) {
            checkIfClusterAndResourceAllowed(cluster, resource);
        }
    }

    protected void checkIfClusterAndResourceAllowed(String cluster, String resource) {
        Optional<Authentication> authentication = applicationContext.containsBean(SecurityService.class) ?
            applicationContext.getBean(SecurityService.class).getAuthentication() :
            Optional.empty();

        if (authentication.isEmpty())
            return;

        StackWalker.StackFrame sf = walker.walk(frames ->
            frames.filter(frame -> frame.getDeclaringClass().equals(getClass()))
                .findFirst()
                .orElseThrow());

        boolean isAllowed;

        try {
            AKHQSecured annotation = getCallingAKHQSecuredAnnotation();

            isAllowed = ((Map<String, List<?>>)authentication.get().getAttributes().get("groups")).values().stream()
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
                    boolean allowed = group.getClusters().stream()
                        .anyMatch(pattern -> Pattern.matches(pattern, cluster));

                    if (StringUtils.isNotEmpty(resource)) {
                        allowed = allowed && group.getPatterns().stream()
                            .anyMatch(pattern -> Pattern.matches(pattern, resource));
                    }

                    return allowed;
                });
        } catch (NoSuchMethodException e) {
            isAllowed = false;
        }

        if (!isAllowed) {
            throw new AuthorizationException(authentication.get());
        }
    }
}

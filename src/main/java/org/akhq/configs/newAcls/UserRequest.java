package org.akhq.configs.newAcls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.http.scope.RequestScope;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SecurityProperties;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RequestScope
public class UserRequest {
    //ctor inject
    private SecurityProperties securityProperties;


    private List<Binding> bindings = List.of();

    private List<String> topicFilters = new ArrayList<>();
    private List<String> connectFilters = new ArrayList<>();
    private List<String> groupFilters = new ArrayList<>();
    private List<String> schemaFilters = new ArrayList<>();

    public UserRequest(@Nullable SecurityService securityService, SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        log.info("Creating UserRequest");
        // if micronaut.security.enabled=false, securityService is null
        Optional<Authentication> user = securityService != null ? securityService.getAuthentication() : Optional.empty();
        if (user.isPresent() && user.get().getAttributes().containsKey("bindings")) {
            bindings = parseBindings(user.get().getAttributes().get("bindings"))
                .stream()
                // keep only bindings matching existing permissions
                .filter(binding -> securityProperties.getPermissions().containsKey(binding.getPermission()))
                .collect(Collectors.toList());

        }
    }

    public boolean isRequestAllowed(Permission.Resource resourceType, Permission.Role role, String cluster, String resourceName) {

        return bindings.stream()
            // filter bindings matching Resource & Roles with Permission object
            .filter(binding -> {
                Permission permission = securityProperties.getPermissions().get(binding.getPermission());
                return permission.getResource() == resourceType && permission.getRoles().contains(role);
            })
            // filter on cluster
            .filter(binding -> binding.getClusters()
                .stream()
                .anyMatch(regex -> Pattern.matches(regex, cluster))
            )
            // match on resource name if set (true otherwise)
            .anyMatch(binding -> resourceName == null ||
                binding.getPatterns()
                    .stream()
                    .anyMatch(regex -> Pattern.matches(regex, resourceName))
            );
    }

    public List<String> getListFilterFor(Permission.Resource resourceType) {
        // creating the list filters
        return bindings.stream()
            .flatMap(binding -> {
                Permission permission = securityProperties.getPermissions().get(binding.getPermission());
                if (permission.getRoles().contains(Permission.Role.READ)
                    && permission.getResource() == resourceType) {
                    return binding.getPatterns().stream();
                } else {
                    return Stream.empty();
                }
            }).collect(Collectors.toList());
    }

    private List<Binding> parseBindings(Object o) {
        ObjectMapper mapper = new ObjectMapper();
        if (o instanceof List) {
            try {
                List<Binding> bindings = mapper.readValue(o.toString(), new TypeReference<>() {
                });
                return bindings;
            } catch (JsonProcessingException e) {
                log.error("Error deserializing JWT attribute", e);
            }
        }
        return List.of();

    }
}

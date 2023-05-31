package org.akhq.security.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.AbstractSecurityRule;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.security.token.RolesFinder;
import io.micronaut.web.router.MethodBasedRouteMatch;
import io.micronaut.web.router.RouteMatch;
import io.reactivex.Flowable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.Group;
import org.akhq.configs.security.Role;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.security.annotation.AKHQSecured;
import org.reactivestreams.Publisher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AKHQSecurityRule extends AbstractSecurityRule {
    /**
     * @param rolesFinder Roles Parser
     */
    public AKHQSecurityRule(RolesFinder rolesFinder) {
        super(rolesFinder);
    }

    @Inject
    SecurityProperties securityProperties;

    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, RouteMatch<?> routeMatch, Authentication authentication) {
        if (!(routeMatch instanceof MethodBasedRouteMatch)) {
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }

        MethodBasedRouteMatch<?, ?> methodRoute = ((MethodBasedRouteMatch<?, ?>) routeMatch);
        if (!methodRoute.hasAnnotation(AKHQSecured.class)) {
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }

        Optional<Role.Resource> optionalResource = methodRoute.getValue(AKHQSecured.class, "resource", Role.Resource.class);
        Optional<Role.Action> optionalAction = methodRoute.getValue(AKHQSecured.class, "action", Role.Action.class);
        if (optionalResource.isEmpty() || optionalAction.isEmpty()) {
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }

        if (!routeMatch.getVariableValues().containsKey("cluster")) {
            log.warn("Route matched AKHQSecured but no `cluster` provided");
            return Flowable.just(SecurityRuleResult.REJECTED);
        }
        String cluster = routeMatch.getVariableValues().get("cluster").toString();

        if (authentication == null) {
            log.warn("No authentication information provided");
            return Flowable.just(SecurityRuleResult.REJECTED);
        }

        List<Group> userGroups = ((Map<String, List<?>>)authentication.getAttributes().get("groups")).values().stream()
            .flatMap(Collection::stream)
            // Type mismatch during serialization from LinkedTreeMap to Group if we use List<Group>
            // Need to serialize Object to Group manually in the stream
            .map(gb -> new ObjectMapper().convertValue(gb, Group.class))
            .collect(Collectors.toList());

        boolean allowed = userGroups.stream()
            // Keep only bindings matching on cluster name
            .filter(binding -> binding.getClusters().stream()
                    .anyMatch(regex -> Pattern.matches(regex, cluster))
            )
            // Map to roles
            .map(binding -> securityProperties.getRoles().entrySet().stream()
                .filter(entry -> entry.getKey().equals(binding.getRole()))
                .flatMap(rb -> rb.getValue().stream())
                .collect(Collectors.toList()))
            // Flatten roles
            .flatMap(Collection::stream)
            // Match on Resource & Action
            .anyMatch(role -> role.getResources().contains(optionalResource.get())
                && role.getActions().contains(optionalAction.get()));

        if (allowed)
            return Flowable.just(SecurityRuleResult.ALLOWED);
        else
            return Flowable.just(SecurityRuleResult.REJECTED);
    }

    public static final Integer ORDER = SecuredAnnotationRule.ORDER - 100;

    public int getOrder() {
        return ORDER;
    }
}
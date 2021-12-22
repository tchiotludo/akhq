package org.akhq.configs.newAcls;

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
import org.akhq.configs.SecurityProperties;
import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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


        Optional<Permission.Resource> optionalResource = methodRoute.getValue(AKHQSecured.class, "resource", Permission.Resource.class);
        Optional<Permission.Role> optionalRole = methodRoute.getValue(AKHQSecured.class, "role", Permission.Role.class);
        if (optionalResource.isEmpty() || optionalRole.isEmpty()) {
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }

        if (!routeMatch.getVariableValues().containsKey("cluster")) {
            log.warn("Route matched AKHQSecured but no `cluster` provided");
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }
        String cluster = routeMatch.getVariableValues().get("cluster").toString();

        List<Binding> userBindings = (List<Binding>) authentication.getAttributes().get("bindings");
        boolean allowed = userBindings.stream()
            // keep only bindings matching on cluster name
            .filter(binding -> binding.getClusters()
                .stream()
                .anyMatch(regex -> Pattern.matches(regex, cluster))
            )
            // keep only bindings matching permission by name
            .filter(binding -> securityProperties.getPermissions().containsKey(binding.getPermission()))
            // map to Permission object
            .map(binding -> securityProperties.getPermissions().get(binding.getPermission()))
            // match on Resource & Role
            .anyMatch(permission -> permission.getResource() == optionalResource.get()
                && permission.getRoles().contains(optionalRole.get()));

        if (allowed)
            return Flowable.just(SecurityRuleResult.ALLOWED);
        else
            return Flowable.just(SecurityRuleResult.UNKNOWN);
    }

    public static final Integer ORDER = SecuredAnnotationRule.ORDER - 100;

    public int getOrder() {
        return ORDER;
    }
}

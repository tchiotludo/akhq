package org.akhq.configs.newAcls;

import com.nimbusds.jose.shaded.json.JSONObject;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.AbstractSecurityRule;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRule;
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
public class AKHQSecurityRule implements SecurityRule {

    @Inject
    UserRequest userRequest;

    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, RouteMatch<?> routeMatch, Authentication authentication) {
        log.info("Checking security for logged-in user");
        return Flowable.just(checkNotReact(request, routeMatch, userRequest));
    }
    public SecurityRuleResult checkNotReact(HttpRequest<?> request, RouteMatch<?> routeMatch, UserRequest userRequest){
        if (!(routeMatch instanceof MethodBasedRouteMatch)) {
            return SecurityRuleResult.UNKNOWN;
        }

        MethodBasedRouteMatch<?, ?> methodRoute = ((MethodBasedRouteMatch<?, ?>) routeMatch);
        if (!methodRoute.hasAnnotation(AKHQSecured.class)) {
            return SecurityRuleResult.UNKNOWN;
        }


        Optional<Permission.Resource> optionalResource = methodRoute.getValue(AKHQSecured.class, "resource", Permission.Resource.class);
        Optional<Permission.Role> optionalRole = methodRoute.getValue(AKHQSecured.class, "role", Permission.Role.class);
        if (optionalResource.isEmpty() || optionalRole.isEmpty()) {
            return SecurityRuleResult.UNKNOWN;
        }
        log.info("Route matches AKHQSecured annotation, validating");

        if (!routeMatch.getVariableValues().containsKey("cluster")) {
            log.warn("Required parameter `cluster` not provided");
            return SecurityRuleResult.REJECTED;
        }
        String cluster = routeMatch.getVariableValues().get("cluster").toString();

        boolean allowed = userRequest.isRequestAllowed(optionalResource.get(), optionalRole.get(), cluster, null);

        if (allowed)
            return SecurityRuleResult.ALLOWED;
        else {
            log.warn("AKHQSecured validation did not succeed: didn't find resource={}, roles={}, cluster={} in bindings",
                optionalResource.get(), optionalRole.get(), cluster);
            return SecurityRuleResult.REJECTED;
        }
    }

    public static final Integer ORDER = SecuredAnnotationRule.ORDER - 100;

    public int getOrder() {
        return ORDER;
    }
}

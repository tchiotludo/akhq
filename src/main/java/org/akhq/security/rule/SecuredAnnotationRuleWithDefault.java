package org.akhq.security.rule;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.security.token.RolesFinder;
import io.micronaut.web.router.MethodBasedRouteMatch;
import io.micronaut.web.router.RouteMatch;
import io.reactivex.Flowable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.security.annotation.HasAnyPermission;
import org.reactivestreams.Publisher;

import java.util.List;

@Singleton
@Replaces(SecuredAnnotationRule.class)
public class SecuredAnnotationRuleWithDefault extends SecuredAnnotationRule {
    @Inject
    protected SecurityProperties securityProperties;

    @Inject
    SecuredAnnotationRuleWithDefault(RolesFinder rolesFinder) {
        super(rolesFinder);
    }

    @Override
    protected List<String> getRoles(Authentication authentication) {
        return super.getRoles(authentication);
    }

    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, Authentication authentication) {
        RouteMatch<?> routeMatch = request.getAttribute(HttpAttributes.ROUTE_MATCH, RouteMatch.class).orElse(null);
        if (!(routeMatch instanceof MethodBasedRouteMatch)) {
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }

        MethodBasedRouteMatch<?, ?> methodRoute = ((MethodBasedRouteMatch<?, ?>) routeMatch);
        if (methodRoute.hasAnnotation(HasAnyPermission.class)) {
            if (authentication != null || securityProperties.getDefaultGroup() != null) {
                return Flowable.just(SecurityRuleResult.ALLOWED);
            } else {
                return Flowable.just(SecurityRuleResult.REJECTED);
            }
        }

        return super.check(request, authentication);
    }
}

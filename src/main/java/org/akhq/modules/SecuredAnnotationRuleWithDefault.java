package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
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
import org.akhq.utils.DefaultGroupUtils;
import org.reactivestreams.Publisher;

import java.util.List;

@Singleton
@Replaces(SecuredAnnotationRule.class)
public class SecuredAnnotationRuleWithDefault extends SecuredAnnotationRule {
    @Inject
    SecuredAnnotationRuleWithDefault(RolesFinder rolesFinder) {
        super(rolesFinder);
    }

    @Inject
    private DefaultGroupUtils defaultGroupUtils;

    @Override
    protected List<String> getRoles(Authentication authentication) {
        List<String> roles = super.getRoles(authentication);

        roles.addAll(this.defaultGroupUtils.getDefaultRoles());

        return roles;
    }

    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, @Nullable RouteMatch<?> routeMatch, Authentication authentication) {
        if (!(routeMatch instanceof MethodBasedRouteMatch)) {
            return Flowable.just(SecurityRuleResult.UNKNOWN);
        }

        MethodBasedRouteMatch<?, ?> methodRoute = ((MethodBasedRouteMatch<?, ?>) routeMatch);
        if (methodRoute.hasAnnotation(HasAnyPermission.class)) {
            if (getRoles(authentication)
                .stream()
                .anyMatch(s -> !s.equals(SecurityRule.IS_ANONYMOUS))
            ) {
                return Flowable.just(SecurityRuleResult.ALLOWED);
            } else {
                return Flowable.just(SecurityRuleResult.REJECTED);
            }
        }

        return super.check(request, routeMatch, authentication);
    }
}

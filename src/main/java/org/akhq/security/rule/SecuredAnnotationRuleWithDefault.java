package org.akhq.security.rule;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.BasicHttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.security.token.RolesFinder;
import io.micronaut.web.router.MethodBasedRouteMatch;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.security.annotation.HasAnyPermission;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

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
        var routeMatchInfo = BasicHttpAttributes.getRouteMatchInfo(request);
        if (routeMatchInfo.isEmpty() || !(routeMatchInfo.get() instanceof MethodBasedRouteMatch)) {
            return Mono.just(SecurityRuleResult.UNKNOWN);
        }

        MethodBasedRouteMatch<?, ?> methodRoute = ((MethodBasedRouteMatch<?, ?>) routeMatchInfo.get());
        if (methodRoute.hasAnnotation(HasAnyPermission.class)) {
            if (authentication != null || securityProperties.getDefaultGroup() != null) {
                return Mono.just(SecurityRuleResult.ALLOWED);
            } else {
                return Mono.just(SecurityRuleResult.REJECTED);
            }
        }

        return super.check(request, authentication);
    }
}

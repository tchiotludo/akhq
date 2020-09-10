package org.akhq.modules;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.security.token.RolesFinder;
import io.micronaut.web.router.MethodBasedRouteMatch;
import io.micronaut.web.router.RouteMatch;
import org.akhq.configs.SecurityProperties;
import org.akhq.utils.UserGroupUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
@Replaces(SecuredAnnotationRule.class)
public class SecuredAnnotationRuleWithDefault extends SecuredAnnotationRule {
    @Inject
    SecuredAnnotationRuleWithDefault(RolesFinder rolesFinder) {
        super(rolesFinder);
    }

    @Inject
    private SecurityProperties securityProperties;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    protected List<String> getRoles(Map<String, Object> claims) {
        List<String> roles = super.getRoles(claims);

        roles.addAll(this.userGroupUtils.getUserRoles(Collections.singletonList(securityProperties.getDefaultGroup())));

        return roles;
    }

    @Override
    public SecurityRuleResult check(HttpRequest<?> request, @Nullable RouteMatch<?> routeMatch, @Nullable Map<String, Object> claims) {
        if (!(routeMatch instanceof MethodBasedRouteMatch)) {
            return SecurityRuleResult.UNKNOWN;
        }
        MethodBasedRouteMatch<?, ?> methodRoute = ((MethodBasedRouteMatch<?, ?>) routeMatch);
        if (methodRoute.hasAnnotation(HasAnyPermission.class)) {
            if(getRoles(claims).stream()
                    .anyMatch(s -> !s.equals(SecurityRule.IS_ANONYMOUS))) {
                return SecurityRuleResult.ALLOWED;
            } else {
                return SecurityRuleResult.REJECTED;
            }
        }
        return super.check(request, routeMatch, claims);
    }
}

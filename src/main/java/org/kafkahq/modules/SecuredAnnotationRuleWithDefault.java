package org.kafkahq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.token.RolesFinder;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
@Replaces(SecuredAnnotationRule.class)
public class SecuredAnnotationRuleWithDefault extends SecuredAnnotationRule {
    @Inject
    SecuredAnnotationRuleWithDefault(RolesFinder rolesFinder) {
        super(rolesFinder);
    }

    @Value("${kafkahq.security.default-roles}")
    List<String> defaultRoles;

    @Override
    protected List<String> getRoles(Map<String, Object> claims) {
        List<String> roles = super.getRoles(claims);

        roles.addAll(defaultRoles);

        return roles;
    }
}

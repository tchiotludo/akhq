package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.token.RolesFinder;
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

    @Value("${akhq.security.default-group}")
    String defaultGroups;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Override
    protected List<String> getRoles(Map<String, Object> claims) {
        List<String> roles = super.getRoles(claims);

        roles.addAll(this.userGroupUtils.getUserRoles(Collections.singletonList(defaultGroups)));

        return roles;
    }
}

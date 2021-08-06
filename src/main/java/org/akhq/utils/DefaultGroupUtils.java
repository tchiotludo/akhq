package org.akhq.utils;

import io.micronaut.core.util.StringUtils;
import org.akhq.configs.SecurityProperties;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class DefaultGroupUtils {

    @Inject
    private SecurityProperties securityProperties;

    /**
     * Get all default roles for all users (authenticated and not)
     *
     * @return list of roles
     */
    public List<String> getDefaultRoles() {
        if (securityProperties.getGroups() == null || StringUtils.isEmpty(securityProperties.getDefaultGroup())) {
            return null;
        }

        return securityProperties.getGroups().values().stream()
            .filter(group -> securityProperties.getDefaultGroup().equals(group.getName()))
            .filter(group -> group.getRoles() != null)
            .flatMap(group -> group.getRoles().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Get all default attributes for all users (authenticated and not)
     *
     * @return Map<attribute_name, List < attribute_value>>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDefaultAttributes() {
        if (securityProperties.getGroups() == null || StringUtils.isEmpty(securityProperties.getDefaultGroup())) {
            return null;
        }

        return securityProperties.getGroups().values().stream()
            .filter(group -> securityProperties.getDefaultGroup().equals(group.getName()))
            .flatMap(group -> (group.getAttributes() != null) ? group.getAttributes().entrySet().stream() : null)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                item -> new ArrayList<>(item.getValue()),
                (e1, e2) -> {
                    ((List) e1).addAll((List) e2); return e1;
                }
            ));
    }
}

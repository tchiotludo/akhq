package org.akhq.utils;

import io.micronaut.core.util.StringUtils;
import org.akhq.configs.Group;
import org.akhq.configs.GroupMapping;
import org.akhq.configs.UserMapping;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class UserGroupUtils {

    @Inject
    private List<Group> akhqGroups;

    /**
     * Get all distinct roles for the list of groups
     *
     * @param groups list of user groups
     * @return list of roles
     */
    public List<String> getUserRoles(List<String> groups) {
        if (this.akhqGroups == null || groups == null) {
            return new ArrayList<>();
        }

        return this.akhqGroups.stream()
            .filter(group -> groups.contains(group.getName()))
            .filter(group -> group.getRoles() != null)
            .flatMap(group -> group.getRoles().stream())
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Merge all group attributes in a Map
     *
     * @param groups list of user groups
     * @return Map<attribute_name, List < attribute_value>>
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserAttributes(List<String> groups) {
        if (this.akhqGroups == null || groups == null) {
            return null;
        }

        return this.akhqGroups.stream()
            .filter(group -> groups.contains(group.getName()))
            .flatMap(group -> (group.getAttributes() != null) ? group.getAttributes().entrySet().stream() : null)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                item -> new ArrayList<>(Collections.singletonList(item.getValue())),
                (e1, e2) -> {
                    ((List) e1).addAll((List) e2); return e1;
                }
            ));
    }

    /**
     * Maps the provider username and a set of provider groups to AKHQ groups using group and user mappings.
     *
     * @param username the username to use
     * @param providerGroups the groups from the provider side
     * @param groupMappings the group mappings configured for the provider
     * @param userMappings the user mappings configured for the provider
     * @param defaultGroup a default group for the provider
     * @return the mapped AKHQ groups
     */
    public static List<String> mapToAkhqGroups(
            String username,
            Set<String> providerGroups,
            List<GroupMapping> groupMappings,
            List<UserMapping> userMappings,
            String defaultGroup
    ) {
        Stream<String> defaultGroupStream = StringUtils.hasText(defaultGroup) ? Stream.of(defaultGroup) : Stream.empty();
        return Stream.concat(
                Stream.concat(
                        userMappings.stream()
                                .filter(mapping -> username.equalsIgnoreCase(mapping.getUsername()))
                                .flatMap(mapping -> mapping.getGroups().stream()),
                        groupMappings.stream()
                                .filter(mapping -> providerGroups.stream().anyMatch(s -> s.equalsIgnoreCase(mapping.getName())))
                                .flatMap(mapping -> mapping.getGroups().stream())
                ),
                defaultGroupStream
        ).distinct().collect(Collectors.toList());
    }

    /**
     * Finalizes a mappings map by setting the keys on to the objects.
     * @param mappings the mappings to finalize
     */
    public static void finalizeUserMappings(Map<String, UserMapping> mappings) {
        mappings.forEach((key, value) -> {
            if (StringUtils.isEmpty(value.getUsername())) {
                value.setUsername(key);
            }
        });
    }

    /**
     * Finalizes a mappings map by setting the keys on to the objects.
     * @param mappings the mappings to finalize
     */
    public static void finalizeGroupMappings(Map<String, GroupMapping> mappings) {
        mappings.forEach((key, value) -> {
            if (StringUtils.isEmpty(value.getName())) {
                value.setName(key);
            }
        });
    }
}

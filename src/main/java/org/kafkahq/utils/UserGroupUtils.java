package org.kafkahq.utils;

import org.kafkahq.configs.Group;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class UserGroupUtils {

    @Inject
    private List<Group> kafkaHqGroups;

    /**
     * Get all distinct roles for the list of groups
     *
     * @param groups list of user groups
     * @return list of roles
     */
    public List<String> getUserRoles(List<String> groups) {
        if (this.kafkaHqGroups == null || groups == null) {
            return new ArrayList<>();
        }

        return this.kafkaHqGroups.stream()
            .filter(group -> groups.contains(group.getName()))
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
    public Map<String, Object> getUserAttributes(List<String> groups) {
        if (this.kafkaHqGroups == null || groups == null) {
            return null;
        }

        return this.kafkaHqGroups.stream()
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
}

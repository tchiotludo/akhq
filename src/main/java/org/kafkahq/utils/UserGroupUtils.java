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

    public List<String> getUserRoles(List<String> groups) {

        // Get all KafkaHQ Group roles by ldap group name
        return this.kafkaHqGroups.stream()
                .filter(group -> groups.contains(group.getName()))
                .flatMap(group -> group.getRoles().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserAttributes(List<String> groups) {

        return this.kafkaHqGroups.stream().filter(group -> groups.contains(group.getName()))
                .flatMap(group -> group.getAttributes().entrySet().stream())
                .collect(Collectors.toMap(
                        item -> item.getKey(),
                        item -> new ArrayList<>(Arrays.asList(item.getValue())),
                        (e1, e2) -> { ((List)e1).addAll((List)e2); return e1; }
                ));
    }
}

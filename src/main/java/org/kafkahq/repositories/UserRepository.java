package org.kafkahq.repositories;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.kafkahq.models.User;
import org.kafkahq.modules.KafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class UserRepository extends AbstractRepository {

    @Inject
    private KafkaWrapper kafkaWrapper;

    public List<User> findAll(String clusterId, Optional<String> search) {
        try {
            return kafkaWrapper.describeAcls(clusterId, AclBindingFilter.ANY).stream()
                    .map(acl -> acl.entry().principal())
                    .distinct()
                    .filter(principal -> isSearchMatch(search, principal))
                    .map(User::new)
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }

    public User findByUser(String clusterId, String encodedUser, Optional<String> resourceType) {
        String username = User.decodeUsername(encodedUser);
        try {
            var userAcls = kafkaWrapper.describeAcls(clusterId, filterForUsername(username, resourceType))
                    .stream().collect(Collectors.toList());
            return toUser(username, userAcls);
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }

    public List<User> findByResourceType(String clusterId, ResourceType resourceType, String resourceName) {
        try {
            return kafkaWrapper.describeAcls(clusterId, filterForResource(resourceType, resourceName))
                    .stream()
                    .collect(
                            Collectors.groupingBy(
                                    acl -> acl.entry().principal(),
                                    Collectors.toList()))
                    .entrySet().stream()
                    .map(entry -> toUser(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }

    private AclBindingFilter filterForResource(ResourceType resourceType,String resourceName) {
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourceType, resourceName, PatternType.ANY);
        return new AclBindingFilter(resourcePatternFilter, AccessControlEntryFilter.ANY);
    }

    private AclBindingFilter filterForUsername(String username, Optional<String> resourceTypeFilter) {
        ResourceType resourceType = resourceTypeFilter.isPresent() ? ResourceType.fromString(resourceTypeFilter.get()) : ResourceType.ANY;
        AccessControlEntryFilter accessControlEntryFilter = new AccessControlEntryFilter(username, null, AclOperation.ANY, AclPermissionType.ANY);
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourceType, null, PatternType.ANY);
        return new AclBindingFilter(resourcePatternFilter, accessControlEntryFilter);
    }

    private User toUser(String username, List<AclBinding> userAcls) {
        User user = new User(username, new HashMap<>());

        userAcls.stream()
                .collect(Collectors.groupingBy(
                        acl -> new HostResourceType(
                                new User.HostResource(
                                        acl.entry().host(),
                                        acl.pattern().patternType().name().toLowerCase() + ":" + acl.pattern().name()),
                                acl.pattern().resourceType()),
                        Collectors.mapping(acl -> acl.entry().operation().name(), Collectors.toList()))
                )
                .entrySet().stream()
                .forEach(entry -> {
                    user.getAcls().putIfAbsent(entry.getKey().resourceType.name().toLowerCase(), new HashMap<>());
                    user.getAcls().get(entry.getKey().resourceType.name().toLowerCase()).put(entry.getKey().hostResource, entry.getValue());
                });
        return user;
    }

    @Data
    @AllArgsConstructor
    class HostResourceType {
        private User.HostResource hostResource;
        private ResourceType resourceType;
    }
}

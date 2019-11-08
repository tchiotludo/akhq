package org.kafkahq.repositories;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.kafkahq.models.AccessControlList;
import org.kafkahq.modules.KafkaWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class AccessControlListRepository extends AbstractRepository {

    @Inject
    private KafkaWrapper kafkaWrapper;

    public List<AccessControlList> findAll(String clusterId, Optional<String> search) {
        try {
            return kafkaWrapper.describeAcls(clusterId, AclBindingFilter.ANY).stream()
                    .map(acl -> acl.entry().principal())
                    .distinct()
                    .filter(principal -> isSearchMatch(search, principal))
                    .map(AccessControlList::new)
                    .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }

    public AccessControlList findByPrincipal(String clusterId, String encodedPrincipal, Optional<String> resourceType) {
        String principal = AccessControlList.decodePrincipal(encodedPrincipal);
        try {
            var aclBindings = kafkaWrapper.describeAcls(clusterId, filterForPrincipal(principal, resourceType))
                    .stream().collect(Collectors.toList());
            return toAcl(principal, aclBindings);
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }

    public List<AccessControlList> findByResourceType(String clusterId, ResourceType resourceType, String resourceName) {
        try {
            return kafkaWrapper.describeAcls(clusterId, filterForResource(resourceType, resourceName))
                    .stream()
                    .collect(
                            Collectors.groupingBy(
                                    acl -> acl.entry().principal(),
                                    Collectors.toList()))
                    .entrySet().stream()
                    .map(entry -> toAcl(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException ex) {
            throw new CompletionException(ex);
        }
    }

    private AclBindingFilter filterForResource(ResourceType resourceType,String resourceName) {
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourceType, resourceName, PatternType.ANY);
        return new AclBindingFilter(resourcePatternFilter, AccessControlEntryFilter.ANY);
    }

    private AclBindingFilter filterForPrincipal(String principal, Optional<String> resourceTypeFilter) {
        ResourceType resourceType = resourceTypeFilter.isPresent() ? ResourceType.fromString(resourceTypeFilter.get()) : ResourceType.ANY;
        AccessControlEntryFilter accessControlEntryFilter = new AccessControlEntryFilter(principal, null, AclOperation.ANY, AclPermissionType.ANY);
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourceType, null, PatternType.ANY);
        return new AclBindingFilter(resourcePatternFilter, accessControlEntryFilter);
    }

    private AccessControlList toAcl(String principal, List<AclBinding> aclBindings) {
        var permissions = new HashMap<String, Map<AccessControlList.HostResource,List<String>>>();

        aclBindings.stream()
                .collect(Collectors.groupingBy(
                        acl -> new HostResourceType(
                                new AccessControlList.HostResource(
                                        acl.entry().host(),
                                        acl.pattern().patternType().name().toLowerCase() + ":" + acl.pattern().name()),
                                acl.pattern().resourceType()),
                        Collectors.mapping(aclBinding -> aclBinding.entry().operation().name(), Collectors.toList()))
                )
                .entrySet().stream()
                .forEach(entry -> {
                    permissions.putIfAbsent(entry.getKey().resourceType.name().toLowerCase(), new HashMap<>());
                    permissions.get(entry.getKey().resourceType.name().toLowerCase()).put(entry.getKey().hostResource, entry.getValue());
                });
        return new AccessControlList(principal, permissions);
    }

    @Data
    @AllArgsConstructor
    class HostResourceType {
        private AccessControlList.HostResource hostResource;
        private ResourceType resourceType;
    }
}

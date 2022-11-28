package org.akhq.repositories;

import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.utils.SecurityService;
import io.micronaut.context.ApplicationContext;
import org.akhq.models.AccessControl;
import org.akhq.modules.AbstractKafkaWrapper;
import org.akhq.utils.DefaultGroupUtils;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class AccessControlListRepository extends AbstractRepository {
    @Inject
    private AbstractKafkaWrapper kafkaWrapper;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private DefaultGroupUtils defaultGroupUtils;

    public List<AccessControl> findAll(String clusterId, Optional<String> search) throws ExecutionException, InterruptedException {
        return toGroupedAcl(kafkaWrapper
            .describeAcls(clusterId, AclBindingFilter.ANY)
            .stream()
            .filter(aclBinding -> isSearchMatch(search, aclBinding.entry().principal()))
            .filter(aclBinding -> isMatchRegex(getAclFilterRegex(),aclBinding.entry().principal()))
            .collect(Collectors.toList())
        );
    }

    public AccessControl findByPrincipal(String clusterId, String encodedPrincipal, Optional<ResourceType> resourceType) throws ExecutionException, InterruptedException {
        String principal = AccessControl.decodePrincipal(encodedPrincipal);

        return new AccessControl(
            principal,
            kafkaWrapper.describeAcls(clusterId, filterForPrincipal(principal, resourceType))
        );
    }

    public List<AccessControl> findByResourceType(String clusterId, ResourceType resourceType, String resourceName) throws ExecutionException, InterruptedException {
        return toGroupedAcl(kafkaWrapper.describeAcls(clusterId, filterForResource(resourceType, resourceName)));
    }

    private static AclBindingFilter filterForResource(ResourceType resourceType, String resourceName) {
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourceType, resourceName, PatternType.ANY);

        return new AclBindingFilter(resourcePatternFilter, AccessControlEntryFilter.ANY);
    }

    private static AclBindingFilter filterForPrincipal(String principal, Optional<ResourceType> resourceTypeFilter) {
        AccessControlEntryFilter accessControlEntryFilter = new AccessControlEntryFilter(principal, null, AclOperation.ANY, AclPermissionType.ANY);
        ResourcePatternFilter resourcePatternFilter = new ResourcePatternFilter(resourceTypeFilter.orElse(ResourceType.ANY), null, PatternType.ANY);

        return new AclBindingFilter(resourcePatternFilter, accessControlEntryFilter);
    }

    private static List<AccessControl> toGroupedAcl(Collection<AclBinding> aclBindings) {
        return aclBindings
            .stream()
            .collect(
                Collectors.groupingBy(
                    acl -> acl.entry().principal(),
                    Collectors.toList()
                )
            )
            .entrySet()
            .stream()
            .map(entry -> new AccessControl(
                entry.getKey(),
                entry.getValue()
            ))
            .collect(Collectors.toList());
    }

    private Optional<List<String>> getAclFilterRegex() {

        List<String> aclFilterRegex = new ArrayList<>();

        if (applicationContext.containsBean(SecurityService.class)) {
            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            Optional<Authentication> authentication = securityService.getAuthentication();
            if (authentication.isPresent()) {
                Authentication auth = authentication.get();
                aclFilterRegex.addAll(getAclFilterRegexFromAttributes(auth.getAttributes()));
            }
        }
        // get topic filter regex for default groups
        aclFilterRegex.addAll(getAclFilterRegexFromAttributes(
            defaultGroupUtils.getDefaultAttributes()
        ));

        return Optional.of(aclFilterRegex);
    }

    @SuppressWarnings("unchecked")
    private List<String> getAclFilterRegexFromAttributes(Map<String, Object> attributes) {
        if ((attributes.get("aclsFilterRegexp") != null) && (attributes.get("aclsFilterRegexp") instanceof List)) {
		    return (List<String>)attributes.get("aclsFilterRegexp");
		}
        return new ArrayList<>();
    }
}

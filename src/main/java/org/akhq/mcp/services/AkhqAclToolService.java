package org.akhq.mcp.services;

import jakarta.inject.Singleton;
import org.akhq.mcp.model.GetPrincipalAclsResult;
import org.akhq.mcp.model.GetTopicAclsResult;
import org.akhq.mcp.model.SearchAclsResult;
import org.akhq.models.AccessControl;
import org.akhq.repositories.AccessControlListRepository;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Singleton
public class AkhqAclToolService {
    private final AccessControlListRepository repository;

    public AkhqAclToolService(AccessControlListRepository repository) {
        this.repository = repository;
    }

    public SearchAclsResult searchAcls(String cluster, List<String> filters, String search, Integer maxResults)
        throws ExecutionException, InterruptedException {
        int limit = McpLimits.maxResults(maxResults);
        List<SearchAclsResult.PrincipalOverview> principals = repository.findAll(cluster, McpLimits.search(search), filters).stream()
            .map(control -> new SearchAclsResult.PrincipalOverview(
                control.getPrincipal(), control.getAcls() == null ? 0 : control.getAcls().size()
            ))
            .sorted(Comparator.comparing(SearchAclsResult.PrincipalOverview::principal, String::compareToIgnoreCase))
            .toList();
        List<SearchAclsResult.PrincipalOverview> returned = principals.subList(0, Math.min(limit, principals.size()));
        boolean truncated = principals.size() > returned.size();

        return new SearchAclsResult(
            cluster, principals.size(), truncated, returned,
            McpLimits.listMessage("principal", principals.size(), returned.size(), truncated)
        );
    }

    public GetPrincipalAclsResult getPrincipalAcls(String cluster, String principal, String resourceType, Integer maxResults)
        throws ExecutionException, InterruptedException {
        int limit = McpLimits.maxItems(maxResults);
        AccessControl control = repository.findByPrincipal(
            cluster, AccessControl.encodePrincipal(principal), parseResourceType(resourceType)
        );

        List<GetPrincipalAclsResult.Acl> acls = toAcls(control);
        List<GetPrincipalAclsResult.Acl> returned = acls.subList(0, Math.min(limit, acls.size()));
        boolean truncated = acls.size() > returned.size();

        return new GetPrincipalAclsResult(
            cluster, principal, acls.size(), truncated, returned,
            acls.isEmpty()
                ? "No ACL found for this principal. Use the exact principal, including its type, such as `User:alice`."
                : truncated
                    ? "Found " + acls.size() + " ACLs, returning the first " + returned.size()
                        + ". Set `resourceType` or raise `maxResults` to see the others."
                    : "Found " + acls.size() + " ACL(s)."
        );
    }

    public GetTopicAclsResult getTopicAcls(String cluster, String topic, Integer maxResults)
        throws ExecutionException, InterruptedException {
        int limit = McpLimits.maxResults(maxResults);
        List<GetTopicAclsResult.PrincipalAcls> principals = repository.findByResourceType(cluster, ResourceType.TOPIC, topic).stream()
            .map(control -> new GetTopicAclsResult.PrincipalAcls(control.getPrincipal(), toAcls(control)))
            .sorted(Comparator.comparing(GetTopicAclsResult.PrincipalAcls::principal, String::compareToIgnoreCase))
            .toList();
        List<GetTopicAclsResult.PrincipalAcls> returned = principals.subList(0, Math.min(limit, principals.size()));
        boolean truncated = principals.size() > returned.size();

        return new GetTopicAclsResult(
            cluster, topic, principals.size(), truncated, returned,
            McpLimits.listMessage("principal", principals.size(), returned.size(), truncated)
        );
    }

    private static List<GetPrincipalAclsResult.Acl> toAcls(AccessControl control) {
        return Optional.ofNullable(control.getAcls()).orElse(List.of()).stream()
            .map(acl -> new GetPrincipalAclsResult.Acl(
                acl.getResource().getResourceType().name(),
                acl.getResource().getName(),
                acl.getResource().getPatternType().name(),
                acl.getOperation().getOperation().name(),
                acl.getOperation().getPermissionType().name(),
                acl.getHost()
            ))
            .sorted(Comparator
                .comparing(GetPrincipalAclsResult.Acl::resourceType)
                .thenComparing(GetPrincipalAclsResult.Acl::resourceName, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(GetPrincipalAclsResult.Acl::operation))
            .toList();
    }

    private static Optional<ResourceType> parseResourceType(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            ResourceType type = ResourceType.valueOf(value.trim().toUpperCase());
            if (type == ResourceType.ANY || type == ResourceType.UNKNOWN) {
                throw new IllegalArgumentException();
            }
            return Optional.of(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "`arguments.resourceType` must be one of TOPIC, GROUP, CLUSTER, TRANSACTIONAL_ID, DELEGATION_TOKEN, USER", e
            );
        }
    }
}

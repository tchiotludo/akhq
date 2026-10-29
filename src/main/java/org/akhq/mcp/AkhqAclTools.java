package org.akhq.mcp;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.MicronautMcpTransportContext;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.rules.SecurityRule;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import jakarta.inject.Singleton;
import org.akhq.configs.security.Role;
import org.akhq.mcp.model.*;
import org.akhq.mcp.services.AkhqAclToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.ACL, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqAclTools extends AbstractMcpTool {
    private final AkhqAclToolService service;

    public AkhqAclTools(AkhqAclToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.search_acls",
        description = """
            Search the principals having ACLs on a cluster.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "search": "optional space separated terms",
              "maxResults": 50
            }

            Rules:
            - `cluster` is required.
            - A principal matches when it contains every `search` term, case insensitive. Omit `search` to list every principal.
            - `maxResults` defaults to 50 and is capped at 200. Principals are sorted by name and come with their `aclCount`.
            - Only principals the caller is allowed to see are returned. Use `akhq.get_principal_acls` for the ACLs of one principal.
            """
    )
    public CallToolResult searchAcls(SearchAclsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            return toolResult(service.searchAcls(scope.cluster(), scope.resourceFilters(), arguments.search(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_principal_acls",
        description = """
            Get the ACLs of one principal: resource type, resource name, pattern type, operation, permission and host.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "principal": "User:alice",
              "resourceType": "TOPIC",
              "maxResults": 100
            }

            Rules:
            - `cluster` and `principal` are required. `principal` is the exact principal, including its type.
            - `resourceType` optionally restricts the ACLs to TOPIC, GROUP, CLUSTER, TRANSACTIONAL_ID, DELEGATION_TOKEN or USER.
            - `maxResults` defaults to 100 and is capped at 500.
            - A principal without ACL returns an empty list.
            """
    )
    public CallToolResult getPrincipalAcls(GetPrincipalAclsArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.principal(), "principal");
            return toolResult(service.getPrincipalAcls(scope.cluster(), scope.resource(), arguments.resourceType(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}

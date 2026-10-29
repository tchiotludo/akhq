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
import org.akhq.mcp.services.AkhqSchemaToolService;
import org.akhq.security.annotation.AKHQSecured;

@Secured(SecurityRule.IS_AUTHENTICATED)
@AKHQSecured(resource = Role.Resource.SCHEMA, action = Role.Action.READ)
@Singleton
@Requires(property = "akhq.mcp.enabled", value = "true")
public class AkhqSchemaTools extends AbstractMcpTool {
    private final AkhqSchemaToolService service;

    public AkhqSchemaTools(AkhqSchemaToolService service) {
        this.service = service;
    }

    @Tool(
        name = "akhq.search_schemas",
        description = """
            Search the Schema Registry subjects of a cluster by name.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "search": "optional space separated terms",
              "maxResults": 50
            }

            Rules:
            - `cluster` is required. A cluster without Schema Registry has no subject.
            - A subject matches when its name contains every `search` term, case insensitive. Omit `search` to list every subject.
            - `maxResults` defaults to 50 and is capped at 200. Subjects are sorted by name.
            - Only subjects the caller is allowed to see are returned. Use `akhq.get_schema` to read a subject.

            When `truncated` is true, tell the user that `totalMatches` subjects matched and only part of them are shown.
            """
    )
    public CallToolResult searchSchemas(SearchSchemasArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ClusterScope scope = authorizeClusterScope(arguments, transportContext);
            return toolResult(service.searchSchemas(scope.cluster(), scope.resourceFilters(), arguments.search(), arguments.maxResults()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }

    @Tool(
        name = "akhq.get_schema",
        description = """
            Get a schema of a subject: its definition, type, compatibility level, references and the list of its versions.

            Expected `arguments` JSON object:
            {
              "cluster": "<cluster-name>",
              "subject": "<subject>",
              "version": 3
            }

            Rules:
            - `cluster` and `subject` are required.
            - `version` defaults to the latest version. `versions` lists every version of the subject.
            - `schema` is the raw definition (Avro, JSON schema or Protobuf). It is cut at 100000 characters, flagged with `schemaTruncated: true`.
            """
    )
    public CallToolResult getSchema(GetSchemaArguments arguments, MicronautMcpTransportContext transportContext) throws Exception {
        try {
            ResourceScope scope = authorizeResourceScope(arguments, transportContext, arguments.subject(), "subject");
            return toolResult(service.getSchema(scope.cluster(), scope.resource(), arguments.version()));
        } catch (IllegalArgumentException | AuthorizationException e) {
            return toolError(e);
        }
    }
}

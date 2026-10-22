package org.akhq.security.authentication.mcp;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.oauth2.metadata.DefaultProtectedResourceMetadataProvider;
import io.micronaut.security.oauth2.metadata.ProtectedResourceMetadata;
import io.micronaut.security.oauth2.metadata.ProtectedResourceMetadataProvider;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

import java.util.List;

@Singleton
@Replaces(DefaultProtectedResourceMetadataProvider.class)
@Requires(property = "akhq.security.mcp-oauth.enabled", value = "true")
public class McpOauthProtectedResourceMetadataProvider implements ProtectedResourceMetadataProvider<HttpRequest<?>> {
    private final McpOauth mcpOauth;
    private final McpOauthResourceMetadata resourceMetadata;

    public McpOauthProtectedResourceMetadataProvider(McpOauth mcpOauth, McpOauthResourceMetadata resourceMetadata) {
        this.mcpOauth = mcpOauth;
        this.resourceMetadata = resourceMetadata;
    }

    @Override
    public ProtectedResourceMetadata get(HttpRequest<?> request) {
        return metadata(request);
    }

    @Override
    public ProtectedResourceMetadata get(String path, HttpRequest<?> request) {
        return metadata(request);
    }

    private ProtectedResourceMetadata metadata(HttpRequest<?> request) {
        ProtectedResourceMetadata.Builder builder = ProtectedResourceMetadata.builder()
            .resource(resourceMetadata.resource(request))
            .authorizationServer(mcpOauth.getAuthorizationServer())
            .bearerMethodsSupported(List.of("header"));
        if (mcpOauth.getRequiredScope() != null && !mcpOauth.getRequiredScope().isBlank()) {
            builder.scopesSupported(List.of(mcpOauth.getRequiredScope()));
        }
        return builder.build();
    }
}

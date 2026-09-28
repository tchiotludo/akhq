package org.akhq.security.authentication.mcp;

import org.akhq.configs.security.McpOauth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpOauthConfigurationValidatorTest {
    @Test
    void acceptsCompleteConfiguration() {
        assertDoesNotThrow(() -> new McpOauthConfigurationValidator(configuration(), true).validate());
    }

    @Test
    void rejectsMissingRequiredProperties() {
        McpOauth configuration = configuration();
        configuration.setIssuer(null);
        configuration.setAudience("  ");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new McpOauthConfigurationValidator(configuration, true).validate()
        );

        assertTrue(exception.getMessage().contains("`issuer` is required"), exception.getMessage());
        assertTrue(exception.getMessage().contains("`audience` is required"), exception.getMessage());
        // The authorization server defaults to the issuer, so it is reported as missing too.
        assertTrue(exception.getMessage().contains("`authorization-server` is required"), exception.getMessage());
    }

    @Test
    void rejectsDisabledSecurity() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new McpOauthConfigurationValidator(configuration(), false).validate()
        );

        assertTrue(exception.getMessage().contains("`micronaut.security.enabled` must be true"), exception.getMessage());
    }

    @Test
    void rejectsRelativeUrls() {
        McpOauth configuration = configuration();
        configuration.setResource("/mcp");

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> new McpOauthConfigurationValidator(configuration, true).validate()
        );

        assertTrue(exception.getMessage().contains("`resource` must be an absolute URL"), exception.getMessage());
    }

    @Test
    void defaultsAuthorizationServerToIssuer() {
        McpOauth configuration = configuration();

        assertEquals("https://identity.example.com/realms/akhq", configuration.getAuthorizationServer());
    }

    private McpOauth configuration() {
        McpOauth configuration = new McpOauth();
        configuration.setEnabled(true);
        configuration.setIssuer("https://identity.example.com/realms/akhq");
        configuration.setJwksUrl("https://identity.example.com/realms/akhq/protocol/openid-connect/certs");
        configuration.setAudience("akhq-mcp");
        configuration.setResource("https://akhq.example.com/mcp");
        return configuration;
    }
}

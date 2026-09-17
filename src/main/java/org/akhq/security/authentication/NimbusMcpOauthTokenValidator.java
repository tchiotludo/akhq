package org.akhq.security.authentication;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSAlgorithmFamilyJWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

import java.net.URL;
import java.util.Set;

@Singleton
public class NimbusMcpOauthTokenValidator implements McpOauthTokenValidator {
    private final McpOauth mcpOauth;
    private JWTProcessor<SecurityContext> jwtProcessor;

    public NimbusMcpOauthTokenValidator(McpOauth mcpOauth) {
        this.mcpOauth = mcpOauth;
    }

    @PostConstruct
    void init() throws Exception {
        if (isBlank(mcpOauth.getIssuer()) || isBlank(mcpOauth.getJwksUrl()) || isBlank(mcpOauth.getAudience())) {
            throw new IllegalStateException(
                "`akhq.security.mcp-oauth.issuer`, `jwks-url`, and `audience` are required when MCP OAuth is enabled"
            );
        }

        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        RemoteJWKSet<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(mcpOauth.getJwksUrl()));
        processor.setJWSKeySelector(new JWSAlgorithmFamilyJWSKeySelector<>(JWSAlgorithm.Family.SIGNATURE, jwkSource));
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<SecurityContext>(
            mcpOauth.getAudience(),
            new JWTClaimsSet.Builder().issuer(mcpOauth.getIssuer()).build(),
            Set.of("exp", "sub")
        ));
        this.jwtProcessor = processor;
    }

    @Override
    public JWTClaimsSet validate(String token) throws Exception {
        return jwtProcessor.process(token, null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

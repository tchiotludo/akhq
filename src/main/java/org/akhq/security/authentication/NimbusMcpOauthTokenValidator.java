package org.akhq.security.authentication;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Requires(property = "akhq.security.mcp-oauth.enabled", value = "true")
public class NimbusMcpOauthTokenValidator implements McpOauthTokenValidator {
    private final McpOauth mcpOauth;
    private JWTProcessor<SecurityContext> jwtProcessor;

    public NimbusMcpOauthTokenValidator(McpOauth mcpOauth) {
        this.mcpOauth = mcpOauth;
    }

    @PostConstruct
    void init() throws Exception {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(keySelector(jwkSource()));
        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
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

    private JWKSource<SecurityContext> jwkSource() throws Exception {
        DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever(
            (int) mcpOauth.getJwksConnectTimeout().toMillis(),
            (int) mcpOauth.getJwksReadTimeout().toMillis()
        );

        return JWKSourceBuilder.create(URI.create(mcpOauth.getJwksUrl()).toURL(), resourceRetriever)
            .retrying(true)
            .build();
    }

    /**
     * Restricts accepted signatures to the configured algorithms, or to asymmetric ones when none is configured,
     * so that a symmetric key published on the JWKS endpoint cannot be used to sign tokens.
     */
    private JWSVerificationKeySelector<SecurityContext> keySelector(JWKSource<SecurityContext> jwkSource) {
        Set<JWSAlgorithm> algorithms = mcpOauth.getJwsAlgorithms().stream()
            .map(JWSAlgorithm::parse)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (algorithms.isEmpty()) {
            algorithms.addAll(JWSAlgorithm.Family.RSA);
            algorithms.addAll(JWSAlgorithm.Family.EC);
            algorithms.addAll(JWSAlgorithm.Family.ED);
        }

        return new JWSVerificationKeySelector<>(algorithms, jwkSource);
    }
}

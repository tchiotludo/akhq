package org.akhq.security.authentication.mcp;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.security.McpOauth;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Requires(property = "akhq.security.mcp-oauth.enabled", value = "true")
public class NimbusMcpOauthTokenValidator implements McpOauthTokenValidator {
    /**
     * Accepted {@code typ} headers: plain JWTs (Keycloak, Entra ID), RFC 9068 access tokens in both their short and
     * media type forms, and tokens without {@code typ}.
     */
    private static final DefaultJOSEObjectTypeVerifier<SecurityContext> TYPE_VERIFIER = new DefaultJOSEObjectTypeVerifier<>(
        JOSEObjectType.JWT,
        new JOSEObjectType("at+jwt"),
        new JOSEObjectType("application/at+jwt"),
        null
    );

    private final JWTProcessor<SecurityContext> jwtProcessor;

    @Inject
    public NimbusMcpOauthTokenValidator(McpOauth mcpOauth) {
        this(mcpOauth, remoteJwkSource(mcpOauth));
    }

    NimbusMcpOauthTokenValidator(McpOauth mcpOauth, JWKSource<SecurityContext> jwkSource) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSTypeVerifier(TYPE_VERIFIER);
        processor.setJWSKeySelector(keySelector(mcpOauth, jwkSource));
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

    private static JWKSource<SecurityContext> remoteJwkSource(McpOauth mcpOauth) {
        DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever(
            (int) mcpOauth.getJwksConnectTimeout().toMillis(),
            (int) mcpOauth.getJwksReadTimeout().toMillis()
        );

        try {
            return JWKSourceBuilder.create(URI.create(mcpOauth.getJwksUrl()).toURL(), resourceRetriever)
                .retrying(true)
                .build();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid `akhq.security.mcp-oauth.jwks-url`: " + mcpOauth.getJwksUrl(), e);
        }
    }

    /**
     * Restricts accepted signatures to the configured algorithms, or to asymmetric ones when none is configured,
     * so that a symmetric key published on the JWKS endpoint cannot be used to sign tokens.
     */
    private static JWSVerificationKeySelector<SecurityContext> keySelector(McpOauth mcpOauth, JWKSource<SecurityContext> jwkSource) {
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

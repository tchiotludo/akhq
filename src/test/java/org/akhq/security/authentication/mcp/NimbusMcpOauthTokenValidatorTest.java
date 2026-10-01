package org.akhq.security.authentication.mcp;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.akhq.configs.security.McpOauth;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises the token validation with real signed tokens against an in-memory JWK set.
 */
class NimbusMcpOauthTokenValidatorTest {
    private static final String ISSUER = "https://identity.example.com/realms/akhq";
    private static final String AUDIENCE = "akhq-mcp";

    private static RSAKey signingKey;
    private static OctetSequenceKey symmetricKey;
    private static NimbusMcpOauthTokenValidator validator;

    @BeforeAll
    static void setUp() throws Exception {
        signingKey = new RSAKeyGenerator(2048).keyID("rsa-key").generate();
        // A symmetric key published on the JWKS endpoint must never be usable to forge tokens.
        symmetricKey = new OctetSequenceKeyGenerator(256).keyID("hmac-key").generate();

        McpOauth configuration = new McpOauth();
        configuration.setIssuer(ISSUER);
        configuration.setAudience(AUDIENCE);

        JWKSet jwkSet = new JWKSet(List.of(signingKey.toPublicJWK(), symmetricKey));
        validator = new NimbusMcpOauthTokenValidator(configuration, new ImmutableJWKSet<SecurityContext>(jwkSet));
    }

    @Test
    void acceptsPlainJwtAccessTokens() throws Exception {
        assertEquals("alice", validator.validate(rsaToken(JOSEObjectType.JWT, claims -> {})).getSubject());
    }

    @Test
    void acceptsRfc9068AccessTokens() throws Exception {
        assertEquals("alice", validator.validate(rsaToken(new JOSEObjectType("at+jwt"), claims -> {})).getSubject());
        assertEquals("alice", validator.validate(rsaToken(new JOSEObjectType("application/at+jwt"), claims -> {})).getSubject());
    }

    @Test
    void acceptsTokensWithoutType() throws Exception {
        assertEquals("alice", validator.validate(rsaToken(null, claims -> {})).getSubject());
    }

    @Test
    void rejectsUnexpectedTokenTypes() {
        assertThrows(Exception.class, () -> validator.validate(rsaToken(new JOSEObjectType("logout+jwt"), claims -> {})));
    }

    @Test
    void rejectsAnotherAudience() {
        assertThrows(Exception.class, () -> validator.validate(rsaToken(JOSEObjectType.JWT, claims -> claims.audience("another-app"))));
    }

    @Test
    void rejectsAnotherIssuer() {
        assertThrows(Exception.class, () -> validator.validate(rsaToken(JOSEObjectType.JWT, claims -> claims.issuer("https://evil.example.com"))));
    }

    @Test
    void rejectsExpiredTokens() {
        assertThrows(Exception.class, () -> validator.validate(rsaToken(
            JOSEObjectType.JWT,
            claims -> claims.expirationTime(Date.from(Instant.now().minusSeconds(3600)))
        )));
    }

    @Test
    void rejectsTokensWithoutSubject() {
        assertThrows(Exception.class, () -> validator.validate(rsaToken(JOSEObjectType.JWT, claims -> claims.subject(null))));
    }

    @Test
    void rejectsSymmetricSignaturesEvenWithAPublishedKey() throws Exception {
        String token = sign(new MACSigner(symmetricKey), JWSAlgorithm.HS256, symmetricKey.getKeyID(), JOSEObjectType.JWT, claims -> {});

        assertThrows(Exception.class, () -> validator.validate(token));
    }

    @Test
    void rejectsTokensSignedByAnUnknownKey() throws Exception {
        RSAKey unknownKey = new RSAKeyGenerator(2048).keyID("rsa-key").generate();
        String token = sign(new RSASSASigner(unknownKey), JWSAlgorithm.RS256, unknownKey.getKeyID(), JOSEObjectType.JWT, claims -> {});

        assertThrows(Exception.class, () -> validator.validate(token));
    }

    private static String rsaToken(JOSEObjectType type, Consumer<JWTClaimsSet.Builder> customizer) throws Exception {
        return sign(new RSASSASigner(signingKey), JWSAlgorithm.RS256, signingKey.getKeyID(), type, customizer);
    }

    private static String sign(
        JWSSigner signer,
        JWSAlgorithm algorithm,
        String keyId,
        JOSEObjectType type,
        Consumer<JWTClaimsSet.Builder> customizer
    ) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
            .issuer(ISSUER)
            .audience(AUDIENCE)
            .subject("alice")
            .expirationTime(Date.from(Instant.now().plusSeconds(300)));
        customizer.accept(claims);

        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(algorithm).keyID(keyId).type(type).build(), claims.build());
        jwt.sign(signer);
        return jwt.serialize();
    }
}

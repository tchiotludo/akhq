package org.akhq.security.claim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import io.jsonwebtoken.impl.compression.GzipCompressionAlgorithm;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.runtime.ApplicationConfiguration;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.claims.ClaimsAudienceProvider;
import io.micronaut.security.token.claims.JtiGenerator;
import io.micronaut.security.token.config.TokenConfiguration;
import io.micronaut.security.token.jwt.generator.claims.JWTClaimsSetGenerator;
import jakarta.inject.Singleton;

import java.util.Base64;

@Singleton
@Replaces(JWTClaimsSetGenerator.class)
public class CustomClaimsGenerator extends JWTClaimsSetGenerator {
    private final GzipCompressionAlgorithm gzip = new GzipCompressionAlgorithm();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param tokenConfiguration       Token Configuration
     * @param jwtIdGenerator           Generator which creates unique JWT ID
     * @param claimsAudienceProvider   Provider which identifies the recipients that the JWT is intended for.
     * @param applicationConfiguration The application configuration
     */
    public CustomClaimsGenerator(TokenConfiguration tokenConfiguration, @Nullable JtiGenerator jwtIdGenerator, @Nullable ClaimsAudienceProvider claimsAudienceProvider, @Nullable ApplicationConfiguration applicationConfiguration) {
        super(tokenConfiguration, jwtIdGenerator, claimsAudienceProvider, applicationConfiguration);
    }

    protected void populateWithAuthentication(JWTClaimsSet.Builder builder, Authentication authentication) {
        super.populateWithAuthentication(builder, authentication);

        try {
            String plainGroups = mapper.writeValueAsString(builder.getClaims().get("groups"));
            builder.claim("groups", Base64.getEncoder().encodeToString(gzip.compress(plainGroups.getBytes())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to build the JWT token, groups format is incorrect");
        }
    }
}

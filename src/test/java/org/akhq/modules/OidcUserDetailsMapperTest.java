package org.akhq.modules;

import com.nimbusds.jwt.JWTClaimsSet;
import io.micronaut.security.authentication.AuthenticationMode;
import io.micronaut.security.config.AuthenticationModeConfiguration;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.JWTOpenIdClaims;
import org.akhq.configs.Oidc;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.List;

class OidcUserDetailsMapperTest {
    private static final String NESTED_GROUPS_AND_USERNAME_PAYLOAD = "{\n" +
        "  \"exp\": 1672716500,\n" +
        "  \"iat\": 1672714700,\n" +
        "  \"auth_time\": 1672714691,\n" +
        "  \"jti\": \"771921ac-378c-4911-98c2-110482cbe62c\",\n" +
        "  \"iss\": \"http://localhost:8903/realms/ttcntt\",\n" +
        "  \"aud\": \"account\",\n" +
        "  \"sub\": \"36eb7ab9-6b1c-4350-b59b-dc0c16e288cb\",\n" +
        "  \"typ\": \"Bearer\",\n" +
        "  \"azp\": \"isohoa\",\n" +
        "  \"nonce\": \"d2033cqtyrs\",\n" +
        "  \"session_state\": \"246bbcf0-e301-4bb4-97e9-4ccc5b7e202d\",\n" +
        "  \"acr\": \"1\",\n" +
        "  \"realm_access\": {\n" +
        "    \"roles\": [\n" +
        "      \"default-roles-ttcntt\",\n" +
        "      \"offline_access\",\n" +
        "      \"uma_authorization\",\n" +
        "      \"clerical\"\n" +
        "    ]\n" +
        "  },\n" +
        "  \"resource_access\": {\n" +
        "    \"account\": {\n" +
        "      \"roles\": [\n" +
        "        \"manage-account\",\n" +
        "        \"manage-account-links\",\n" +
        "        \"view-profile\"\n" +
        "      ]\n" +
        "    }\n" +
        "  },\n" +
        "  \"scope\": \"email profile\",\n" +
        "  \"sid\": \"246bbcf0-e301-4bb4-97e9-4ccc5b7e202d\",\n" +
        "  \"email_verified\": true,\n" +
        "  \"name\": \"C 1\",\n" +
        "  \"username\": {\n" +
        "    \"preferred_username\": \"clerical1\"\n" +
        "  },\n" +
        "  \"given_name\": \"C\",\n" +
        "  \"family_name\": \"1\",\n" +
        "  \"email\": \"clerical1@sohoa.asia\"\n" +
        "}";

    private static final String NON_NESTED_GROUPS_AND_USERNAME_PAYLOAD = "{\n" +
        "  \"exp\": 1672716500,\n" +
        "  \"iat\": 1672714700,\n" +
        "  \"auth_time\": 1672714691,\n" +
        "  \"jti\": \"771921ac-378c-4911-98c2-110482cbe62c\",\n" +
        "  \"iss\": \"http://localhost:8903/realms/ttcntt\",\n" +
        "  \"aud\": \"account\",\n" +
        "  \"sub\": \"36eb7ab9-6b1c-4350-b59b-dc0c16e288cb\",\n" +
        "  \"typ\": \"Bearer\",\n" +
        "  \"azp\": \"isohoa\",\n" +
        "  \"nonce\": \"d2033cqtyrs\",\n" +
        "  \"session_state\": \"246bbcf0-e301-4bb4-97e9-4ccc5b7e202d\",\n" +
        "  \"acr\": \"1\",\n" +
        "  \"roles\": [\n" +
        "    \"default-roles-ttcntt\",\n" +
        "    \"offline_access\",\n" +
        "    \"uma_authorization\",\n" +
        "    \"clerical\"\n" +
        "  ],\n" +
        "  \"resource_access\": {\n" +
        "    \"account\": {\n" +
        "      \"roles\": [\n" +
        "        \"manage-account\",\n" +
        "        \"manage-account-links\",\n" +
        "        \"view-profile\"\n" +
        "      ]\n" +
        "    }\n" +
        "  },\n" +
        "  \"scope\": \"email profile\",\n" +
        "  \"sid\": \"246bbcf0-e301-4bb4-97e9-4ccc5b7e202d\",\n" +
        "  \"email_verified\": true,\n" +
        "  \"name\": \"C 1\",\n" +
        "  \"preferred_username\": \"clerical1\",\n" +
        "  \"given_name\": \"C\",\n" +
        "  \"family_name\": \"1\",\n" +
        "  \"email\": \"clerical1@sohoa.asia\"\n" +
        "}";

    @Test
    void givenNestedGroupsAndUsername_whenGetGroupsAndUsername_thenSuccess() throws ParseException {
        OidcUserDetailsMapper mapper = createMapper();
        JWTClaimsSet jwtClaimsSet = JWTClaimsSet.parse(NESTED_GROUPS_AND_USERNAME_PAYLOAD);
        JWTOpenIdClaims jwtOpenIdClaims = new JWTOpenIdClaims(jwtClaimsSet);

        Oidc.Provider provider = new Oidc.Provider();
        provider.setGroupsField("realm_access.roles");
        provider.setUsernameField("username.preferred_username");

        List<String> oidcGroups = mapper.getOidcGroups(provider, jwtOpenIdClaims);
        Assertions.assertEquals(4, oidcGroups.size());
        Assertions.assertEquals("default-roles-ttcntt", oidcGroups.get(0));
        Assertions.assertEquals("offline_access", oidcGroups.get(1));
        Assertions.assertEquals("uma_authorization", oidcGroups.get(2));
        Assertions.assertEquals("clerical", oidcGroups.get(3));

        String username = mapper.getUsername(provider, jwtOpenIdClaims);
        Assertions.assertEquals("clerical1", username);
    }

    @Test
    void givenNonNestedGroupsAndUsername_whenGetGroupsAndUsername_thenSuccess() throws ParseException {
        OidcUserDetailsMapper mapper = createMapper();
        JWTClaimsSet jwtClaimsSet = JWTClaimsSet.parse(NON_NESTED_GROUPS_AND_USERNAME_PAYLOAD);
        JWTOpenIdClaims jwtOpenIdClaims = new JWTOpenIdClaims(jwtClaimsSet);

        Oidc.Provider provider = new Oidc.Provider();

        List<String> oidcGroups = mapper.getOidcGroups(provider, jwtOpenIdClaims);
        Assertions.assertEquals(4, oidcGroups.size());
        Assertions.assertEquals("default-roles-ttcntt", oidcGroups.get(0));
        Assertions.assertEquals("offline_access", oidcGroups.get(1));
        Assertions.assertEquals("uma_authorization", oidcGroups.get(2));
        Assertions.assertEquals("clerical", oidcGroups.get(3));

        String username = mapper.getUsername(provider, jwtOpenIdClaims);
        Assertions.assertEquals("clerical1", username);
    }


    private OidcUserDetailsMapper createMapper() {
        OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration =
            new OpenIdAdditionalClaimsConfiguration() {
                @Override
                public boolean isJwt() {
                    return true;
                }

                @Override
                public boolean isAccessToken() {
                    return true;
                }

                @Override
                public boolean isRefreshToken() {
                    return true;
                }
            };

        AuthenticationModeConfiguration authenticationModeConfiguration = () -> AuthenticationMode.BEARER;

        return new OidcUserDetailsMapper(openIdAdditionalClaimsConfiguration, authenticationModeConfiguration);
    }
}

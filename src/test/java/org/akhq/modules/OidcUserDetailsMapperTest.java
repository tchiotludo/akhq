package org.akhq.modules;

import com.nimbusds.jwt.JWTClaimsSet;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.JWTOpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.akhq.configs.Oidc;
import org.akhq.utils.UserGroupUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcUserDetailsMapperTest {
    private static final String CLAIM_ROLES = "roles";

    private static final String USERNAME = "example";
    private static final String ROLE_1 = "role1";
    private static final String ROLE_2 = "role2";
    private static final String DEFAULT_ROLE = "test";
    private static final String TRANSLATED_PREFIX = "translated/";
    private static final String PROVIDER_NAME = "sample";

    @Mock
    private Oidc oidc;
    @Mock
    private UserGroupUtils userGroupUtils;
    @Mock
    private OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration;

    @InjectMocks
    private OidcUserDetailsMapper subject;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        FieldUtils.writeField(subject, "defaultGroups", DEFAULT_ROLE, true);

        when(openIdAdditionalClaimsConfiguration.isAccessToken()).thenReturn(false);
        when(openIdAdditionalClaimsConfiguration.isJwt()).thenReturn(false);
        when(openIdAdditionalClaimsConfiguration.isRefreshToken()).thenReturn(false);

        Oidc.Provider provider = new Oidc.Provider();
        when(oidc.getProvider(PROVIDER_NAME)).thenReturn(provider);
    }

    @Test
    void createUserDetails() {
        stubRoles();
        JWTOpenIdClaims claims = buildClaims(Arrays.asList(ROLE_1, ROLE_2));
        UserDetails userDetails = subject.createUserDetails(PROVIDER_NAME, new OpenIdTokenResponse(), claims);
        assertEquals(userDetails.getUsername(), USERNAME);
        assertEquals(userDetails.getRoles(), Arrays.asList("translated/role1", "translated/role2"));
        Map<String, Object> attributes = userDetails.getAttributes("roles", "username");
        assertEquals(attributes.get("role1"), true);
        assertEquals(attributes.get("role2"), true);
    }

    @Test
    void fieldMissing() {
        stubDefaultRole();
        JWTOpenIdClaims claims = buildClaims(null);
        UserDetails userDetails = subject.createUserDetails(PROVIDER_NAME, new OpenIdTokenResponse(), claims);
        assertDefaultRole(userDetails);
    }

    @Test
    void fieldIncompatible() {
        stubDefaultRole();
        JWTOpenIdClaims claims = buildClaims(ROLE_1);
        UserDetails userDetails = subject.createUserDetails(PROVIDER_NAME, new OpenIdTokenResponse(), claims);
        assertDefaultRole(userDetails);
    }

    private void stubRoles() {
        when(userGroupUtils.getUserAttributes(Arrays.asList(ROLE_1, ROLE_2))).thenReturn(Map.of(ROLE_1, true, ROLE_2, true));
        when(userGroupUtils.getUserRoles(Arrays.asList(ROLE_1, ROLE_2)))
                .thenReturn(Arrays.asList(TRANSLATED_PREFIX + ROLE_1, TRANSLATED_PREFIX + ROLE_2));
    }

    private void stubDefaultRole() {
        when(userGroupUtils.getUserAttributes(Collections.singletonList(DEFAULT_ROLE))).thenReturn(Map.of(DEFAULT_ROLE, true));
        when(userGroupUtils.getUserRoles(Collections.singletonList(DEFAULT_ROLE)))
                .thenReturn(Collections.singletonList(TRANSLATED_PREFIX + DEFAULT_ROLE));
    }

    private void assertDefaultRole(UserDetails userDetails) {
        assertEquals(userDetails.getRoles(), Collections.singletonList("translated/test"));
        Map<String, Object> attributes = userDetails.getAttributes("roles", "username");
        assertEquals(attributes.get("test"), true);
    }

    private JWTOpenIdClaims buildClaims(Object roles) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, USERNAME)
                .claim(CLAIM_ROLES, roles)
                .build();
        return new JWTOpenIdClaims(claimsSet);
    }
}

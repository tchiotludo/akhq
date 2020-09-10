package org.akhq.modules;

import com.nimbusds.jwt.JWTClaimsSet;
import io.micronaut.security.authentication.AuthenticationMode;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.config.AuthenticationModeConfiguration;
import io.micronaut.security.oauth2.configuration.OpenIdAdditionalClaimsConfiguration;
import io.micronaut.security.oauth2.endpoint.token.response.JWTOpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdClaims;
import io.micronaut.security.oauth2.endpoint.token.response.OpenIdTokenResponse;
import org.akhq.configs.GroupMapping;
import org.akhq.configs.Oidc;
import org.akhq.configs.UserMapping;
import org.akhq.utils.UserGroupUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcUserDetailsMapperTest {
    private static final String CLAIM_ROLES = "roles";

    private static final String USERNAME = "example";
    private static final String GROUP_1 = "role1";
    private static final String GROUP_2 = "role2";
    private static final String USER_GROUP = "user-group";
    private static final String DEFAULT_GROUP = "test";
    private static final String INTERNAL_PREFIX = "internal/";
    private static final String TRANSLATED_PREFIX = "translated/";
    private static final String PROVIDER_NAME = "sample";

    @Mock
    private Oidc oidc;
    @Mock
    private UserGroupUtils userGroupUtils;
    @Mock
    private OpenIdAdditionalClaimsConfiguration openIdAdditionalClaimsConfiguration;
    @Mock
    private AuthenticationModeConfiguration authenticationModeConfiguration;

    @InjectMocks
    private OidcUserDetailsMapper subject;

    @BeforeEach
    void setUp() {
        when(openIdAdditionalClaimsConfiguration.isAccessToken()).thenReturn(false);
        when(openIdAdditionalClaimsConfiguration.isJwt()).thenReturn(false);
        when(openIdAdditionalClaimsConfiguration.isRefreshToken()).thenReturn(false);
        when(authenticationModeConfiguration.getAuthentication()).thenReturn(AuthenticationMode.COOKIE);

        when(userGroupUtils.getUserRoles(anyList())).thenAnswer((Answer<List<String>>) invocation -> {
            List<String> input = (List<String>) invocation.getArgument(0, List.class);
            return input.stream().map(g -> TRANSLATED_PREFIX + g).collect(Collectors.toList());
        });
        when(userGroupUtils.getUserAttributes(anyList())).thenAnswer((Answer<Map<String, Object>>) invocation -> {
            List<String> input = (List<String>) invocation.getArgument(0, List.class);
            final Map<String, Object> attributes = new HashMap<>();
            input.forEach(g -> attributes.put(g, true));
            return attributes;
        });

        Oidc.Provider provider = new Oidc.Provider();
        provider.setGroups(
                Arrays.asList(
                        buildGroupMapping(GROUP_1, INTERNAL_PREFIX + GROUP_1),
                        buildGroupMapping(GROUP_2, INTERNAL_PREFIX + GROUP_2)
                )
        );
        provider.setUsers(Collections.singletonList(buildUserMapping(USERNAME, "user-group")));
        provider.setDefaultGroup(DEFAULT_GROUP);
        when(oidc.getProvider(PROVIDER_NAME)).thenReturn(provider);
    }

    @Test
    void createUserDetails() {
        JWTOpenIdClaims claims = buildClaims(Arrays.asList(GROUP_1, GROUP_2));
        UserDetails userDetails = subject.createUserDetails(PROVIDER_NAME, new OpenIdTokenResponse(), claims);
        assertEquals(USERNAME, userDetails.getUsername());
        assertContainsInAnyOrder(
                Arrays.asList(
                        "translated/user-group",
                        "translated/internal/role1",
                        "translated/internal/role2",
                        "translated/test"
                ),
                userDetails.getRoles()
        );
        Map<String, Object> attributes = userDetails.getAttributes("roles", "username");
        assertEquals(true, attributes.get("internal/role1"));
        assertEquals(true, attributes.get("internal/role2"));
        assertEquals(true, attributes.get("user-group"));
        assertEquals(true, attributes.get("test"));
    }

    @Test
    void fieldMissing() {
        JWTOpenIdClaims claims = buildClaims(null);
        UserDetails userDetails = subject.createUserDetails(PROVIDER_NAME, new OpenIdTokenResponse(), claims);
        assertDefaultRole(userDetails);
    }

    @Test
    void fieldIncompatible() {
        JWTOpenIdClaims claims = buildClaims(GROUP_1);
        UserDetails userDetails = subject.createUserDetails(PROVIDER_NAME, new OpenIdTokenResponse(), claims);
        assertDefaultRole(userDetails);
    }

    private GroupMapping buildGroupMapping(String name, String mapped) {
        GroupMapping groupMapping = new GroupMapping();
        groupMapping.setName(name);
        groupMapping.setGroups(Collections.singletonList(mapped));
        return groupMapping;
    }

    private UserMapping buildUserMapping(String name, String group) {
        UserMapping userMapping = new UserMapping();
        userMapping.setUsername(name);
        userMapping.setGroups(Collections.singletonList(group));
        return userMapping;
    }

    private void assertDefaultRole(UserDetails userDetails) {
        assertContainsInAnyOrder(Arrays.asList("translated/user-group", "translated/test"), userDetails.getRoles());
        Map<String, Object> attributes = userDetails.getAttributes("roles", "username");
        assertEquals(true, attributes.get("user-group"));
        assertEquals(true, attributes.get("test"));
    }

    private <T> void assertContainsInAnyOrder(Collection<T> expected, Collection<T> actual) {
        Set<T> expectedSet = new HashSet<>(expected);
        Set<T> actualSet = new HashSet<>(actual);
        assertEquals(expectedSet, actualSet);
    }

    private JWTOpenIdClaims buildClaims(Object roles) {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(OpenIdClaims.CLAIMS_PREFERRED_USERNAME, USERNAME)
                .claim(CLAIM_ROLES, roles)
                .build();
        return new JWTOpenIdClaims(claimsSet);
    }
}

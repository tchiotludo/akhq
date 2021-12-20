package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.ldap.ContextAuthenticationMapper;
import io.micronaut.security.ldap.DefaultContextAuthenticationMapper;
import org.akhq.utils.ClaimRequest;
import org.akhq.utils.ClaimResponse;
import org.akhq.utils.ClaimProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.utils.ClaimProviderType;

import java.util.List;
import java.util.Set;

@Singleton
@Replaces(DefaultContextAuthenticationMapper.class)
public class LdapContextAuthenticationMapper implements ContextAuthenticationMapper {
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {
        ClaimRequest request = ClaimRequest.builder()
                .providerType(ClaimProviderType.LDAP)
                .providerName(null)
                .username(username)
                .groups(List.copyOf(groups))
                .build();
        try {
            ClaimResponse claim = claimProvider.generateClaim(request);
            return AuthenticationResponse.success(username, claim.getRoles(), claim.getAttributes());
        } catch (Exception e) {
            String claimProviderClass = claimProvider.getClass().getName();
            return new AuthenticationFailed("Exception from ClaimProvider " + claimProviderClass + ": " + e.getMessage());
        }
    }
}

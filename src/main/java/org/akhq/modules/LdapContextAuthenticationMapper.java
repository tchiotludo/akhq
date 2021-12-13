package org.akhq.modules;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.convert.value.ConvertibleValues;
import io.micronaut.security.authentication.AuthenticationFailed;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.ldap.ContextAuthenticationMapper;
import io.micronaut.security.ldap.DefaultContextAuthenticationMapper;
import org.akhq.utils.ClaimProvider;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Set;

@Singleton
@Replaces(DefaultContextAuthenticationMapper.class)
public class LdapContextAuthenticationMapper implements ContextAuthenticationMapper {
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public AuthenticationResponse map(ConvertibleValues<Object> attributes, String username, Set<String> groups) {
        ClaimProvider.AKHQClaimRequest request = ClaimProvider.AKHQClaimRequest.builder()
                .providerType(ClaimProvider.ProviderType.LDAP)
                .providerName(null)
                .username(username)
                .groups(List.copyOf(groups))
                .build();
        try {
            ClaimProvider.AKHQClaimResponse claim = claimProvider.generateClaim(request);
            return AuthenticationResponse.success(username, claim.getRoles(), claim.getAttributes());
        } catch (Exception e) {
            String claimProviderClass = claimProvider.getClass().getName();
            return new AuthenticationFailed("Exception from ClaimProvider " + claimProviderClass + ": " + e.getMessage());
        }
    }
}

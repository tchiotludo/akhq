package org.akhq.utils;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public interface ClaimProvider {
    /**
     * Provide an interface to any mechanism that uses the Authentication Response from LDAP, OIDC or BasicAuth
     * to transform username and groups into AKHQ {@link org.akhq.configs.Role} and Attributes
     * @param providerType either BASIC_AUTH, LDAP or OIDC
     * @param providerName required for OIDC, unused for BASIC and LDAP providerType
     * @param username provided by the Authentication mechanism
     * @param groups provided by the Authentication mechanism (LDAP, OIDC, BasicAuth)
     * @return AKHQClaimResponse with AKHQ Roles and Attributes
     */
    AKHQClaimResponse generateClaim(ProviderType providerType, String providerName, String username, List<String> groups);

    enum ProviderType {
        BASIC_AUTH,
        LDAP,
        OIDC
    }

    @Introspected
    @Builder
    @Getter
    @Setter
    class AKHQClaimResponse {
        private List<String> roles;
        private List<String> topicsFilterRegexp;
        private List<String> connectsFilterRegexp;
        private List<String> consumerGroupsFilterRegexp;

    }

}

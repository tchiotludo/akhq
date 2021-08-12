package org.akhq.utils;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

public interface ClaimProvider {

    AKHQClaimResponse generateClaim(AKHQClaimRequest request);

    enum ProviderType {
        HEADER,
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
        private Map<String,Object> attributes;
    }

    @Introspected
    @Builder
    @Getter
    @Setter
    class AKHQClaimRequest{
        ProviderType providerType;
        String providerName;
        String username;
        List<String> groups;
    }

}

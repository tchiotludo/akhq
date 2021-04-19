package org.akhq.utils;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Primary
@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
public class RestApiClaimProvider implements ClaimProvider {

    @Inject
    RestApiClaimProviderClient client;

    @Override
    public AKHQClaimResponse generateClaim(AKHQClaimRequest request) {
        return client.post(request);
    }
}

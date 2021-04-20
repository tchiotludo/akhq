package org.akhq.utils;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

@Primary
@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Client("${akhq.security.rest.url}")
public interface RestApiClaimProvider extends ClaimProvider {

    @Post
    @Override
    AKHQClaimResponse generateClaim(@Body AKHQClaimRequest request);
}

package org.akhq.utils;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;

import java.util.List;

@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Client("${akhq.security.rest.url}")
public interface RestApiClaimProviderClient {

    @Post
    ClaimProvider.AKHQClaimResponse post(ClaimProvider.AKHQClaimRequest request);
}

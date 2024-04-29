package org.akhq.security.claim;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;

@Primary
@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Client("${akhq.security.rest.url}")
@ExecuteOn(TaskExecutors.BLOCKING)
@Header(name = "${akhq.security.rest.token-header.name}", value = "${akhq.security.rest.token-header.value}")
public interface RestApiClaimProvider extends ClaimProvider {
    @Post
    @Override
    ClaimResponse generateClaim(@Body ClaimRequest request);
}
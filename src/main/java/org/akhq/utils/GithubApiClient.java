package org.akhq.utils;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.client.annotation.Client;
import org.akhq.models.GithubClaims;
import org.reactivestreams.Publisher;

@Header(name = "User-Agent", value = "Micronaut")
@Header(name = "Accept", value = "application/vnd.github.v3+json, application/json")
@Client("https://api.github.com")
public interface GithubApiClient {

    @Get("/user")
    Publisher<GithubClaims> getUser(@Header("Authorization") String authorization);
}
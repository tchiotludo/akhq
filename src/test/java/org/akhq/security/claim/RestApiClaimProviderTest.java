package org.akhq.security.claim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import io.jsonwebtoken.impl.compression.GzipCompressionAlgorithm;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.rxjava2.http.client.RxHttpClient;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.Group;
import org.akhq.middlewares.KafkaWrapperFilter;
import org.akhq.models.security.ClaimRequest;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.security.PermitAll;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(environments = "rest-api")
@Slf4j
public class RestApiClaimProviderTest {

    @Inject
    @Client("/")
    protected RxHttpClient client;

    @Test
    void loginExternalClaim() throws ParseException, JsonProcessingException {
        HttpResponse<?> resultResponse = client.toBlocking().exchange(
            HttpRequest.POST("/login", new UsernamePasswordCredentials("admin", "pass"))
        );

        assertTrue(resultResponse.getCookie("JWT").isPresent());

        String jwtCookie = resultResponse.getCookie("JWT").get().getValue();
        JWT token = JWTParser.parse(jwtCookie);

        assertTrue(token.getJWTClaimsSet().getClaims().containsKey("groups"));

        var gzip = new GzipCompressionAlgorithm();
        String compressedGroups = new String(gzip.decompress(
            Base64.getDecoder().decode((String) token.getJWTClaimsSet().getClaim("groups"))));

        Map<String, List<Group>> groups = new ObjectMapper().readValue(compressedGroups, Map.class);
        assertThat(groups.keySet(), hasSize(1));
        assertNotNull(groups.get("limited"));

        Group limited = new ObjectMapper().convertValue(groups.get("limited").get(0), Group.class);
        assertThat(limited.getRole(), is("topic-read"));
        assertThat(limited.getPatterns(), containsInAnyOrder("user.*"));
        assertThat(limited.getClusters(), containsInAnyOrder("pub.*"));
    }

    @Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
    @Filter("/**")
    @Replaces(KafkaWrapperFilter.class)
    static class IgnoreKafkaWrapperFilter implements HttpServerFilter {

        @Override
        public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
            return chain.proceed(request);
        }
    }

    @Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
    @PermitAll
    @Controller("/external-mock")
    static class RestApiExternalService {
        @Post
        String generateClaim(@Body ClaimRequest request,
                @Header("X-Custom-Authentication") Optional<String> authHeader) {
            if (authHeader.isEmpty() || !authHeader.get().equals("Bearer custom-authentication")) {
                throw new RuntimeException("Invalid custom authentication header.");
            }

            return
                "{\n" +
                    "  \"groups\" : {" +
                        "\"limited\": [{" +
                            "\"role\": \"topic-read\"," +
                            "\"patterns\": [\"user.*\"]," +
                            "\"clusters\": [\"pub.*\"]" +
                        "}]" +
                    "}" +
                "}";
        }
    }
}

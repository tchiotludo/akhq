package org.akhq.security.authentication;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.authentication.ServerAuthentication;
import io.micronaut.security.config.SecurityConfigurationProperties;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.token.config.TokenConfiguration;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.HeaderAuth;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.reactivestreams.Publisher;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Requires(property = "akhq.security.header-auth.user-header")
@Singleton
@Slf4j
public class HeaderAuthenticationFetcher implements AuthenticationFetcher {
    @Inject
    HeaderAuth headerAuth;

    @Inject
    Authenticator authenticator;

    @Inject
    ClaimProvider claimProvider;

    @Inject
    TokenConfiguration configuration;

    private List<Pattern> ipPatternList;

    @PostConstruct
    public void init() {
        this.ipPatternList = headerAuth.getIpPatterns()
            .stream()
            .map(Pattern::compile)
            .collect(Collectors.toList());
    }

    @Override
    public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
        Optional<String> userHeaders = headerAuth.getUserHeader() != null ?
            request.getHeaders().get(headerAuth.getUserHeader(), String.class) :
            Optional.empty();

        if (userHeaders.isEmpty()) {
            return Publishers.empty();
        }

        if (!ipPatternList.isEmpty()) {
            InetSocketAddress socketAddress = request.getRemoteAddress();
            //noinspection ConstantConditions https://github.com/micronaut-projects/micronaut-security/issues/186
            if (socketAddress == null) {
                log.debug("Request remote address was not found. Skipping header authentication.");
                return Publishers.empty();
            }

            if (socketAddress.getAddress() == null) {
                log.debug("Could not resolve the InetAddress. Skipping header authentication.");
                return Publishers.empty();
            }

            String hostAddress = socketAddress.getAddress().getHostAddress();
            if (ipPatternList.stream().noneMatch(pattern ->
                pattern.pattern().equals(SecurityConfigurationProperties.ANYWHERE) ||
                    pattern.matcher(hostAddress).matches())) {
                log.warn("None of the IP patterns [{}] matched the host address [{}]. Skipping header authentication.", headerAuth.getIpPatterns(), hostAddress);
                return Publishers.empty();
            }
            log.debug("One or more of the IP patterns matched the host address [{}]. Continuing request processing.", hostAddress);
        }

        List<String> groupsHeader = headerAuth.getGroupsHeader() != null ?
            request.getHeaders().getAll(headerAuth.getGroupsHeader()) :
            Collections.emptyList();

        return Flowable
            .fromCallable(() -> {
                List<String> groups = groupsHeader
                    .stream()
                    .flatMap(s -> Arrays.stream(s.split(headerAuth.getGroupsHeaderSeparator())))
                    .collect(Collectors.toList());

                log.debug("Got groups [{}] from groupsHeader [{}] and separator [{}]", groups, groupsHeader, headerAuth.getGroupsHeaderSeparator());

                ClaimRequest claim =
                    ClaimRequest.builder()
                        .providerType(ClaimProviderType.HEADER)
                        .providerName(null)
                        .username(userHeaders.get())
                        .groups(groups)
                        .build();

                return Optional.of(claimProvider.generateClaim(claim));

            })
            .switchMap(t -> {
                if (t.isPresent()) {
                    return Flowable.just(new ServerAuthentication(
                        userHeaders.get(),
                        List.of(SecurityRule.IS_AUTHENTICATED),
                        Map.of("groups", t.get().getGroups())
                    ));
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Could not authenticate {}", userHeaders.get());
                    }
                    return Flowable.empty();
                }
            });
    }
}

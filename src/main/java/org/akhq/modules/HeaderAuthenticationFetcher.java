package org.akhq.modules;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationUserDetailsAdapter;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.config.SecurityConfigurationProperties;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.security.token.config.TokenConfiguration;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.HeaderAuth;
import org.akhq.utils.ClaimProvider;
import org.reactivestreams.Publisher;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

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

        Optional<String> groupHeaders = headerAuth.getGroupsHeader() != null ?
            request.getHeaders().get(headerAuth.getGroupsHeader(), String.class) :
            Optional.empty();

        return Flowable
            .fromCallable(() -> {
                List<String> groups = groupHeaders
                    .stream()
                    .flatMap(s -> Arrays.stream(s.split(headerAuth.getGroupsHeaderSeparator())))
                    .collect(Collectors.toList());

                ClaimProvider.AKHQClaimRequest claim =
                    ClaimProvider.AKHQClaimRequest.builder()
                        .providerType(ClaimProvider.ProviderType.HEADER)
                        .providerName(null)
                        .username(userHeaders.get())
                        .groups(groups)
                        .build();

                return Optional.of(claimProvider.generateClaim(claim));

            })
            .switchMap(t -> {
                if (t.isPresent()) {
                    UserDetails userDetails = new UserDetails(
                        userHeaders.get(),
                        t.get().getRoles(),
                        t.get().getAttributes()
                    );

                    return Flowable.just(new AuthenticationUserDetailsAdapter(
                        userDetails,
                        configuration.getRolesName(),
                        configuration.getNameKey()
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

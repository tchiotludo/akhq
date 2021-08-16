package org.akhq.modules;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationUserDetailsAdapter;
import io.micronaut.security.authentication.Authenticator;
import io.micronaut.security.authentication.UserDetails;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.micronaut.security.token.config.TokenConfiguration;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.HeaderAuth;
import org.akhq.utils.ClaimProvider;
import org.reactivestreams.Publisher;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    @Override
    public Publisher<Authentication> fetchAuthentication(HttpRequest<?> request) {
        Optional<String> userHeaders = headerAuth.getUserHeader() != null ?
            request.getHeaders().get(headerAuth.getUserHeader(), String.class) :
            Optional.empty();

        if (userHeaders.isEmpty()) {
            return Publishers.empty();
        }

        Optional<String> groupHeaders = headerAuth.getGroupsHeader() != null ?
            request.getHeaders().get(headerAuth.getGroupsHeader(), String.class) :
            Optional.empty();

        return Flowable
            .fromCallable(() -> {
                List<String> strings = groupsMapper(userHeaders.get(), groupHeaders);

                if (strings.size() == 0) {
                    return Optional.<ClaimProvider.AKHQClaimResponse>empty();
                }

                ClaimProvider.AKHQClaimRequest claim =
                    ClaimProvider.AKHQClaimRequest.builder()
                        .providerType(ClaimProvider.ProviderType.HEADER)
                        .providerName(null)
                        .username(userHeaders.get())
                        .groups(strings)
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

    private List<String> groupsMapper(String user, Optional<String> groupHeaders) {
        if (headerAuth.getUsers() == null || headerAuth.getUsers().size() == 0) {
            return groupsSplit(groupHeaders)
                .collect(Collectors.toList());
        }

        return headerAuth
            .getUsers()
            .stream()
            .filter(users -> users.getUsername().equals(user))
            .flatMap(users -> Stream.concat(
                groupsSplit(groupHeaders),
                users.getGroups() != null ? users.getGroups().stream() : Stream.empty()
            ))
            .collect(Collectors.toList());
    }

    private Stream<String> groupsSplit(Optional<String> groupHeaders) {
        return groupHeaders
            .stream()
            .flatMap(s -> Arrays.stream(s.split(headerAuth.getGroupsHeaderSeparator())));
    }
}

package org.akhq.configs.newAcls;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.FilterChain;
import io.micronaut.http.filter.HttpFilter;
import io.micronaut.security.authentication.AuthorizationException;
import io.micronaut.security.config.SecurityConfigurationProperties;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.web.router.RouteMatch;
import io.reactivex.Flowable;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

@Slf4j
@Requires(property = SecurityConfigurationProperties.PREFIX + ".enabled", value = StringUtils.FALSE)
@Filter("/api/**")
public class AKHQSecurityRuleAsFilterWhenSecurityDisabled extends AKHQSecurityRule implements HttpFilter {
    @Inject
    UserRequest userRequest;

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(HttpRequest<?> request, FilterChain chain) {
        log.info("Checking security for anonymous user");
        RouteMatch<?> routeMatch = request.getAttribute(HttpAttributes.ROUTE_MATCH, RouteMatch.class).orElse(null);

        SecurityRuleResult result = this.checkNotReact(request, routeMatch, userRequest);
        if (result == SecurityRuleResult.REJECTED) {
            return Flowable.error(new AuthorizationException(null));
        } else {
            return chain.proceed(request);
        }
    }
}

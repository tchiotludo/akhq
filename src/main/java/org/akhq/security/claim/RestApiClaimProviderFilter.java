package org.akhq.security.claim;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Primary
@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Requires(property = "akhq.security.rest.headers")
@ClientFilter("${akhq.security.rest.url}")
public class RestApiClaimProviderFilter implements HttpClientFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiClaimProviderFilter.class);

    private static final String HEADER_KEY = "name";
    private static final String HEADER_VALUE = "value";

    @Property(name = "akhq.security.rest.headers")
    private List<Map<String, String>> headers;

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        LOG.trace("Modifying outgoing authentication request. from muttable");
        headers.forEach(header -> request.header(header.get(HEADER_KEY), header.get(HEADER_VALUE)));
        return Mono.from(chain.proceed(request));
    }
}

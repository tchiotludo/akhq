package org.akhq.security.claim;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;

@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Requires(property = "akhq.security.rest.headers")
@ClientFilter("${akhq.security.rest.url}/**")
public class RestApiClaimProviderFilter implements HttpClientFilter {
    private static final String HEADER_KEY = "name";
    private static final String HEADER_VALUE = "value";

    @Nullable
    @Value("${akhq.security.rest.headers}")
    private List<Map<String, String>> headers;

    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        this.headers.forEach(header -> request.header(header.get(HEADER_KEY), header.get(HEADER_VALUE)));

        return chain.proceed(request);
    }
}
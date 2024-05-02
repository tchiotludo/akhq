package org.akhq.security.claim;

import java.util.List;
import java.util.Map;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;

@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Requires(property = "akhq.security.rest.headers")
@ClientFilter("${akhq.security.rest.url}")
public class RestApiClaimProviderFilter {
    private static final String HEADER_KEY = "name";
    private static final String HEADER_VALUE = "value";

    @Value("${akhq.security.rest.headers}")
    private List<Map<String, String>> headers;

    @RequestFilter
    public void filter(MutableHttpRequest<?> request) {
        this.headers.forEach(header -> request.header(header.get(HEADER_KEY), header.get(HEADER_VALUE)));
    }
}

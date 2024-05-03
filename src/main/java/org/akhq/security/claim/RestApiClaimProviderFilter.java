package org.akhq.security.claim;

import java.util.List;
import java.util.Map;

import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Primary
@Requires(property = "akhq.security.rest.enabled", value = StringUtils.TRUE)
@Requires(property = "akhq.security.rest.headers")
@ClientFilter("${akhq.security.rest.url}")
public class RestApiClaimProviderFilter {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiClaimProviderFilter.class);

    private static final String HEADER_KEY = "name";
    private static final String HEADER_VALUE = "value";

    @Property(name = "akhq.security.rest.headers")
    private List<Map<String, String>> headers;

    @RequestFilter
    public void filter(MutableHttpRequest<?> request) {
        LOG.trace("Modifying outgoing authentication request.");

        headers.forEach(header -> request.header(header.get(HEADER_KEY), header.get(HEADER_VALUE)));
    }
}

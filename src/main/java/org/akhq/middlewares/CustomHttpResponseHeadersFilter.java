package org.akhq.middlewares;


import io.micronaut.context.ApplicationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.akhq.configs.Server;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add or replace custom HTTP response headers and remove some headers that are not allowed according to the
 * configuration property {@code akhq.server.custom-http-response-headers}.
 */
@Filter("/**")
public class CustomHttpResponseHeadersFilter implements HttpServerFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CustomHttpResponseHeadersFilter.class);
    private static final String REMOVE_HEADER_VALUE = "REMOVE_HEADER";

    private final Server server;

    public CustomHttpResponseHeadersFilter(ApplicationContext context, Server server) {
        this.server = server;
        LOG.trace("Created instance of " + CustomHttpResponseHeadersFilter.class);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        Publisher<MutableHttpResponse<?>> responsePublisher = chain.proceed(request);
        LOG.trace("Adding custom headers to response.");
        return Publishers.map(responsePublisher, mutableHttpResponse -> {
            this.server.getCustomHttpResponseHeaders().entrySet().forEach(responseHeader -> {
                if (responseHeader.getValue().equals(REMOVE_HEADER_VALUE)) {
                    if (mutableHttpResponse.getHeaders().contains(responseHeader.getKey())) {
                        String existingHeaderValue = mutableHttpResponse.getHeaders().get(responseHeader.getKey());
                        mutableHttpResponse.getHeaders().remove(responseHeader.getKey());
                        LOG.trace("Removed header '{}' (value was '{}')", responseHeader.getKey(), existingHeaderValue);
                    } else {
                        LOG.trace("Header '{}' to be removed did not exist", responseHeader.getKey());
                    }
                } else {
                    if (mutableHttpResponse.getHeaders().contains(responseHeader.getKey())) {
                        String existingHeaderValue = mutableHttpResponse.getHeaders().get(responseHeader.getKey());
                        mutableHttpResponse.getHeaders().set(responseHeader.getKey(), responseHeader.getValue());
                        LOG.trace("Replaced existing header '{}' by value {} (value was '{}')", responseHeader.getKey(), responseHeader.getValue(), existingHeaderValue);
                    } else {
                        mutableHttpResponse.getHeaders().add(responseHeader.getKey(), responseHeader.getValue());
                        LOG.trace("Added custom header '{}' with value '{}'", responseHeader.getKey(), responseHeader.getValue());
                    }
                }
            });
            return mutableHttpResponse;
        });

    }
}
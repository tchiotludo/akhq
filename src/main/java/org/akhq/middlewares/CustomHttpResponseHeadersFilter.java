package org.akhq.middlewares;


import io.micronaut.context.ApplicationContext;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Filter("/**")
public class CustomHttpResponseHeadersFilter implements HttpServerFilter {

    private static final Logger LOG = LoggerFactory.getLogger(CustomHttpResponseHeadersFilter.class);
    public static final String PROPERTY_CUSTOM_RESPONSE_HEADERS = "akhq.http.headers.custom-response-headers";
    public static final String REMOVE_HEADER_VALUE = "REMOVE_HEADER";

    private Map<String,String> customResponseHeaders = new HashMap<>();

    @Inject
    public CustomHttpResponseHeadersFilter(ApplicationContext context) {
        if (context.containsProperty(PROPERTY_CUSTOM_RESPONSE_HEADERS)) {
            context.getProperty(PROPERTY_CUSTOM_RESPONSE_HEADERS, List.class).get().forEach(header -> {
                String[] customResponseHeaderArray = ((String)header).split("\\|");
                customResponseHeaders.put(customResponseHeaderArray[0], customResponseHeaderArray[1]);
            });
        }
        LOG.info("Created instance of " + CustomHttpResponseHeadersFilter.class);
    }

    /**
     * Set required headers and remove some headers that are not allowed.
     *
     * @param request The request
     * @param chain   The chain
     * @return The response
     */
    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        Publisher<MutableHttpResponse<?>> responsePublisher = chain.proceed(request);
        LOG.debug("Adding custom headers to response.");
        return Publishers.map(responsePublisher, mutableHttpResponse -> {
            this.customResponseHeaders.entrySet().forEach(responseHeader -> {
                if (!responseHeader.getValue().equals(REMOVE_HEADER_VALUE)) {
                    if (mutableHttpResponse.getHeaders().contains(responseHeader.getKey())) {
                        String existingHeaderValue = mutableHttpResponse.getHeaders().get(responseHeader.getKey());
                        mutableHttpResponse.getHeaders().set(responseHeader.getKey(), responseHeader.getValue());
                        LOG.debug("Replaced existing header {} with value {} by value {}", responseHeader.getKey(), existingHeaderValue, responseHeader.getValue());
                    } else {
                        mutableHttpResponse.getHeaders().add(responseHeader.getKey(), responseHeader.getValue());
                        LOG.debug("Added custom header {} with value {}", responseHeader.getKey(), responseHeader.getValue());
                    }
                } else {
                    if (mutableHttpResponse.getHeaders().contains(responseHeader.getKey())) {
                        mutableHttpResponse.getHeaders().remove(responseHeader.getKey());
                        LOG.debug("Removed header {}", responseHeader.getKey());
                    } else {
                        LOG.debug("Header to be removed was not existing: {}", responseHeader.getKey());
                    }
                }
            });
            return mutableHttpResponse;
        });

    }
}
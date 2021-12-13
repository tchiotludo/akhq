package org.akhq.middlewares;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.akhq.modules.KafkaModule;
import org.reactivestreams.Publisher;

import jakarta.inject.Inject;

@Filter("/**")
public class KafkaWrapperFilter implements HttpServerFilter {
    private final KafkaModule kafkaModule;

    @Inject
    public KafkaWrapperFilter(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (kafkaModule.getClustersList().size() == 0) {
            throw new IllegalArgumentException(
                "Couldn't find any clusters on your configuration file, " +
                "please ensure that the configuration file is loaded correctly"
            );
        }

        return chain.proceed(request);
    }
}

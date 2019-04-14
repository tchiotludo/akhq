package org.kafkahq.middlewares;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.modules.RequestHelper;
import org.kafkahq.repositories.AbstractRepository;
import org.reactivestreams.Publisher;

import javax.inject.Inject;

@Filter("/**")
public class KafkaWrapperFilter implements HttpServerFilter {
    private final KafkaModule kafkaModule;
    private final RequestHelper requestHelper;

    @Inject
    public KafkaWrapperFilter(KafkaModule kafkaModule, RequestHelper requestHelper) {
        this.kafkaModule = kafkaModule;
        this.requestHelper = requestHelper;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (kafkaModule.getClustersList().size() == 0) {
            throw new IllegalArgumentException(
                "Couldn't find any clusters on your configuration file, " +
                "please ensure that the configuration file is loaded correctly"
            );
        }

        requestHelper
            .getClusterId(request)
            .ifPresent(s -> AbstractRepository.setWrapper(new KafkaWrapper(kafkaModule, s)));

        return chain.proceed(request);
    }
}
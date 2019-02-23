package org.kafkahq.middlewares;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.kafkahq.configs.Connection;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.repositories.AbstractRepository;
import org.reactivestreams.Publisher;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@Filter("/**")
public class KafkaWrapperFilter implements HttpServerFilter {
    private final KafkaModule kafkaModule;

    @Inject
    public KafkaWrapperFilter(KafkaModule kafkaModule) {
        this.kafkaModule = kafkaModule;
    }

    @Value("${kafkahq.server.base-path}")
    protected String basePath;

    @Inject
    private List<Connection> connections;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String path = request.getPath();
        if (!this.basePath.equals("") && path.indexOf(this.basePath) == 0) {
            path = path.substring(this.basePath.length());
        }

        List<String> pathSplit = Arrays.asList(path.split("/"));

        // set cluster
        if (pathSplit.size() >= 2) {
            String clusterId = pathSplit.get(1);

            connections
                .stream()
                .filter(connection -> connection.getName().equals(clusterId))
                .findFirst()
                .ifPresent(connection -> AbstractRepository.setWrapper(new KafkaWrapper(kafkaModule, clusterId)));
        }

        return chain.proceed(request);
    }
}
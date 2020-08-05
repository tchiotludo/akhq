package org.akhq.modules;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Connection;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Slf4j
public class RequestHelper {
    @Value("${akhq.server.base-path}")
    protected String basePath;

    @Inject
    private List<Connection> connections;

    public Optional<String> getClusterId(HttpRequest<?> request) {
        String path = request.getPath();
        if (!basePath.equals("") && path.indexOf(basePath) == 0) {
            path = path.substring(basePath.length());
        }

        List<String> pathSplit = Arrays.asList(path.split("/"));

        if (pathSplit.size() >= 2) {
            String clusterId = pathSplit.get(1);

            return connections
                .stream()
                .filter(connection -> connection.getName().equals(clusterId))
                .map(r -> clusterId)
                .findFirst();
        }

        return Optional.empty();
    }
}

package io.micronaut.web.router.resource;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.util.CollectionUtils;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import jakarta.inject.Singleton;

@Singleton
@Replaces(StaticResourceResolver.class)
public class ReactStaticResourceResolver extends StaticResourceResolver {
    private List<ResourceLoader> uiResourceLoader;

    ReactStaticResourceResolver(List<StaticResourceConfiguration> configurations) {
        super(configurations);
        if (CollectionUtils.isNotEmpty(configurations)) {
            for (StaticResourceConfiguration config: configurations) {
                if (config.getMapping().contains("/ui/")) {
                    this.uiResourceLoader = config.getResourceLoaders();
                }
            }
        }
    }

    public Optional<URL> resolve(String resourcePath) {
        Optional<URL> resolve = super.resolve(resourcePath);

        if (resolve.isEmpty() && (resourcePath.contains("/ui/"))) {
            for (ResourceLoader loader : uiResourceLoader) {
                return loader.getResource("index.html");
            }
        }

        return resolve;
    }
}

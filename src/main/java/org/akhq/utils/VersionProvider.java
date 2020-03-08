package org.akhq.utils;

import io.micronaut.context.env.Environment;
import io.micronaut.context.env.PropertiesPropertySourceLoader;
import io.micronaut.core.util.StringUtils;
import lombok.Getter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

@Singleton
public class VersionProvider {

    @Getter
    private String tag = "Snapshot";

    @Inject
    Environment environment;

    @PostConstruct
    public void start() {
        new PropertiesPropertySourceLoader()
                .load("classpath:git", environment)
                .ifPresent(properties -> setTag(properties.get("git.tags")));
    }

    private void setTag(Object value) {
        String candidate = Objects.toString(value, null);
        this.tag = StringUtils.isNotEmpty(candidate) ? candidate : this.tag;
    }

}

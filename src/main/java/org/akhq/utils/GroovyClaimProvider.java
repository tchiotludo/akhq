package org.akhq.utils;

import groovy.lang.GroovyClassLoader;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;

@Slf4j
@Singleton
@Primary
@Requires(property = "akhq.security.groovy.enabled", value = StringUtils.TRUE)
public class GroovyClaimProvider implements ClaimProvider {
    final GroovyClassLoader loader = new GroovyClassLoader();
    private ClaimProvider groovyImpl;

    @Value("${akhq.security.groovy.file}")
    private String groovyFile;

    @PostConstruct
    private void init() {
        try {
            // the file must be an implementation of ClaimProvider Interface
            final Class<?> clazz = loader.parseClass(groovyFile);
            groovyImpl = ClaimProvider.class.cast(clazz.getDeclaredConstructors()[0].newInstance());

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("Error", e);
        }
    }

    @Override
    public AKHQClaimResponse generateClaim(AKHQClaimRequest request) {
        return groovyImpl.generateClaim(request);
    }
}

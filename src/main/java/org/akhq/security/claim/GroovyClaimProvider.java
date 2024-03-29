package org.akhq.security.claim;

import groovy.lang.GroovyClassLoader;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;

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
            groovyImpl = (ClaimProvider) clazz.getDeclaredConstructors()[0].newInstance();

        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("Error", e);
        }
    }

    @Override
    public ClaimResponse generateClaim(ClaimRequest request) {
        return groovyImpl.generateClaim(request);
    }
}

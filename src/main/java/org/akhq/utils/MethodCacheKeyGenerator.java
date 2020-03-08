package org.akhq.utils;

import io.micronaut.aop.chain.MethodInterceptorChain;
import io.micronaut.cache.interceptor.DefaultCacheKeyGenerator;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Introspected
public class MethodCacheKeyGenerator extends DefaultCacheKeyGenerator {
    @Override
    public Object generateKey(AnnotationMetadata annotationMetadata, Object... params) {
        return CacheKey.builder()
            .method(((MethodInterceptorChain) annotationMetadata).getExecutableMethod().getName())
            .parametersKey(super.generateKey(annotationMetadata, params))
            .build();
    }

    @ToString
    @EqualsAndHashCode
    @Builder
    public static class CacheKey {
        private String method;
        private Object parametersKey;
    }
}

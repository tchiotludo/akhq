package org.kafkahq.utils;

import io.micronaut.context.annotation.Value;
import org.codehaus.httpcache4j.uri.URIBuilder;

import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class CompletablePagedService {
    private Integer pageSize;
    private Integer threads;
    private ExecutorService executorService;

    public CompletablePagedService(
        @Value("${kafkahq.pagination.page-size}") Integer pageSize,
        @Value("${kafkahq.pagination.threads}") Integer threads
    ) {
        this.pageSize = pageSize;
        this.threads = threads;
        executorService = Executors.newFixedThreadPool(this.threads);
    }

    public <T> CompletablePaged<T> of(List<CompletableFuture<T>> list, URIBuilder uri, Integer currentPage) {
        return new CompletablePaged<>(list, this.pageSize, executorService, uri, currentPage);
    }
}

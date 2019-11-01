package org.kafkahq.utils;

import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class CompletablePaged<T> extends Paged<CompletableFuture<T>> {
    public CompletablePaged(List<CompletableFuture<T>> list, Integer pageSize, URIBuilder uri, Integer currentPage) {
        super(list, pageSize, uri, currentPage);
    }

    public List<T> complete() throws ExecutionException, InterruptedException {
        List<CompletableFuture<T>> futuresList = this.page();

        list
            .stream()
            .filter(r -> !futuresList.contains(r))
            .forEach(r -> {
                r.cancel(true);
            });

        CompletableFuture<Void> allFuturesResult = CompletableFuture.allOf(
            futuresList.toArray(new CompletableFuture[0])
        );

        return allFuturesResult.thenApply(s ->
            futuresList.stream().
                map(CompletableFuture::join).
                collect(Collectors.toList())
        )
            .get();
    }
}

package org.kafkahq.utils;

import lombok.AllArgsConstructor;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CompletablePaged <T> {
    private List<CompletableFuture<T>> list;
    private Integer pageSize;
    private URIBuilder uri;
    private Integer currentPage;

    public int size() {
        return this.list.size();
    }

    public URIBuilder before() {
        if (currentPage - 1 > 0) {
            return uri.addParameter("page", String.valueOf(currentPage - 1));
        } else {
            return URIBuilder.empty();
        }
    }

    public URIBuilder after() {
        if (currentPage + 1 <= this.pageCount()) {
            return uri.addParameter("page", String.valueOf(currentPage + 1));
        } else {
            return URIBuilder.empty();
        }
    }

    public int pageCount() {
        return (this.list.size() / this.pageSize) + (this.list.size() % this.pageSize == 0 ? 0 : 1);
    }

    public List<T> complete() throws ExecutionException, InterruptedException {
        int start;
        int end;

        if (this.currentPage == 1) {
            start = 0;
            end = Math.min(this.pageSize, list.size());
        } else if (this.currentPage == this.pageCount()) {
            start = (this.currentPage - 1) * this.pageSize;
            end = this.list.size();
        } else {
            start = (this.currentPage - 1) * this.pageSize;
            end = (this.currentPage * this.pageSize);
        }

        List<CompletableFuture<T>> futuresList = list.subList(start, end);

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

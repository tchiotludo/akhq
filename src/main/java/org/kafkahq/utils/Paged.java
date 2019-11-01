package org.kafkahq.utils;

import lombok.AllArgsConstructor;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.List;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor
public class Paged<T> {
    protected List<T> list;
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

    public List<T> page() throws ExecutionException, InterruptedException {
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

        return list.subList(start, end);
    }
}

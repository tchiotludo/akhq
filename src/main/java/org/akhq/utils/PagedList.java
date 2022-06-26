package org.akhq.utils;

import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PagedList<T> extends ArrayList<T> {
    private final Integer pageSize;
    private final Integer total;
    private final URIBuilder uri;
    private final Integer currentPage;

    private PagedList(Collection<? extends T> c, Integer pageSize, Integer total, URIBuilder uri, Integer currentPage) {
        super(c);
        this.pageSize = pageSize;
        this.total = total;
        this.uri = uri;
        this.currentPage = currentPage;
    }

    public int total() {
        return this.total;
    }

    public int pageSize() {
        return this.pageSize;
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
        return PagedList.pageCount(this.total, this.pageSize);
    }

    private static int pageCount(int total, int pageSize) {
        return (total / pageSize) + (total % pageSize == 0 ? 0 : 1);
    }

    public static <I, T> PagedList<T> of(List<I> list, Pagination pagination, CheckedFunction<List<I>, List<T>> mapper) throws ExecutionException, InterruptedException {
        int start;
        int end;

        if (pagination.getCurrentPage() == 1) {
            start = 0;
            end = Math.min(pagination.getPageSize(), list.size());
        } else if (pagination.getCurrentPage() == PagedList.pageCount(list.size(), pagination.getPageSize())) {
            start = (pagination.getCurrentPage() - 1) * pagination.getPageSize();
            end = list.size();
        } else {
            start = (pagination.getCurrentPage() - 1) * pagination.getPageSize();
            end = (pagination.getCurrentPage() * pagination.getPageSize());
        }

        List<I> sub = list.subList(start, end);

        return new PagedList<>(
            mapper.apply(sub),
            pagination.getPageSize(),
            list.size(),
            pagination.getUri(),
            pagination.getCurrentPage()
        );
    }
}

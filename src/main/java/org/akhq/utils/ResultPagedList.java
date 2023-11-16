package org.akhq.utils;

import java.util.List;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Serdeable
public class ResultPagedList<T> {
    private List<T> results;
    private String before;
    private String after;
    private int page;
    private int total;
    private int pageSize;

    public static <T> ResultPagedList<T> of(PagedList<T> pagedList) {
        return new ResultPagedList<>(
            pagedList,
            pagedList.before().toNormalizedURI(false).toString(),
            pagedList.after().toNormalizedURI(false).toString(),
            pagedList.pageCount(),
            pagedList.total(),
            pagedList.pageSize()
        );
    }
}

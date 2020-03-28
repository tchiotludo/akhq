package org.akhq.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ResultPagedList<T> {
    private List<T> results;
    private String before;
    private String after;
    private int page;
    private int total;

    public static <T> ResultPagedList<T> of(PagedList<T> pagedList) {
        return new ResultPagedList<>(
            pagedList,
            pagedList.before().toNormalizedURI(false).toString(),
            pagedList.after().toNormalizedURI(false).toString(),
            pagedList.pageCount(),
            pagedList.total()
        );
    }
}

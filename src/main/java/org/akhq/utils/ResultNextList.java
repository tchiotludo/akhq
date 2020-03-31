package org.akhq.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ResultNextList<T> {
    private List<T> results;
    private String after;
    private long size;

    public static <T> ResultNextList<T> of(List<T> pagedList, URIBuilder after, long size) {
        return new ResultNextList<>(
            pagedList,
            after.toNormalizedURI(false).toString(),
            size
        );
    }
}

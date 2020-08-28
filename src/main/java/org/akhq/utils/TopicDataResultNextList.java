package org.akhq.utils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codehaus.httpcache4j.uri.URIBuilder;

import java.util.List;

@NoArgsConstructor
@Getter
public class TopicDataResultNextList<T> extends ResultNextList<T> {
    private boolean canDeleteRecords;

    public TopicDataResultNextList(
        List<T> results,
        String after,
        long size,
        boolean canDeleteRecords
    ) {
        super(results, after, size);
        this.canDeleteRecords = canDeleteRecords;
    }

    public static <T> TopicDataResultNextList<T> of(List<T> pagedList, URIBuilder after, long size, boolean canDeleteRecords) {
        return new TopicDataResultNextList<>(
            pagedList,
            after.toNormalizedURI(false).toString(),
            size,
            canDeleteRecords
        );
    }
}

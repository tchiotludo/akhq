package org.akhq.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codehaus.httpcache4j.uri.URIBuilder;

@AllArgsConstructor
@Getter
public class Pagination{
    private Integer pageSize;
    private URIBuilder uri;
    private Integer currentPage;
}

package org.akhq.utils;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.codehaus.httpcache4j.uri.URIBuilder;

@AllArgsConstructor
@Getter
//@Serdeable
public class Pagination{
    private Integer pageSize;
    private URIBuilder uri;
    private Integer currentPage;
}

package org.akhq.middlewares;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;

@Controller("/issues")
public class CustomHttpResponseHeadersFilterTestPage {

    @Get("/{number}")
    public String issue(@PathVariable Integer number) {
        return "Issue # " + number + "!";
    }
}

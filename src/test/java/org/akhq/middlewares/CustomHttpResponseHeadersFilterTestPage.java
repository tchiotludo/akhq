package org.akhq.middlewares;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;

@Controller("/issues")
public class CustomHttpResponseHeadersFilterTestPage {

    @Get("/{number}")
    public HttpResponse<String> issue(@PathVariable Integer number) {
        return HttpResponse.ok("Issue # " + number + "!")
                .header("Cross-Origin-Opener-Policy", "test if being replaced")
                .header("Via", "test if being removed");
    }
}

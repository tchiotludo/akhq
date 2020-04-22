package org.akhq.rest.error;


import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Error;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;

@Slf4j
//@Controller
public class ErrorController {

    private static final String INTERNAL_TITLE_FORMAT = "%d %s - %s";
    private static final String INTERNAL_DESCRIPTION_FORMAT = "%s";
    private static final String NOT_FOUND_TITLE_FORMAT = "%d %s";
    private static final String NOT_FOUND_DESCRIPTION_FORMAT = "Endpoint with the following url not found: %s";

    @Error(global = true)
    public HttpResponse error(HttpRequest request, Throwable e) {
        log.error(e.getMessage(), e);

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));

        ApiError error = new ApiError(
                String.format(INTERNAL_TITLE_FORMAT, HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                        HttpStatus.INTERNAL_SERVER_ERROR.getReason(), e.getMessage()),
                String.format(INTERNAL_DESCRIPTION_FORMAT, stringWriter.toString())
        );

        return HttpResponse.<ApiError>serverError()
                .body(error);
    }

    @Error(status = HttpStatus.NOT_FOUND, global = true)
    public HttpResponse notFound(HttpRequest request) {
        log.error("Endpoint with the following url not found: {}", request.getUri().toString());

        ApiError error = new ApiError(
                String.format(NOT_FOUND_TITLE_FORMAT, HttpStatus.NOT_FOUND.getCode(), HttpStatus.NOT_FOUND.getReason()),
                String.format(NOT_FOUND_DESCRIPTION_FORMAT, request.getUri())
        );

        return HttpResponse.<ApiError>notFound()
                .body(error);
    }
}

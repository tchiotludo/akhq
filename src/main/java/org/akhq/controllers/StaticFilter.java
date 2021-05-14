package org.akhq.controllers;

import com.google.common.io.CharStreams;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.server.netty.types.files.NettyStreamedFileCustomizableResponseType;
import io.micronaut.http.server.netty.types.files.NettySystemFileCustomizableResponseType;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

@Filter("/ui/**")
public class StaticFilter implements HttpServerFilter {
    @Nullable
    @Value("${micronaut.server.context-path}")
    protected String basePath;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Publishers
            .map(chain.proceed(request), response -> {
                boolean first = response.getBody(NettyStreamedFileCustomizableResponseType.class)
                    .filter(n -> n.getMediaType().getName().equals(MediaType.TEXT_HTML))
                    .isPresent();

                boolean second = response.getBody(NettySystemFileCustomizableResponseType.class)
                    .filter(n -> n.getFile().getAbsoluteFile().toString().endsWith("ui/index.html"))
                    .isPresent();

                if (first || second) {
                    try {
                        InputStream inputStream = Objects.requireNonNull(StaticFilter.class.getClassLoader()
                            .getResourceAsStream("ui/index.html"));

                        String content;
                        try (Reader reader = new InputStreamReader(inputStream)) {
                            content = CharStreams.toString(reader);
                        }

                        String finalBody = replace(content);

                        return HttpResponse
                            .<String>ok()
                            .body(finalBody)
                            .contentType(MediaType.TEXT_HTML)
                            .contentLength(finalBody.length());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                return response;
            });
    }

    private String replace(String line) {

        line = line.replace("./ui", (basePath == null ? "" : basePath) + "/ui");


        return line;
    }
}

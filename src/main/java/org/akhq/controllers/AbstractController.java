package org.akhq.controllers;

import io.micronaut.context.annotation.Value;

import java.net.URI;
import java.net.URISyntaxException;

abstract public class AbstractController {
    @Value("${micronaut.server.context-path:}")
    protected String basePath;

    protected String getBasePath() {
        return basePath.replaceAll("/$","");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
    }
}

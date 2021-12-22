package org.akhq.controllers;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.security.utils.SecurityService;

import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

abstract public class AbstractController {
    @Value("${micronaut.server.context-path:}")
    protected String basePath;

    @Inject
    private ApplicationContext applicationContext;

    protected String getBasePath() {
        return basePath.replaceAll("/$","");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
    }

}

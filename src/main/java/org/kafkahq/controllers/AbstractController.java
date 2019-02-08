package org.kafkahq.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import lombok.Builder;
import lombok.Getter;
import org.codehaus.httpcache4j.uri.QueryParams;
import org.codehaus.httpcache4j.uri.URIBuilder;
import org.jooby.Request;
import org.jooby.View;
import org.kafkahq.App;
import org.kafkahq.modules.KafkaModule;

abstract public class AbstractController {
    private static final String SESSION_TOAST = "TOAST";
    private static Gson gson = new GsonBuilder()
        .enableComplexMapKeySerialization()
        .create();

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private Config config;

    protected View template(Request request, String cluster, View view) {
        view
            .put("clusterId", cluster)
            .put("clusters", this.kafkaModule.getClustersList())
            .put("registryEnabled", this.kafkaModule.getRegistryRestClient(cluster) != null)
            .put("basePath", App.getBasePath(config));

        request
            .ifFlash(SESSION_TOAST)
            .ifPresent(s -> view.put("toast", s));

        return view;
    }

    protected URIBuilder uri(Request request) {
        return URIBuilder.empty()
            .withPath(request.path())
            .withParameters(QueryParams.parse(request.queryString().orElse("")));
    }

    protected Toast toast(Request request, Toast toast) {
        request.flash(SESSION_TOAST, gson.toJson(toast));

        return toast;
    }

    @Builder
    @Getter
    public static class Toast {
        public enum Type {
            success,
            error,
            warning,
            info,
            question
        }

        private String title;

        private String message;

        @Builder.Default
        private Type type = Type.info;
    }
}
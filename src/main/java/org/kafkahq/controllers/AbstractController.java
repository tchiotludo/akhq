package org.kafkahq.controllers;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.jooby.Request;
import org.jooby.View;
import org.kafkahq.modules.KafkaModule;

abstract public class AbstractController {
    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private Config config;

    protected View template(Request request, View view) {
        return view
            .put("clusterId", request.param("cluster").value())
            .put("clusters", this.kafkaModule.getClustersList())
            .put("basePath", config.getString("application.path"));
    }
}
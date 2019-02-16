package org.kafkahq;

import io.micronaut.runtime.Micronaut;

public class App {
    // route
    {
        /*
        use(new Whoops());

        use("*", new RequestLogger()
            .latency()
            .extended()
        );

        assets("/favicon.ico");

        use("*", "/", (req, rsp, chain)  -> {
            rsp.redirect("/" + this.require(KafkaModule.class).getClustersList().get(0) + "/topic");
        });
        use("*", "/{cluster}", (req, rsp, chain)  -> {
            rsp.redirect("/" + req.param("cluster").value() + "/topic");
        });
        */
    }

    public static void main(String[] args) {
        Micronaut.run(App.class);
    }
}
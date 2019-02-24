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
        });
        */
    }

    public static void main(String[] args) {
        Micronaut.run(App.class);
    }
}
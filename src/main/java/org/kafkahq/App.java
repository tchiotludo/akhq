package org.kafkahq;

import com.typesafe.config.Config;
import org.jooby.FlashScope;
import org.jooby.Jooby;
import org.jooby.RequestLogger;
import org.jooby.assets.Assets;
import org.jooby.ftl.Ftl;
import org.jooby.json.Jackson;
import org.jooby.livereload.LiveReload;
import org.jooby.whoops.Whoops;
import org.kafkahq.controllers.*;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.KafkaWrapper;
import org.kafkahq.repositories.AbstractRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class App extends Jooby {
    // module
    {
        use(new FlashScope());

        use("*", new RequestLogger()
            .latency()
            .extended()
        );
        use(new Jackson());

        on("dev", () -> {
            Path basedir = Paths.get(System.getProperty("user.dir"));
            use(new Whoops());
            use(new LiveReload()
                .register(basedir.resolve("public"),
                    "**/*.ftl"
                )
                .register(basedir.resolve("target"),
                    "**/*.class",
                    "**/*.conf",
                    "**/*.properties")
                .register(basedir.resolve("build"),
                    "**/*.class",
                    "**/*.conf",
                    "**/*.properties")
            );
        });

        on("prod", () -> {
            use(new Assets());
        });
        assets("/favicon.ico");

        use(new Ftl("/", ".ftl"));
        use(KafkaModule.class);

        // @RequestScoped hack
        use("*", "/{cluster}/**", (req, rsp, chain)  -> {
            Optional<String> cluster = req.param("cluster").toOptional();
            cluster.ifPresent(clusterId ->
                AbstractRepository.setWrapper(new KafkaWrapper(this.require(KafkaModule.class), clusterId))
            );

            chain.next(req, rsp);
        });
    }

    // route
    {
        use("*", "/", (req, rsp, chain)  -> {
            rsp.redirect("/" + this.require(KafkaModule.class).getClustersList().get(0) + "/topic");
        });
        use("*", "/{cluster}", (req, rsp, chain)  -> {
            rsp.redirect("/" + req.param("cluster").value() + "/topic");
        });
        use(NodeController.class);
        use(SchemaController.class);
        use(TopicController.class);
        use(GroupController.class);
        sse("/{cluster}/topic/{topic}/search/{search}", new DataSseController());
    }

    public static String getBasePath(Config config) {
        return config.getString("application.path").replaceAll("/$","");
    }

    public static void main(String[] args) {
        run(App::new, args);
    }
}
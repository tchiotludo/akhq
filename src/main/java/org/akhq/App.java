package org.akhq;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;

@OpenAPIDefinition(
    info = @Info(
        title = "AKHQ",
        license = @License(name = "Apache 2.0", url = "https://raw.githubusercontent.com/tchiotludo/akhq/master/LICENSE")
    ),
    tags = {
        @Tag(name = "AKHQ", description = "AKHQ api"),
        @Tag(name = "node", description = "Kafka Node api"),
        @Tag(name = "topic", description = "Kafka Topic api"),
        @Tag(name = "topic data", description = "Kafka Topic data api"),
        @Tag(name = "consumer group", description = "Kafka Consumer group api"),
        @Tag(name = "schema registry", description = "Kafka Schema registry api"),
        @Tag(name = "connect", description = "Kafka Connect api"),
        @Tag(name = "acls", description = "Kafka Acls api"),
    }
)
public class App {
    public static void main(String[] args) {
        Micronaut.run(App.class);
    }
}

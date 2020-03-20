package org.kafkahq.rest;


import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micrometer.core.lang.Nullable;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import lombok.extern.slf4j.Slf4j;
import org.kafkahq.service.ConsumerGroupService;
import org.kafkahq.service.SchemaRegistryService;
import org.kafkahq.service.dto.ConsumerGroup.ConsumerGroupListDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaRegistryListDTO;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class SchemaRegistryResource {

    private SchemaRegistryService schemaRegistryService;

    @Inject
    public SchemaRegistryResource(SchemaRegistryService schemaRegistryService) {
        this.schemaRegistryService =schemaRegistryService;
    }

    @Get("/schema")
    public SchemaRegistryListDTO fetchAllSchemaRegistry(String clusterId, @Nullable String search, Optional<Integer> pageNumber) throws ExecutionException, InterruptedException, IOException, RestClientException {
        log.debug("Fetch all Schema Registry");

        return schemaRegistryService.getSchemaRegistry(clusterId,  Optional.ofNullable(search), pageNumber);
    }
}

package org.kafkahq.rest;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.kafkahq.service.SchemaService;
import org.kafkahq.service.dto.schema.SchemaVersionDTO;
import org.kafkahq.service.dto.schema.UpdateSchemaDTO;

import javax.inject.Inject;
import java.io.IOException;

@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class SchemaResource {
    private SchemaService schemaService;

    @Inject
    public SchemaResource(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Get("schema/version")
    public SchemaVersionDTO fetchLatestSchemaVersion(String clusterId, String subject) throws IOException, RestClientException {
        log.debug("Fetching latest schema version from subject: {}", subject);
        return schemaService.getLatestSchemaVersion(clusterId, subject);
    }

    @Post("/schema/update")
    public void updateSchema(UpdateSchemaDTO updateSchemaDTO) throws IOException, RestClientException {
        log.debug("Updating schema from subject: {}", updateSchemaDTO.getSubject());
        schemaService.updateSchema(updateSchemaDTO);
    }
}

package org.kafkahq.rest;

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.kafkahq.service.SchemaService;
import org.kafkahq.service.dto.schema.SchemaDTO;

import javax.inject.Inject;

@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class SchemaResource {
    private SchemaService schemaService;


    @Inject
    public SchemaResource(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Post("/schema/create")
    public void schemaCreate(@Body SchemaDTO schemaDTO) throws Exception {
        log.debug("Create topic {}", schemaDTO.getSubject());
        schemaService.createSchema(schemaDTO);
    }
}

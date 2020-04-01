package org.akhq.rest;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import lombok.extern.slf4j.Slf4j;
import org.akhq.service.SchemaService;
import org.akhq.service.dto.SchemaRegistry.DeleteSchemaVersionDTO;
import org.akhq.service.dto.schema.SchemaVersionDTO;
import org.akhq.service.dto.schema.UpdateSchemaDTO;

import javax.inject.Inject;
import java.io.IOException;

import io.micronaut.http.annotation.Body;

import io.micrometer.core.lang.Nullable;
import io.micronaut.http.annotation.*;
import org.akhq.service.dto.SchemaRegistry.CreateSchemaDTO;
import org.akhq.service.dto.SchemaRegistry.SchemaListDTO;
import org.akhq.service.dto.SchemaRegistry.DeleteSchemaDTO;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;


@Slf4j
@Controller("${kafkahq.server.base-path:}/api")
public class SchemaResource {
    private SchemaService schemaService;

    @Inject
    public SchemaResource(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @Get("/schema/versions")
    public List<SchemaVersionDTO> fetchSchemaVersions(String clusterId, String subject) throws IOException, RestClientException {
        log.debug("Fetching schema versions from subject: {}", subject);
        return schemaService.getAllSchemaVersions(clusterId, subject);
    }



    @Get("/schema/version")
    public SchemaVersionDTO fetchLatestSchemaVersion(String clusterId, String subject) throws IOException, RestClientException {
        log.debug("Fetching latest schema version from subject: {}", subject);
        return schemaService.getLatestSchemaVersion(clusterId, subject);
    }



    @Delete("/schema/version")
    public void deleteSchemaVersion(DeleteSchemaVersionDTO deleteSchemaDTO) throws IOException, RestClientException {
        log.debug("Deleting schema version {} from subject: {}", deleteSchemaDTO.getVersionId(), deleteSchemaDTO.getSubject());
        schemaService.deleteSchemaVersion(deleteSchemaDTO);
    }

    @Post("/schema/update")
    public void updateSchema(UpdateSchemaDTO updateSchemaDTO) throws IOException, RestClientException {
        log.debug("Updating schema from subject: {}", updateSchemaDTO.getSubject());
        schemaService.updateSchema(updateSchemaDTO);
    }

    @Get("/schema")
    public SchemaListDTO fetchAllSchemaRegistry(String clusterId, @Nullable String search, Optional<Integer> pageNumber) throws ExecutionException, InterruptedException, IOException, RestClientException {
        log.debug("Fetch all Schema Registry");

        return schemaService.getSchema(clusterId,  Optional.ofNullable(search), pageNumber);
    }

    @Delete("/schema/delete")
    public SchemaListDTO deleteSchema(@Body DeleteSchemaDTO deleteSchemaDTO) throws ExecutionException, InterruptedException, IOException, RestClientException {
        log.debug("Delete schema: {}", deleteSchemaDTO.getSubject());
        schemaService.deleteSchema(deleteSchemaDTO.getClusterId(), deleteSchemaDTO.getSubject());
        return schemaService.getSchema(deleteSchemaDTO.getClusterId(), Optional.empty(), Optional.empty());
    }

    @Post("/schema/create")
    public void schemaCreate(@Body CreateSchemaDTO schemaDTO) throws Exception {
        log.debug("Create topic {}", schemaDTO.getSubject());
        schemaService.createSchema(schemaDTO);
    }
}

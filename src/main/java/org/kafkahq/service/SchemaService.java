package org.kafkahq.service;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.errors.ApiException;
import org.kafkahq.models.Schema;
import org.kafkahq.repositories.SchemaRegistryRepository;
import org.kafkahq.service.dto.schema.SchemaDTO;
import org.kafkahq.service.dto.schema.SchemaVersionDTO;
import org.kafkahq.service.dto.schema.UpdateSchemaDTO;
import org.kafkahq.service.mapper.SchemaMapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class SchemaService {
    private SchemaRegistryRepository schemaRepository;
    private SchemaMapper schemaMapper;

    @Inject
    public SchemaService(SchemaRegistryRepository schemaRepository, SchemaMapper schemaMapper) {
        this.schemaRepository = schemaRepository;
        this.schemaMapper = schemaMapper;
    }

    public SchemaVersionDTO getLatestSchemaVersion(String clusterId, String subject) throws IOException, RestClientException {
        Schema schemaLatestVersion = schemaRepository.getLatestVersion(clusterId, subject);
        Schema.Config schemaLatestConfig = schemaRepository.getConfig(clusterId, subject);

        return schemaMapper.fromSchemaToSchemaVersionDTO(Pair.of(schemaLatestVersion, schemaLatestConfig));
    }

    public Schema createSchema(SchemaDTO schemaDTO) throws Exception {
        if (this.schemaRepository.exist(schemaDTO.getCluster(), schemaDTO.getSubject())) {
            throw new Exception("Subject '" + schemaDTO.getSubject() + "' already exits");
        }

        Schema register = this.registerSchema(schemaDTO.getCluster(), schemaDTO.getSubject(), schemaDTO.getSchema(), schemaDTO.getCompatibilityLevel());

        return register;
    }

    public void updateSchema(UpdateSchemaDTO updateSchemaDTO) throws IOException, RestClientException {
        registerSchema(updateSchemaDTO.getClusterId(), updateSchemaDTO.getSubject(), updateSchemaDTO.getSchema(), updateSchemaDTO.getCompatibilityLevel());
    }

    private Schema registerSchema(String cluster, String subject, String schema, String compatibilityLevel) throws IOException, RestClientException {
        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schema);

        Schema register = this.schemaRepository.register(cluster, subject, avroSchema);

        Schema.Config config = Schema.Config.builder()
                .compatibilityLevel(Schema.Config.CompatibilityLevelConfig.valueOf(
                        compatibilityLevel
                ))
                .build();
        this.schemaRepository.updateConfig(cluster, subject, config);

        return register;
    }
}

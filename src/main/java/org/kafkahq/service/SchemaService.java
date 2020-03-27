package org.kafkahq.service;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.errors.ApiException;
import io.micronaut.context.env.Environment;
import org.kafkahq.models.Schema;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.SchemaRegistryRepository;
import org.kafkahq.service.dto.SchemaRegistry.DeleteSchemaVersionDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaDTO;
import org.kafkahq.service.dto.schema.SchemaVersionDTO;
import org.kafkahq.service.dto.schema.UpdateSchemaDTO;
import org.kafkahq.service.mapper.SchemaMapper;
import org.kafkahq.service.dto.SchemaRegistry.CreateSchemaDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaListDTO;
import org.kafkahq.service.mapper.SchemaMapper;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import java.io.IOException;

@Singleton
public class SchemaService {
    private KafkaModule kafkaModule;
    private AbstractKafkaWrapper kafkaWrapper;
    private Environment environment;
    private SchemaRegistryRepository schemaRepository;
    private SchemaMapper schemaMapper;

    @Value("${kafkahq.pagination.page-size}")
    private Integer pageSize;

    @Inject
    public SchemaService(KafkaModule kafkaModule, SchemaMapper schemaMapper, SchemaRegistryRepository schemaRepository, AbstractKafkaWrapper kafkaWrapper, Environment environment) {
        this.kafkaModule = kafkaModule;
        this.schemaMapper = schemaMapper;
        this.kafkaWrapper = kafkaWrapper;
        this.environment = environment;
        this.schemaRepository = schemaRepository;
    }

    public SchemaListDTO getSchema(String clusterId, Optional<String> search, Optional<Integer> pageNumber)
            throws ExecutionException, InterruptedException, IOException, RestClientException {
        Pagination pagination = new Pagination(pageSize, pageNumber.orElse(1));
        PagedList<Schema> list = this.schemaRepository.list(clusterId, pagination, search);
        ArrayList<SchemaDTO> schemaRegistryList = new ArrayList<>();
        list.stream().map(schemaRegistry -> schemaRegistryList.add(schemaMapper.fromSchemaRegistryToSchemaRegistryDTO(schemaRegistry))).collect(Collectors.toList());
        return new SchemaListDTO(schemaRegistryList, list.pageCount());
    }

    public List<SchemaVersionDTO> getAllSchemaVersions(String clusterId, String subject) throws IOException, RestClientException {
        return schemaRepository.getAllVersions(clusterId, subject)
                .stream()
                .map(schema -> schemaMapper.fromSchemaToSchemaVersionDTO(Pair.of(schema, Optional.empty())))
                .collect(Collectors.toList());
    }

    public void deleteSchemaVersion(DeleteSchemaVersionDTO deleteSchemaVersionDTO) throws IOException, RestClientException {
        this.schemaRepository.deleteVersion(
                deleteSchemaVersionDTO.getClusterId(),
                deleteSchemaVersionDTO.getSubject(),
                deleteSchemaVersionDTO.getVersionId()
        );
    }

    public SchemaVersionDTO getLatestSchemaVersion(String clusterId, String subject) throws IOException, RestClientException {
        Schema schemaLatestVersion = schemaRepository.getLatestVersion(clusterId, subject);
        Schema.Config schemaLatestConfig = schemaRepository.getConfig(clusterId, subject);

        return schemaMapper.fromSchemaToSchemaVersionDTO(Pair.of(schemaLatestVersion, Optional.of(schemaLatestConfig.toString())));
    }

    public Schema createSchema(CreateSchemaDTO schemaDTO) throws Exception {
        if (this.schemaRepository.exist(schemaDTO.getCluster(), schemaDTO.getSubject())) {
            throw new Exception("Subject '" + schemaDTO.getSubject() + "' already exits");
        }

        Schema register = this.registerSchema(schemaDTO.getCluster(), schemaDTO.getSubject(), schemaDTO.getSchema(), schemaDTO.getCompatibilityLevel());

        return register;
    }

    public int deleteSchema(String clusterId, String schema) throws ExecutionException, InterruptedException, IOException, RestClientException {
        return schemaRepository.delete(clusterId, schema);
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

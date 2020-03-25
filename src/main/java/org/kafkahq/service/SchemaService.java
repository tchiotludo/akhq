package org.kafkahq.service;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import org.kafkahq.models.Schema;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.SchemaRegistryRepository;
import org.kafkahq.service.dto.SchemaRegistry.SchemaDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaListDTO;
import org.kafkahq.service.mapper.SchemaMapper;
import org.kafkahq.utils.PagedList;
import org.kafkahq.utils.Pagination;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


@Singleton
    public class SchemaService {
        private KafkaModule kafkaModule;
        private AbstractKafkaWrapper kafkaWrapper;
        private Environment environment;
        private SchemaRegistryRepository schemaRegistryRepository;
        private SchemaMapper schemaMapper;

        @Value("${kafkahq.pagination.page-size}")
        private Integer pageSize;

        @Inject
        public SchemaService(KafkaModule kafkaModule, SchemaMapper schemaMapper, SchemaRegistryRepository schemaRegistryRepository, AbstractKafkaWrapper kafkaWrapper, Environment environment) {
            this.kafkaModule = kafkaModule;
            this.schemaMapper = schemaMapper;
            this.kafkaWrapper = kafkaWrapper;
            this.environment = environment;
            this.schemaRegistryRepository=schemaRegistryRepository;
        }

        public SchemaListDTO getSchema(String clusterId, Optional<String> search, Optional<Integer> pageNumber)
                throws ExecutionException, InterruptedException, IOException, RestClientException {
            Pagination pagination = new Pagination(pageSize, pageNumber.orElse(1));
            PagedList<Schema> list =this.schemaRegistryRepository.list(clusterId, pagination, search);
            ArrayList<SchemaDTO> schemaRegistryList = new ArrayList<>();
            list.stream().map(schemaRegistry -> schemaRegistryList.add(schemaMapper.fromSchemaRegistryToSchemaRegistryDTO(schemaRegistry))).collect(Collectors.toList());
            return new SchemaListDTO(schemaRegistryList, list.pageCount());
        }
    public int deleteSchema(String clusterId, String schema) throws ExecutionException, InterruptedException, IOException, RestClientException {
       return schemaRegistryRepository.delete(clusterId, schema);

    
    }


    public Schema createSchema(SchemaDTO schemaDTO) throws Exception {


        if (this.schemaRepository.exist(schemaDTO.getCluster(), schemaDTO.getSubject())) {
            throw new Exception("Subject '" + schemaDTO.getSubject() + "' already exits");
        }

        org.apache.avro.Schema avroSchema = new org.apache.avro.Schema.Parser().parse(schemaDTO.getSchema());

        Schema register = this.schemaRepository.register(schemaDTO.getCluster(), schemaDTO.getSubject(), avroSchema);

        Schema.Config config = Schema.Config.builder()
                .compatibilityLevel(Schema.Config.CompatibilityLevelConfig.valueOf(
                        schemaDTO.getCompatibilityLevel()
                ))
                .build();

        this.schemaRepository.updateConfig(schemaDTO.getCluster(), schemaDTO.getSubject(), config);

        return register;
    }
}

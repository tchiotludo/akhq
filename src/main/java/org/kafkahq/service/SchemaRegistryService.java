package org.kafkahq.service;

import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import org.kafkahq.configs.Connection;
import org.kafkahq.models.Schema;
import org.kafkahq.modules.AbstractKafkaWrapper;
import org.kafkahq.modules.KafkaModule;
import org.kafkahq.repositories.SchemaRegistryRepository;
import org.kafkahq.service.dto.ConsumerGroup.ConsumerGroupDTO;
import org.kafkahq.service.dto.ConsumerGroup.ConsumerGroupListDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaRegistryDTO;
import org.kafkahq.service.dto.SchemaRegistry.SchemaRegistryListDTO;
import org.kafkahq.service.mapper.SchemaRegistryMapper;
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
    public class SchemaRegistryService {
        private KafkaModule kafkaModule;
        private AbstractKafkaWrapper kafkaWrapper;
        private Environment environment;
        private SchemaRegistryRepository schemaRegistryRepository;
        private SchemaRegistryMapper schemaRegistryMapper;

        @Value("${kafkahq.pagination.page-size}")
        private Integer pageSize;

        @Inject
        public SchemaRegistryService(KafkaModule kafkaModule,SchemaRegistryMapper schemaRegistryMapper, SchemaRegistryRepository schemaRegistryRepository, AbstractKafkaWrapper kafkaWrapper, Environment environment) {
            this.kafkaModule = kafkaModule;
            this.schemaRegistryMapper=schemaRegistryMapper;
            this.kafkaWrapper = kafkaWrapper;
            this.environment = environment;
            this.schemaRegistryRepository=schemaRegistryRepository;
        }
        public SchemaRegistryListDTO getSchemaRegistry(String clusterId, Optional<String> search, Optional<Integer> pageNumber)
                throws ExecutionException, InterruptedException {
            Pagination pagination = new Pagination(pageSize, pageNumber.orElse(1
            ));

            PagedList<Schema> list = null;
            try {
                list = this.schemaRegistryRepository.list(clusterId, pagination, search);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RestClientException e) {
                e.printStackTrace();
            }


            ArrayList<SchemaRegistryDTO> schemaRegistryList = new ArrayList<>();
            list.stream().map(schemaRegistry -> schemaRegistryList.add(schemaRegistryMapper.fromSchemaRegistryToSchemaRegistryDTO(schemaRegistry))).collect(Collectors.toList());

            return new SchemaRegistryListDTO(schemaRegistryList, list.pageCount());
        }

}

package org.kafkahq.service;

import org.kafkahq.models.Schema;
import org.kafkahq.repositories.SchemaRegistryRepository;
import org.kafkahq.service.dto.schema.SchemaDTO;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SchemaService {

    private SchemaRegistryRepository schemaRepository;

    @Inject
    public SchemaService(SchemaRegistryRepository schemaRepository) {
        this.schemaRepository = schemaRepository;
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

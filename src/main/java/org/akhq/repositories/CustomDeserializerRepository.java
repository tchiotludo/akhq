package org.akhq.repositories;

import org.akhq.configs.Connection;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.modules.KafkaModule;
import org.akhq.modules.schemaregistry.BufSchemaRegistryClient;
import org.akhq.utils.AvroToJsonDeserializer;
import org.akhq.utils.AvroToJsonSerializer;
import org.akhq.utils.BufProtobufToJsonDeserializer;
import org.akhq.utils.ProtobufToJsonDeserializer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CustomDeserializerRepository {
    @Inject
    private KafkaModule kafkaModule;
    @Inject
    private AvroToJsonSerializer avroToJsonSerializer;
    private final Map<String, ProtobufToJsonDeserializer> protobufToJsonDeserializers = new HashMap<>();
    private final Map<String, AvroToJsonDeserializer> avroToJsonDeserializers = new HashMap<>();
    private final Map<String, BufProtobufToJsonDeserializer> bsrDeserializers = new HashMap<>();

    public ProtobufToJsonDeserializer getProtobufToJsonDeserializer(String clusterId) {
        if (!this.protobufToJsonDeserializers.containsKey(clusterId)) {
            this.protobufToJsonDeserializers.put(
                    clusterId,
                    new ProtobufToJsonDeserializer(this.kafkaModule.getConnection(clusterId).getDeserialization().getProtobuf())
            );
        }
        return this.protobufToJsonDeserializers.get(clusterId);
    }

    public AvroToJsonDeserializer getAvroToJsonDeserializer(String clusterId) {
        if (!this.avroToJsonDeserializers.containsKey(clusterId)) {
            this.avroToJsonDeserializers.put(
                clusterId,
                new AvroToJsonDeserializer(this.kafkaModule.getConnection(clusterId).getDeserialization().getAvroRaw(), this.avroToJsonSerializer)
            );
        }
        return this.avroToJsonDeserializers.get(clusterId);
    }

    public BufProtobufToJsonDeserializer getBsrProtobufDeserializer(String clusterId) {
        if (!this.bsrDeserializers.containsKey(clusterId)) {
            Connection connection = kafkaModule.getConnection(clusterId);

            // Only create BSR deserializer if BSR is configured
            if (connection.getSchemaRegistry() != null &&
                connection.getSchemaRegistry().getType() == SchemaRegistryType.BSR) {

                BufSchemaRegistryClient bsrClient = kafkaModule.getBsrClient(clusterId);
                if (bsrClient != null) {
                    this.bsrDeserializers.put(
                        clusterId,
                        new BufProtobufToJsonDeserializer(
                            bsrClient,
                            connection.getDeserialization().getProtobuf()
                        )
                    );
                }
            }
        }
        return this.bsrDeserializers.get(clusterId);
    }
}

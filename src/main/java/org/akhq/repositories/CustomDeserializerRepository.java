package org.akhq.repositories;

import org.akhq.modules.KafkaModule;
import org.akhq.utils.AvroToJsonDeserializer;
import org.akhq.utils.AvroToJsonSerializer;
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
}

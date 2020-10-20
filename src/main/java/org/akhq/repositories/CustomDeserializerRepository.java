package org.akhq.repositories;

import org.akhq.modules.KafkaModule;
import org.akhq.utils.ProtobufToJsonDeserializer;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CustomDeserializerRepository {
    @Inject
    private KafkaModule kafkaModule;
    private final Map<String, ProtobufToJsonDeserializer> protobufToJsonDeserializers = new HashMap<>();

    public ProtobufToJsonDeserializer getProtobufToJsonDeserializer(String clusterId) {
        if (!this.protobufToJsonDeserializers.containsKey(clusterId)) {
            this.protobufToJsonDeserializers.put(
                    clusterId,
                    new ProtobufToJsonDeserializer(this.kafkaModule.getConnection(clusterId).getDeserialization())
            );
        }
        return this.protobufToJsonDeserializers.get(clusterId);
    }
}

package org.akhq.repositories;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.JsonToAvroSerializer;

@Singleton
public class CustomSerializerRepository {
    @Inject
    private KafkaModule kafkaModule;
    private final Map<String, JsonToAvroSerializer> jsonToAvroSerializers = new HashMap<>();

    public JsonToAvroSerializer getJsonToAvroSerializer(String clusterId) {
        if (!this.jsonToAvroSerializers.containsKey(clusterId)) {
            this.jsonToAvroSerializers.put(
                clusterId,
                new JsonToAvroSerializer(this.kafkaModule.getConnection(clusterId).getSerialization().getAvroRaw())
            );
        }
        return this.jsonToAvroSerializers.get(clusterId);
    }
}

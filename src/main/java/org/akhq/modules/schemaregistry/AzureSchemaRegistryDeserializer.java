package org.akhq.modules.schemaregistry;

import com.azure.data.schemaregistry.SchemaRegistryClient;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.akhq.modules.KafkaModule;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure Schema Registry support for AKHQ
 * Deserializes RAW Avro messages from Azure Event Hub using schemas from Azure Schema Registry
 */
@Singleton
public class AzureSchemaRegistryDeserializer implements Deserializer<Object> {

    private final Map<String, Schema> schemaCache = new HashMap<>();
    private final KafkaModule kafkaModule;
    private final String configuredClusterId;

    public AzureSchemaRegistryDeserializer(KafkaModule kafkaModule, String clusterId) {
        this.kafkaModule = kafkaModule;
        this.configuredClusterId = clusterId;
    }


    @SneakyThrows
    @Override
    public Object deserialize(String topic, byte[] data) {
        return deserialize(topic, data, null);
    }

    /**
     * Deserialize with headers support (Azure Schema Registry uses content-type header)
     */
    public Object deserialize(String topic, byte[] data, String schemaId) throws IOException {
        if (data == null || data.length == 0) {
            return null;
        }
        Schema schema = this.getSchemaById(configuredClusterId, schemaId);
        GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
        return reader.read(null, decoder);
    }

    /**
     * Get schema by ID from Azure Schema Registry
     */
    private Schema getSchemaById(String clusterId, String schemaId) {
        return schemaCache.computeIfAbsent(schemaId, id -> {
            try {
                SchemaRegistryClient client = this.kafkaModule.getAzureSchemaRegistryClient(clusterId);
                // Get schema definition by ID
                String schemaDefinition = client.getSchema(schemaId).getDefinition();
                // Parse Avro schema
                Schema.Parser parser = new Schema.Parser();
                return parser.parse(schemaDefinition);
            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve schema by ID from Azure: " + schemaId, e);
            }
        });
    }

    @Override
    public void close() {
        // Clear caches
        schemaCache.clear();
    }
}

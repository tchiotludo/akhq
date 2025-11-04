package org.akhq.utils;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Connection.Deserialization.ProtobufDeserializationTopicsMapping;
import org.akhq.configs.TopicsMapping;
import org.akhq.models.KeyValue;
import org.akhq.modules.schemaregistry.BufSchemaRegistryClient;
import org.apache.kafka.common.errors.SerializationException;

import java.util.ArrayList;
import java.util.List;

/**
 * Deserializer for protobuf messages using Buf Schema Registry (BSR).
 * Supports header-based schema resolution (preferred) and topic mapping fallback.
 */
@Slf4j
public class BufProtobufToJsonDeserializer {
    private final BufSchemaRegistryClient bsrClient;
    private final List<TopicsMapping> topicsMapping;

    public static final String HEADER_BSR_COMMIT = "buf.registry.value.schema.commit";
    public static final String HEADER_BSR_MESSAGE = "buf.registry.value.schema.message";

    public BufProtobufToJsonDeserializer(
            BufSchemaRegistryClient bsrClient,
            ProtobufDeserializationTopicsMapping config) {
        this.bsrClient = bsrClient;
        this.topicsMapping = config != null ? config.getTopicsMapping() : new ArrayList<>();
    }

    /**
     * Deserialize protobuf binary data using BSR.
     * Prefers headers, falls back to topic mapping configuration.
     *
     * @param topic   Topic name
     * @param buffer  Binary protobuf data
     * @param isKey   Is this a key or value
     * @param headers Kafka record headers (may contain BSR schema info)
     * @return JSON string or null if not configured
     */
    public String deserialize(String topic, byte[] buffer, boolean isKey,
                              List<KeyValue<String, String>> headers) {
        if (buffer == null) {
            log.debug("Buffer is null for topic: {}", topic);
            return null;
        }

        log.debug("BSR deserialize called - topic: {}, isKey: {}, bufferSize: {}",
                 topic, isKey, buffer.length);

        // Prefer headers (standard BSR approach)
        String commitFromHeader = getHeaderValue(headers, HEADER_BSR_COMMIT);
        String messageFromHeader = getHeaderValue(headers, HEADER_BSR_MESSAGE);

        if (commitFromHeader != null && messageFromHeader != null) {
            log.debug("Using BSR schema from headers - commit: {}, message: {}", commitFromHeader, messageFromHeader);
            return deserializeWithBSR(commitFromHeader, messageFromHeader, buffer);
        }

        // Fall back to topic mapping if configured
        TopicsMapping mapping = findMatchingMapping(topic);
        if (mapping != null && mapping.getBsrCommit() != null && mapping.getBsrMessageType() != null) {
            log.debug("Using BSR schema from topic mapping - topic: {}, commit: {}, message: {}",
                     topic, mapping.getBsrCommit(), mapping.getBsrMessageType());
            return deserializeWithBSR(mapping.getBsrCommit(), mapping.getBsrMessageType(), buffer);
        }

        log.debug("No BSR configuration found for topic [{}]", topic);
        return null;
    }

    private String deserializeWithBSR(String commit, String messageFQN, byte[] buffer) {
        log.debug("Deserializing with BSR - commit: {}, message: {}, bufferSize: {}",
                 commit, messageFQN, buffer.length);

        try {
            // Step 1: Fetch descriptor from BSR (via HTTP)
            Descriptors.Descriptor descriptor = bsrClient.getMessageDescriptor(commit, messageFQN);

            // Step 2: Parse protobuf bytes directly using the descriptor
            DynamicMessage message = DynamicMessage.parseFrom(descriptor, buffer);

            // Step 3: Convert to JSON
            String json = JsonFormat.printer().print(message);
            log.debug("Successfully deserialized and converted to JSON");
            return json;
        } catch (Exception e) {
            log.error("Failed to deserialize with BSR - commit: {}, message: {}", commit, messageFQN, e);
            throw new SerializationException(
                    String.format("Failed to deserialize with BSR [commit=%s, message=%s]: %s",
                            commit, messageFQN, e.getMessage()), e);
        }
    }

    private TopicsMapping findMatchingMapping(String topic) {
        return topicsMapping.stream()
                .filter(mapping -> topic.matches(mapping.getTopicRegex()))
                .findFirst()
                .orElse(null);
    }

    private String getHeaderValue(List<KeyValue<String, String>> headers, String key) {
        if (headers == null) {
            return null;
        }
        return headers.stream()
                .filter(h -> key.equals(h.getKey()))
                .map(KeyValue::getValue)
                .findFirst()
                .orElse(null);
    }
}

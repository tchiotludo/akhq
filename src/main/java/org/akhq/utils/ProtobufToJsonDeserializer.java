package org.akhq.utils;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Connection;
import org.akhq.configs.TopicsMapping;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for deserialization of messages in Protobuf format using topics mapping config.
 */
@Slf4j
public class ProtobufToJsonDeserializer {
    private final Connection.ProtobufDeserializationTopicsMapping protobufDeserializationTopicsMapping;
    private final Map<String, List<Descriptor>> descriptors;
    private final List<TopicsMapping> topicsMapping;

    public ProtobufToJsonDeserializer(Connection.ProtobufDeserializationTopicsMapping protobufDeserializationTopicsMapping) {
        this.protobufDeserializationTopicsMapping = protobufDeserializationTopicsMapping;
        if (protobufDeserializationTopicsMapping == null) {
            this.descriptors = new HashMap<>();
            this.topicsMapping = new ArrayList<>();
        } else {
            this.descriptors = buildAllDescriptors();
            this.topicsMapping = protobufDeserializationTopicsMapping.getTopicsMapping();
        }
    }

    /**
     * Check protobuf deserialization topics mapping config, get all protobuf descriptor files in Base64 format and convert to bytes
     * For each descriptor file builds Descriptors list - full description with all dependencies
     *
     * @return map where keys are topic regexes and values are Descriptors matching these regexes
     */
    private Map<String, List<Descriptor>> buildAllDescriptors() {
        List<TopicsMapping> topicsMapping = protobufDeserializationTopicsMapping.getTopicsMapping();
        Map<String, List<Descriptor>> allDescriptors = new HashMap<>();
        for (TopicsMapping mapping : topicsMapping) {
            byte[] decodedDescriptorFile = Base64.getDecoder().decode(mapping.getDescriptorFileBase64());
            try {
                allDescriptors.put(mapping.getTopicRegex(), buildAllDescriptorsForDescriptorFile(decodedDescriptorFile));
            } catch (IOException | DescriptorValidationException e) {
                log.error("Cannot build Protobuf descriptors for topics regex [{}]", mapping.getTopicRegex(), e);
            }
        }
        return allDescriptors;
    }

    /**
     * Builds Descriptors list for current descriptor file
     */
    private List<Descriptor> buildAllDescriptorsForDescriptorFile(byte[] descriptorFile)
            throws IOException, DescriptorValidationException {
        FileDescriptorSet fileDescriptorSet = FileDescriptorSet.parseFrom(descriptorFile);

        List<FileDescriptor> fileDescriptorsWithDependencies = new ArrayList<>();
        for (FileDescriptorProto protoDescriptorFile : fileDescriptorSet.getFileList()) {
            FileDescriptor fd = FileDescriptor.buildFrom(protoDescriptorFile,
                    fileDescriptorsWithDependencies.toArray(new FileDescriptor[fileDescriptorsWithDependencies.size()]));
            fileDescriptorsWithDependencies.add(fd);
        }

        return fileDescriptorsWithDependencies
                .stream().flatMap(desc -> desc.getMessageTypes().stream())
                .collect(Collectors.toList());
    }

    /**
     * Deserialize binary data from Protobuf format to Json.
     * Topic name should match topic-regex from {@code akhq.connections.[clusterName].deserialization.protobuf.topics-mapping} config
     * and message-type should be set for key or value in that config.
     *
     * @param topic  current topic name
     * @param buffer binary data to decode
     * @param isKey  is this data represent key or value
     * @return {@code null} if cannot deserialize or configuration is not matching, return decoded string otherwise
     */
    public String deserialize(String topic, byte[] buffer, boolean isKey) {
        TopicsMapping matchingConfig = findMatchingConfig(topic);
        if (matchingConfig == null) {
            return null;
        }
        String messageType = matchingConfig.getValueMessageType();
        if (isKey) {
            messageType = matchingConfig.getKeyMessageType();
        }

        if (messageType == null) {
            return null;
        }

        String result = null;
        try {
            result = tryToDeserializeWithMessageType(buffer, matchingConfig.getTopicRegex(), messageType);
        } catch (Exception e) {
            log.error("Cannot deserialize message with Protobuf deserializer " +
                    "for topic [{}] and message type [{}]", topic, messageType, e);
        }
        return result;
    }

    private TopicsMapping findMatchingConfig(String topic) {
        for (TopicsMapping mapping : topicsMapping) {
            if (topic.matches(mapping.getTopicRegex())) {
                return new TopicsMapping(mapping.getTopicRegex(), mapping.getDescriptorFileBase64(), mapping.getKeyMessageType(), mapping.getValueMessageType());
            }
        }
        return null;
    }

    private String tryToDeserializeWithMessageType(byte[] buffer, String topicRegex, String messageType) throws IOException {
        List<Descriptor> descriptorsWithDependencies = this.descriptors.get(topicRegex);
        List<Descriptor> descriptorsForConfiguredMessageTypes =
                descriptorsWithDependencies.stream()
                        .filter(mp -> messageType.equals(mp.getName()))
                        .collect(Collectors.toList());

        for (Descriptor descriptor : descriptorsForConfiguredMessageTypes) {
            String decodedMessage = tryToParseDataToJsonWithDescriptor(buffer, descriptor, descriptorsWithDependencies);
            if (!decodedMessage.isEmpty()) {
                return decodedMessage;
            }
        }
        return null;
    }

    private String tryToParseDataToJsonWithDescriptor(byte[] buffer, Descriptor descriptor, List<Descriptor> allDependencies) throws IOException {
        DynamicMessage message = DynamicMessage.parseFrom(descriptor, buffer);
        JsonFormat.TypeRegistry typeRegistry = JsonFormat.TypeRegistry.newBuilder().add(allDependencies).build();
        JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry);
        return printer.print(message);
    }
}

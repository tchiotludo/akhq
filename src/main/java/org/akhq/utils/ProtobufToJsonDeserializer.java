package org.akhq.utils;

import com.google.protobuf.*;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.Connection;
import org.akhq.configs.TopicsMapping;
import org.apache.kafka.common.errors.SerializationException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for deserialization of messages in Protobuf format using topics mapping config.
 */
@Slf4j
public class ProtobufToJsonDeserializer {
    private final Map<String, List<Descriptor>> descriptors;
    private final List<TopicsMapping> topicsMapping;
    private final String protobufDescriptorsFolder;
    private final Map<String, List<Descriptor>> descriptorByTypeName;

    public ProtobufToJsonDeserializer(Connection.Deserialization.ProtobufDeserializationTopicsMapping protobufDeserializationTopicsMapping) {
        if (protobufDeserializationTopicsMapping == null) {
            this.descriptors = new HashMap<>();
            this.topicsMapping = new ArrayList<>();
            this.protobufDescriptorsFolder = null;
            this.descriptorByTypeName = null;
        } else {
            this.protobufDescriptorsFolder = protobufDeserializationTopicsMapping.getDescriptorsFolder();
            this.descriptorByTypeName = new HashMap<>();
            this.topicsMapping = protobufDeserializationTopicsMapping.getTopicsMapping();
            this.descriptors = buildAllDescriptors();
        }
    }

    /**
     * Check Protobuf deserialization topics mapping config, get all Protobuf descriptor files
     * from Protobuf descriptor folder or descriptor files in Base64 format and convert to bytes.
     * For each descriptor file builds Descriptors list - full description with all dependencies.
     *
     * @return map where keys are topic regexes and values are Descriptors matching these regexes
     */
    private Map<String, List<Descriptor>> buildAllDescriptors() {
        Map<String, List<Descriptor>> allDescriptors = new HashMap<>();
        for (TopicsMapping mapping : topicsMapping) {
            byte[] fileBytes = new byte[0];
            try {
                fileBytes = getDescriptorFileAsBytes(mapping);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Cannot get a descriptor file for the topics regex [%s]", mapping.getTopicRegex()), e);
            }
            try {
                allDescriptors.put(mapping.getTopicRegex(), buildAllDescriptorsForDescriptorFile(fileBytes));
            } catch (IOException | DescriptorValidationException e) {
                throw new RuntimeException(String.format("Cannot build Protobuf descriptors for the topics regex [%s]", mapping.getTopicRegex()), e);
            }
        }
        buildAdditionalDescriptors();
        return allDescriptors;
    }

    /**
     * Build descriptors for files from Protobuf descriptor folder, which are not specified in topics mapping.
     * These descriptors can be used for deserializing dynamic messages with {@code Any} field.
     */
    private void buildAdditionalDescriptors() {
        List<String> filesFromTopicsMapping = topicsMapping.stream()
                .map(TopicsMapping::getDescriptorFile)
                .collect(Collectors.toList());
        List<String> additionalFiles = getDescriptorFiles().stream()
                .filter(file -> !filesFromTopicsMapping.contains(file))
                .map(file -> protobufDescriptorsFolder + File.separator + file)
                .collect(Collectors.toList());
        for (String file : additionalFiles) {
            try {
                byte[] fileBytes = Files.readAllBytes(Path.of(file));
                buildAllDescriptorsForDescriptorFile(fileBytes);
            } catch (IOException | DescriptorValidationException e) {
                e.printStackTrace();
            }
        }
        buildDescriptorsForWellKnownTypes();
    }

    private void buildDescriptorsForWellKnownTypes() {
        List<FileDescriptor> descriptors = new ArrayList<>();
        descriptors.add(WrappersProto.getDescriptor());
        descriptors.add(AnyProto.getDescriptor());
        descriptors.add(ApiProto.getDescriptor());
        descriptors.add(DurationProto.getDescriptor());
        descriptors.add(DescriptorProtos.getDescriptor());
        descriptors.add(EmptyProto.getDescriptor());
        descriptors.add(FieldMaskProto.getDescriptor());
        descriptors.add(SourceContextProto.getDescriptor());
        descriptors.add(StructProto.getDescriptor());
        descriptors.add(TimestampProto.getDescriptor());
        descriptors.add(TypeProto.getDescriptor());

        buildDescriptorsByTypeName(descriptors);
    }

    /**
     * Extracts all descriptor files from Protobuf descriptor folder
     */
    private List<String> getDescriptorFiles() {
        if (protobufDescriptorsFolder != null && Files.exists(Path.of(protobufDescriptorsFolder))) {
            File path = new File(protobufDescriptorsFolder);
            String[] fileNames = path.list();
            return fileNames == null ? Collections.emptyList() : Arrays.asList(fileNames);
        }
        return Collections.emptyList();
    }

    byte[] getDescriptorFileAsBytes(TopicsMapping mapping) throws IOException {
        if (protobufDescriptorsFolder != null && Files.exists(Path.of(protobufDescriptorsFolder))) {
            String descriptorFile = mapping.getDescriptorFile();
            if (descriptorFile != null) {
                String fullPath = protobufDescriptorsFolder + File.separator + descriptorFile;
                return Files.readAllBytes(Path.of(fullPath));
            }
        }
        String descriptorFileBase64 = mapping.getDescriptorFileBase64();
        if (descriptorFileBase64 != null) {
            return Base64.getDecoder().decode(descriptorFileBase64);
        }
        throw new FileNotFoundException("Protobuf descriptor file is not found for topic regex [" +
                mapping.getTopicRegex() + "]. File name or Base64 file content is not specified.");
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

        return buildDescriptorsByTypeName(fileDescriptorsWithDependencies);
    }

    private List<Descriptor> buildDescriptorsByTypeName(List<FileDescriptor> fileDescriptorsWithDependencies) {
        List<Descriptor> result = new ArrayList<>();
        for (FileDescriptor fd : fileDescriptorsWithDependencies) {
            for (Descriptor messageType : fd.getMessageTypes()) {
                descriptorByTypeName.put(messageType.getFullName(), fd.getMessageTypes());
                result.add(messageType);
            }
        }
        return result;
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
            log.debug("Protobuf deserialization config is not found for topic [{}]", topic);
            return null;
        }

        if (matchingConfig.getValueMessageType() == null && matchingConfig.getKeyMessageType() == null) {
            throw new SerializationException(String.format("Protobuf deserialization is configured for topic [%s], " +
                    "but message type is not specified neither for a key, nor for a value.", topic));
        }

        String messageType = matchingConfig.getValueMessageType();
        if (isKey) {
            messageType = matchingConfig.getKeyMessageType();
        }

        if (messageType == null) {
            return null;
        }

        String result;
        try {
            result = tryToDeserializeWithMessageType(buffer, matchingConfig.getTopicRegex(), messageType);
        } catch (Exception e) {
            String cannotSeserializeMessage = String.format("Cannot deserialize message with Protobuf deserializer " +
                    "for topic [%s] and message type [%s]", topic, messageType);
            log.error(cannotSeserializeMessage + ". Raw message bytes [{}]", buffer, e);
            throw new SerializationException(cannotSeserializeMessage, e);
        }
        return result;
    }

    private TopicsMapping findMatchingConfig(String topic) {
        for (TopicsMapping mapping : topicsMapping) {
            if (topic.matches(mapping.getTopicRegex())) {
                return new TopicsMapping(
                        mapping.getTopicRegex(),
                        mapping.getDescriptorFile(), mapping.getDescriptorFileBase64(),
                        mapping.getKeyMessageType(), mapping.getValueMessageType());
            }
        }
        return null;
    }

    private String tryToDeserializeWithMessageType(byte[] buffer, String topicRegex, String messageType) throws IOException {
        List<Descriptor> descriptorsWithDependencies = this.descriptors.get(topicRegex);
        List<Descriptor> descriptorsForConfiguredMessageTypes =
                descriptorsWithDependencies.stream()
                        .filter(mp -> messageType.equals(mp.getFullName()))
                        .collect(Collectors.toList());

        if (descriptorsForConfiguredMessageTypes.isEmpty()) {
            throw new SerializationException(String.format("Not found descriptors for topic regex [%s] " +
                    "and message type [%s]", topicRegex, messageType));
        }
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
        JsonFormat.TypeRegistry.Builder builder = JsonFormat.TypeRegistry.newBuilder().add(allDependencies);

        Set<Descriptor> descriptorsForFieldsWithTypeAny = getDescriptorsForFieldsWithTypeAny(message);
        if (!descriptorsForFieldsWithTypeAny.isEmpty()) {
            builder.add(descriptorsForFieldsWithTypeAny);
        }

        JsonFormat.TypeRegistry typeRegistry = builder.build();
        JsonFormat.Printer printer = JsonFormat.printer().usingTypeRegistry(typeRegistry);
        return printer.print(message);
    }

    /**
     * If a message contains a field with type {@code Any}, we can find out an actual field type
     * at the runtime from the message, get descriptors for this field type and add them to TypeRegistry.
     * Multiple layer complex objects with {@code Any} fields on any layers are also supported using recursion.
     */
    private Set<Descriptor> getDescriptorsForFieldsWithTypeAny(DynamicMessage message) {
        Set<Descriptor> result = new HashSet<>();
        for (Object fieldDescriptor : message.getAllFields().values()) {
            if (isFieldTypeAny(fieldDescriptor)) {
                var internalFields = ((DynamicMessage) fieldDescriptor).getAllFields().entrySet();
                for (var internalField : internalFields) {
                    if (isFieldTypeAnyTypeUrl(internalField.getKey())) {
                        String typeNameFromUrl = internalField.getValue().toString().split("/")[1];
                        result.addAll(descriptorByTypeName.get(typeNameFromUrl));
                    }
                }
            } else if (isDynamicMessage(fieldDescriptor)) {
                result.addAll(getDescriptorsForFieldsWithTypeAny((DynamicMessage) fieldDescriptor));
            }
        }
        return result;
    }

    private boolean isDynamicMessage(Object fieldDescriptor) {
        return fieldDescriptor instanceof DynamicMessage;
    }

    private boolean isFieldTypeAny(Object fieldDescriptor) {
        if (fieldDescriptor instanceof DynamicMessage) {
            return (((DynamicMessage) fieldDescriptor).getDescriptorForType().getFullName().equals("google.protobuf.Any"));
        }
        return false;
    }

    private boolean isFieldTypeAnyTypeUrl(Descriptors.FieldDescriptor field) {
        return field.getFullName().equals("google.protobuf.Any.type_url");
    }
}

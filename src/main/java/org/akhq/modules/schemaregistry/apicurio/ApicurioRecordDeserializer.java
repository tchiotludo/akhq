package org.akhq.modules.schemaregistry.apicurio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.protobuf.MessageIndexes;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import lombok.extern.slf4j.Slf4j;
import org.akhq.utils.AvroToJsonSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Deserializes Kafka message keys and values produced with Apicurio Registry SerDes into
 * JSON strings for display in AKHQ.
 *
 * <p>Supports all standard Apicurio wire formats:
 * <ul>
 *     <li>Payload with magic byte {@code 0x00} + 4-byte ID (Apicurio v3 default / Confluent-compatible)</li>
 *     <li>Payload with magic byte {@code 0x00} + 8-byte ID (Apicurio v2 / Debezium legacy format)</li>
 *     <li>Kafka headers ({@code apicurio.*.globalId}, {@code contentId}, {@code contentHash},
 *         {@code groupId}/{@code artifactId}/{@code version}, inline {@code schema}/{@code schemaType})</li>
 *     <li>Avro (binary and JSON encoding), JSON Schema, and Protobuf (Apicurio {@code Ref},
 *         Confluent message indexes, and {@code msgType} header)</li>
 * </ul>
 */
@Slf4j
public class ApicurioRecordDeserializer {
    public static final byte MAGIC_BYTE = 0x00;
    private static final int PROTOBUF_REF_NAME_TAG = (1 << 3) | 2; // field 1, length-delimited
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ApicurioSchemaRegistryClient client;
    private final ApicurioConfig config;

    public ApicurioRecordDeserializer(ApicurioSchemaRegistryClient client) {
        this.client = client;
        this.config = client.getConfig();
    }

    public boolean canDeserialize(Headers headers, byte[] payload, boolean isKey) {
        return payload != null && extractWireReference(headers, payload, isKey) != null;
    }

    public String extractSchemaId(Headers headers, byte[] payload, boolean isKey) {
        if (payload == null) {
            return null;
        }
        ApicurioWireReference ref = extractWireReference(headers, payload, isKey);
        if (ref == null) {
            return null;
        }
        if (ref.globalId != null) {
            return String.valueOf(ref.globalId);
        }
        if (ref.contentId != null) {
            return String.valueOf(ref.contentId);
        }
        if (ref.payloadId != null) {
            return String.valueOf(ref.payloadId);
        }
        if (ref.inlineSchema != null) {
            return null;
        }
        try {
            ResolvedApicurioSchema resolved = resolveSchema(ref);
            Long id = resolved != null ? resolved.preferredId(config.getUseId()) : null;
            return id != null ? String.valueOf(id) : null;
        } catch (Exception e) {
            log.debug("Failed to resolve Apicurio schema ID for record", e);
            return null;
        }
    }

    public String extractSchemaSubject(Headers headers, byte[] payload, boolean isKey) {
        if (payload == null) {
            return null;
        }
        ApicurioWireReference ref = extractWireReference(headers, payload, isKey);
        if (ref == null || ref.inlineSchema != null) {
            return null;
        }
        if (ref.artifactId != null && !ref.artifactId.isBlank()) {
            return client.formatSubject(ref.groupId, ref.artifactId);
        }
        try {
            ResolvedApicurioSchema resolved = resolveSchema(ref);
            return resolved != null ? resolved.subject() : null;
        } catch (Exception e) {
            log.debug("Failed to resolve Apicurio schema subject for record", e);
            return null;
        }
    }

    public String deserialize(
        String topic,
        Headers headers,
        byte[] payload,
        boolean isKey,
        AvroToJsonSerializer avroToJsonSerializer
    ) throws IOException, RestClientException {
        if (payload == null) {
            return null;
        }
        ApicurioWireReference ref = extractWireReference(headers, payload, isKey);
        if (ref == null) {
            return new String(payload, StandardCharsets.UTF_8);
        }

        ResolvedApicurioSchema resolved = resolveSchema(ref);
        if (resolved == null || resolved.parsedSchema() == null) {
            throw new IllegalStateException("Could not resolve Apicurio schema for topic '" + topic + "'");
        }

        int offset = ref.payloadOffset;
        int length = payload.length - offset;
        if (length < 0) {
            throw new IllegalStateException("Payload too short for Apicurio wire header (length=" + payload.length + ", offset=" + offset + ")");
        }

        String artifactType = resolved.artifactType() != null
            ? resolved.artifactType().toUpperCase(Locale.ROOT)
            : AvroSchema.TYPE;

        return switch (artifactType) {
            case AvroSchema.TYPE -> deserializeAvro(
                ((AvroSchema) resolved.parsedSchema()).rawSchema(),
                payload,
                offset,
                length,
                ref.encoding,
                avroToJsonSerializer
            );
            case JsonSchema.TYPE -> deserializeJson(payload, offset, length);
            case ProtobufSchema.TYPE -> deserializeProtobuf(
                (ProtobufSchema) resolved.parsedSchema(),
                payload,
                offset,
                length,
                ref.msgType,
                avroToJsonSerializer
            );
            default -> new String(payload, offset, length, StandardCharsets.UTF_8);
        };
    }

    ApicurioWireReference extractWireReference(Headers headers, byte[] payload, boolean isKey) {
        String encoding = readStringHeader(headers, isKey ? config.getKeyEncodingHeader() : config.getValueEncodingHeader());
        String msgType = readStringHeader(headers, isKey ? config.getKeyMsgTypeHeader() : config.getValueMsgTypeHeader());

        // 1. Check Kafka headers first (Apicurio headers mode has no magic byte in payload)
        if (headers != null) {
            Long globalId = readLongHeader(headers, isKey ? config.getKeyGlobalIdHeader() : config.getValueGlobalIdHeader());
            Long contentId = readLongHeader(headers, isKey ? config.getKeyContentIdHeader() : config.getValueContentIdHeader());
            String contentHash = readStringHeader(headers, isKey ? config.getKeyContentHashHeader() : config.getValueContentHashHeader());
            String groupId = readStringHeader(headers, isKey ? config.getKeyGroupIdHeader() : config.getValueGroupIdHeader());
            String artifactId = readStringHeader(headers, isKey ? config.getKeyArtifactIdHeader() : config.getValueArtifactIdHeader());
            String version = readStringHeader(headers, isKey ? config.getKeyVersionHeader() : config.getValueVersionHeader());
            String inlineSchema = readStringHeader(headers, isKey ? config.getKeySchemaHeader() : config.getValueSchemaHeader());
            String inlineSchemaType = readStringHeader(headers, isKey ? config.getKeySchemaTypeHeader() : config.getValueSchemaTypeHeader());

            if (globalId != null || contentId != null || contentHash != null || artifactId != null || inlineSchema != null) {
                return new ApicurioWireReference(
                    globalId,
                    contentId,
                    null,
                    0,
                    0,
                    contentHash,
                    groupId,
                    artifactId,
                    version,
                    inlineSchema,
                    inlineSchemaType,
                    encoding,
                    msgType
                );
            }
        }

        // 2. Check payload magic byte + 4-byte or 8-byte ID
        if (payload == null || payload.length < 5 || payload[0] != MAGIC_BYTE) {
            return null;
        }

        ApicurioConfig.IdHandlerMode mode = config.getIdHandlerMode();
        if (mode == ApicurioConfig.IdHandlerMode.EIGHT_BYTE) {
            if (payload.length < 9) {
                return null;
            }
            long id = ByteBuffer.wrap(payload, 1, 8).getLong();
            if (id <= 0) {
                return null;
            }
            return ApicurioWireReference.fromPayload(id, 8, encoding, msgType);
        }

        if (mode == ApicurioConfig.IdHandlerMode.FOUR_BYTE) {
            int id = ByteBuffer.wrap(payload, 1, 4).getInt();
            if (id <= 0) {
                return null;
            }
            return ApicurioWireReference.fromPayload(id, 4, encoding, msgType);
        }

        // AUTO mode: mirrors Apicurio's OptimisticFallbackIdHandler
        int firstFour = ByteBuffer.wrap(payload, 1, 4).getInt();
        if (firstFour == 0 && payload.length >= 9) {
            long id8 = ByteBuffer.wrap(payload, 1, 8).getLong();
            if (id8 > 0) {
                return ApicurioWireReference.fromPayload(id8, 8, encoding, msgType);
            }
            return null;
        }
        if (firstFour > 0) {
            return ApicurioWireReference.fromPayload(firstFour, 4, encoding, msgType);
        }
        return null;
    }

    private ResolvedApicurioSchema resolveSchema(ApicurioWireReference ref) throws IOException, RestClientException {
        if (ref.inlineSchema != null) {
            return client.parseInlineSchema(ref.inlineSchema, ref.inlineSchemaType);
        }
        if (ref.globalId != null) {
            return client.resolveByGlobalId(ref.globalId);
        }
        if (ref.contentId != null) {
            return client.resolveByContentId(ref.contentId);
        }
        if (ref.contentHash != null && !ref.contentHash.isBlank()) {
            return client.resolveByContentHash(ref.contentHash);
        }
        if (ref.artifactId != null && !ref.artifactId.isBlank()) {
            return client.resolveByCoordinates(ref.groupId, ref.artifactId, ref.version);
        }
        if (ref.payloadId != null) {
            ApicurioSchemaRegistryClient.IdLookupHint hint = resolvePayloadIdHint(ref.idSize);
            return client.resolveById(ref.payloadId, hint);
        }
        return null;
    }

    private ApicurioSchemaRegistryClient.IdLookupHint resolvePayloadIdHint(int idSize) {
        return switch (config.getUseId()) {
            case GLOBAL_ID -> ApicurioSchemaRegistryClient.IdLookupHint.GLOBAL_ID;
            case CONTENT_ID -> ApicurioSchemaRegistryClient.IdLookupHint.CONTENT_ID;
            case AUTO -> idSize == 8
                ? ApicurioSchemaRegistryClient.IdLookupHint.PREFER_GLOBAL_ID
                : ApicurioSchemaRegistryClient.IdLookupHint.PREFER_CONTENT_ID;
        };
    }

    private String deserializeAvro(
        Schema avroSchema,
        byte[] payload,
        int offset,
        int length,
        String encoding,
        AvroToJsonSerializer avroToJsonSerializer
    ) throws IOException {
        DatumReader<Object> reader = new GenericDatumReader<>(avroSchema);
        Decoder decoder;
        if ("JSON".equalsIgnoreCase(encoding)) {
            decoder = DecoderFactory.get().jsonDecoder(avroSchema, new ByteArrayInputStream(payload, offset, length));
        } else {
            decoder = DecoderFactory.get().binaryDecoder(payload, offset, length, null);
        }
        Object datum = reader.read(null, decoder);
        if (!(datum instanceof GenericRecord genericRecord)) {
            return String.valueOf(datum);
        }
        return avroToJsonSerializer.toJson(genericRecord);
    }

    private String deserializeJson(byte[] payload, int offset, int length) throws IOException {
        JsonNode node = JSON_MAPPER.readTree(new ByteArrayInputStream(payload, offset, length));
        return node != null ? node.toString() : "null";
    }

    private String deserializeProtobuf(
        ProtobufSchema protoSchema,
        byte[] payload,
        int offset,
        int length,
        String msgTypeHeader,
        AvroToJsonSerializer avroToJsonSerializer
    ) throws IOException {
        DynamicMessage message = parseProtobufMessage(protoSchema, payload, offset, length, msgTypeHeader);
        String printed = JsonFormat.printer().print(message);
        ObjectMapper mapper = avroToJsonSerializer != null ? avroToJsonSerializer.getMapper() : JSON_MAPPER;
        return mapper.readTree(printed).toString();
    }

    private DynamicMessage parseProtobufMessage(
        ProtobufSchema protoSchema,
        byte[] payload,
        int offset,
        int length,
        String msgTypeHeader
    ) throws IOException {
        // 1. Explicit msgType header (headers mode or explicit override): payload is raw protobuf bytes
        if (msgTypeHeader != null && !msgTypeHeader.isBlank()) {
            Descriptor descriptor = findDescriptor(protoSchema, msgTypeHeader);
            if (descriptor != null) {
                return DynamicMessage.parseFrom(descriptor, new ByteArrayInputStream(payload, offset, length));
            }
        }

        // 2. Try Apicurio length-delimited Ref prefix: message Ref { string name = 1; }
        ApicurioRefResult refResult = tryReadApicurioRef(payload, offset, length);
        if (refResult != null) {
            Descriptor descriptor = findDescriptor(protoSchema, refResult.name);
            if (descriptor != null) {
                int msgOffset = offset + refResult.bytesConsumed;
                int msgLength = length - refResult.bytesConsumed;
                return DynamicMessage.parseFrom(descriptor, new ByteArrayInputStream(payload, msgOffset, msgLength));
            }
        }

        // 3. Try Confluent message-indexes prefix (e.g., 0x00 or varint index array)
        try {
            ByteBuffer buffer = ByteBuffer.wrap(payload, offset, length);
            MessageIndexes indexes = MessageIndexes.readFrom(buffer);
            String messageName = protoSchema.toMessageName(indexes);
            if (messageName != null) {
                Descriptor descriptor = findDescriptor(protoSchema, messageName);
                if (descriptor != null) {
                    int remaining = buffer.remaining();
                    byte[] msgBytes = new byte[remaining];
                    buffer.get(msgBytes);
                    return DynamicMessage.parseFrom(descriptor, msgBytes);
                }
            }
        } catch (Exception ignored) {
            // Fall through to raw protobuf with first message descriptor
        }

        // 4. Fallback: raw protobuf bytes using the first message type defined in the schema
        Descriptor firstDescriptor = protoSchema.toDescriptor();
        if (firstDescriptor == null) {
            throw new IllegalStateException("No message descriptor found in Protobuf schema");
        }
        return DynamicMessage.parseFrom(firstDescriptor, new ByteArrayInputStream(payload, offset, length));
    }

    /**
     * Attempts to parse Apicurio's length-delimited {@code Ref { string name = 1; }} header.
     * Returns {@code null} if the bytes do not match a valid delimited Ref message.
     */
    static ApicurioRefResult tryReadApicurioRef(byte[] payload, int offset, int length) {
        if (length < 3) {
            return null;
        }
        try {
            CodedInputStream cis = CodedInputStream.newInstance(payload, offset, length);
            int delimitedSize = cis.readRawVarint32();
            int headerBytes = cis.getTotalBytesRead();
            if (delimitedSize <= 0 || headerBytes + delimitedSize > length) {
                return null;
            }
            int limit = cis.pushLimit(delimitedSize);
            String name = null;
            boolean sawUnknown = false;
            while (!cis.isAtEnd()) {
                int tag = cis.readTag();
                if (tag == PROTOBUF_REF_NAME_TAG) {
                    name = cis.readStringRequireUtf8();
                } else {
                    sawUnknown = true;
                    break;
                }
            }
            cis.popLimit(limit);
            if (!sawUnknown && name != null && !name.isBlank() && cis.getTotalBytesRead() == headerBytes + delimitedSize) {
                return new ApicurioRefResult(name, cis.getTotalBytesRead());
            }
        } catch (Exception ignored) {
            // Not an Apicurio Ref prefix
        }
        return null;
    }

    static Descriptor findDescriptor(ProtobufSchema protoSchema, String messageName) {
        if (messageName == null || messageName.isBlank()) {
            return null;
        }
        try {
            Descriptor direct = protoSchema.toDescriptor(messageName);
            if (direct != null) {
                return direct;
            }
        } catch (Exception ignored) {
            // Try searching FileDescriptor directly
        }

        Descriptor root = protoSchema.toDescriptor();
        if (root == null) {
            return null;
        }
        FileDescriptor fileDescriptor = root.getFile();
        Descriptor bySimpleName = fileDescriptor.findMessageTypeByName(messageName);
        if (bySimpleName != null) {
            return bySimpleName;
        }

        // Handle Java class name or nested/qualified name (e.g. "pkg.Outer$Inner" or "pkg.Message")
        String simpleName = messageName;
        int dollarIdx = simpleName.lastIndexOf('$');
        if (dollarIdx >= 0 && dollarIdx < simpleName.length() - 1) {
            simpleName = simpleName.substring(dollarIdx + 1);
        } else {
            int dotIdx = simpleName.lastIndexOf('.');
            if (dotIdx >= 0 && dotIdx < simpleName.length() - 1) {
                simpleName = simpleName.substring(dotIdx + 1);
            }
        }
        return findDescriptorRecursively(fileDescriptor.getMessageTypes(), messageName, simpleName);
    }

    private static Descriptor findDescriptorRecursively(List<Descriptor> descriptors, String fullOrName, String simpleName) {
        for (Descriptor d : descriptors) {
            if (d.getFullName().equals(fullOrName) || d.getName().equals(fullOrName) || d.getName().equals(simpleName)) {
                return d;
            }
            Descriptor nested = findDescriptorRecursively(d.getNestedTypes(), fullOrName, simpleName);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static String readStringHeader(Headers headers, String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }
        Header header = headers.lastHeader(headerName);
        if (header == null || header.value() == null) {
            return null;
        }
        String value = new String(header.value(), StandardCharsets.UTF_8);
        return value.isBlank() ? null : value;
    }

    private static Long readLongHeader(Headers headers, String headerName) {
        if (headers == null || headerName == null) {
            return null;
        }
        Header header = headers.lastHeader(headerName);
        if (header == null || header.value() == null || header.value().length == 0) {
            return null;
        }
        byte[] value = header.value();
        if (value.length == 8) {
            return ByteBuffer.wrap(value).getLong();
        }
        if (value.length == 4) {
            return (long) ByteBuffer.wrap(value).getInt();
        }
        try {
            return Long.parseLong(new String(value, StandardCharsets.UTF_8).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    record ApicurioRefResult(String name, int bytesConsumed) {}

    record ApicurioWireReference(
        Long globalId,
        Long contentId,
        Long payloadId,
        int payloadOffset,
        int idSize,
        String contentHash,
        String groupId,
        String artifactId,
        String version,
        String inlineSchema,
        String inlineSchemaType,
        String encoding,
        String msgType
    ) {
        static ApicurioWireReference fromPayload(long id, int idSize, String encoding, String msgType) {
            return new ApicurioWireReference(
                null,
                null,
                id,
                1 + idSize,
                idSize,
                null,
                null,
                null,
                null,
                null,
                null,
                encoding,
                msgType
            );
        }
    }
}

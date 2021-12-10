package org.akhq.repositories;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.akhq.configs.SchemaRegistryType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Singleton;

/**
 * Converts an avro payload to the kafka avro wire format (https://docs.confluent.io/current/schema-registry/serializer-formatter.html#wire-format)
 * Some producers (like Spring Cloud Stream) do write this wire format, but use the raw avro binary encoding (without magic byte and schema id)
 * and put the reference to the schema in a header field. This converter will add the magic byte and schema id to the byte[] to
 * be wire format compatible if the following conditions are met:
 * - magic byte is not already present
 * - schema reference (subject and version) can be found in the message header
 * - schema can be fetch from the registry
 */
@Singleton
public class AvroWireFormatConverter {

    private static final Pattern AVRO_CONTENT_TYPE_PATTERN = Pattern.compile("\"?application/vnd\\.(.+)\\.v(\\d+)\\+avro\"?");

    public byte[] convertValueToWireFormat(ConsumerRecord<byte[], byte[]> record, SchemaRegistryClient registryClient, SchemaRegistryType schemaRegistryType) {
        byte magicByte = 0x0;
        if (schemaRegistryType == SchemaRegistryType.TIBCO) {
            magicByte = (byte) 0x80;
        }
        Iterator<Header> contentTypeIter = record.headers().headers("contentType").iterator();
        byte[] value = record.value();
        if (contentTypeIter.hasNext() &&
                value.length > 0 &&
                ByteBuffer.wrap(value).get() != magicByte) {
            String headerValue = new String(contentTypeIter.next().value());
            Matcher matcher = AVRO_CONTENT_TYPE_PATTERN.matcher(headerValue);
            if (matcher.matches()) {
                String subject = matcher.group(1);
                int version = Integer.parseInt(matcher.group(2));
                value = prependWireFormatHeader(value, registryClient, subject, version, magicByte);
            }
        }
        return value;
    }

    private byte[] prependWireFormatHeader(byte[] value, SchemaRegistryClient registryClient, String subject, int version, byte magicByte) {
        try {
            SchemaMetadata schemaMetadata = registryClient.getSchemaMetadata(subject, version);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(magicByte);
            out.write(ByteBuffer.allocate(4).putInt(schemaMetadata.getId()).array());
            out.write(value);
            value = out.toByteArray();
        } catch (IOException | RestClientException e) {
            // ignore on purpose, dont prepend anything
        }
        return value;
    }
}

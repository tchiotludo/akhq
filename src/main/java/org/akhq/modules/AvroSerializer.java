package org.akhq.modules;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.SchemaRegistryType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.akhq.configs.Connection;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Singleton
@Slf4j
public class AvroSerializer {

    private final int MAGIC_BYTE;

    public static final int SCHEMA_ID_SIZE = 4;
    private SchemaRegistryClient registryClient;

    public AvroSerializer(SchemaRegistryClient registryClient, SchemaRegistryType schemaRegistryType) {
        this.registryClient = registryClient;
        if (schemaRegistryType == SchemaRegistryType.TIBCO) {
            MAGIC_BYTE = (byte) 0x80;
        } else {
            MAGIC_BYTE = 0x0;
        }
    }

    public byte[] toAvro(String json, int schemaId) {
        byte[] asBytes;
        try {
            Schema schema = this.registryClient.getById(schemaId);
            asBytes = this.fromJsonToAvro(json.trim(), schema, schemaId);
        } catch (IOException | RestClientException e) {
            throw new RuntimeException(String.format("Can't retrieve schema %d in registry", schemaId), e);
        }
        return asBytes;
    }

    private byte[] fromJsonToAvro(String json, Schema schema, int schemaId) throws IOException {
        log.trace("encoding message {} with schema {} and id {}",json, schema, schemaId);
        InputStream input = new ByteArrayInputStream(json.getBytes());
        DataInputStream din = new DataInputStream(input);

        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, din);

        DatumReader<Object> reader = new GenericDatumReader<>(schema);
        Object datum = reader.read(null, decoder);

        GenericDatumWriter<Object> w = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(MAGIC_BYTE);
        outputStream.write(ByteBuffer.allocate(SCHEMA_ID_SIZE).putInt(schemaId).array());

        Encoder e = EncoderFactory.get().binaryEncoder(outputStream, null);

        w.write(datum, e);
        e.flush();

        return outputStream.toByteArray();
    }
}

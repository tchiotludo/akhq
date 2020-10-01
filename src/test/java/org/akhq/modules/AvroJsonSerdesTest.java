package org.akhq.modules;

import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.akhq.Breed;
import org.akhq.Cat;
import org.akhq.Dog;
import org.akhq.PetOwner;
import org.akhq.utils.avroserdes.AvroSerializer;
import org.akhq.utils.avroserdes.AvroToJsonSerializer;
import org.apache.avro.Conversion;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AvroJsonSerdesTest {

    @Mock
    private static MockSchemaRegistryClient registryClient = new MockSchemaRegistryClient();
    private static AvroSerializer avroSerializer = new AvroSerializer(registryClient);

    private static final Conversion<BigDecimal> DECIMAL_CONVERSION = new Conversions.DecimalConversion();

    @BeforeAll
    private static void setUp() throws IOException, RestClientException {
        registryClient.register(Breed.SCHEMA$.getName(), Breed.SCHEMA$);
        registryClient.register(Dog.SCHEMA$.getName(), Dog.SCHEMA$);
        registryClient.register(Cat.SCHEMA$.getName(), Cat.SCHEMA$);
        registryClient.register(PetOwner.SCHEMA$.getName(), PetOwner.SCHEMA$);
    }

    @Test
    public void serdesWithoutDate() throws IOException, RestClientException {
        String expectedString = "{\"id\":10,\"name\":\"Tom\",\"breed\":\"SPHYNX\"}";
        GenericRecord catExample = aCatExample(10, "Tom", Breed.SPHYNX);

        testJsonSerialisationAndBack(catExample, expectedString);
    }

    @Test
    public void serdesWithDecimalAndDate() throws IOException, RestClientException {
        String expectedString = "{\"id\":10,\"name\":\"Tiger\",\"weight\":\"10.40\",\"birthdate\":1601337600}";
        GenericRecord dogExample = aDogExample(10, "Tiger", 10.40, 1601337600);

        testJsonSerialisationAndBack(dogExample, expectedString);
    }

    @Test
    public void serdesWithDecimalAndDateAndUnions() throws IOException, RestClientException {
        GenericRecord dogExample1 = aDogExample(10, "Alfie", 10.40, 1601510400);
        GenericRecord dogExample2 = aDogExample(11, "Bella", 32.89, 1601424000);
        GenericRecord catExample1 = aCatExample(12, "Charlie", Breed.ABYSSINIAN);
        GenericRecord catExample2 = aCatExample(12, "Daisy", Breed.AMERICAN_SHORTHAIR);

        GenericRecord petOwnerExample = aPetOwnerExample(1, "Omega",
                List.of(dogExample1, dogExample2, catExample1, catExample2));
        String expectedString = "{\"id\":1,\"name\":\"Omega\",\"pets\":[{\"org.akhq.Dog\":{\"id\":10,\"name\":\"Alfie\",\"weight\":\"10.40\",\"birthdate\":1601510400}},{\"org.akhq.Dog\":{\"id\":11,\"name\":\"Bella\",\"weight\":\"32.89\",\"birthdate\":1601424000}},{\"org.akhq.Cat\":{\"id\":12,\"name\":\"Charlie\",\"breed\":\"ABYSSINIAN\"}},{\"org.akhq.Cat\":{\"id\":12,\"name\":\"Daisy\",\"breed\":\"AMERICAN_SHORTHAIR\"}}]}";

        testJsonSerialisationAndBack(petOwnerExample, expectedString);
    }


    private void testJsonSerialisationAndBack(GenericRecord record, String expectedString) throws IOException, RestClientException {
        int schemaId = registryClient.getId(record.getSchema().getName(), record.getSchema());
        // Compare JSON
        assertEquals(expectedString, AvroToJsonSerializer.toJson(record));
        // Compare Avro bytes
        assertArrayEquals(
                fromGenericRecordToEncodedBytes(record, schemaId),
                avroSerializer.toAvro(expectedString, schemaId));
    }

    private GenericRecord aCatExample(int id, String name, Breed breed) {
        return new GenericRecordBuilder(Cat.SCHEMA$)
                .set("id", id)
                .set("name", name)
                .set("breed", breed)
                .build();
    }

    private GenericRecord aDogExample(int id, String name, double weight, int birthdate) {
        Schema.Field weightField = Dog.SCHEMA$.getField("weight");
        return new GenericRecordBuilder(Dog.SCHEMA$)
                .set("id", id)
                .set("name", name)
                .set("weight", DECIMAL_CONVERSION.toBytes(BigDecimal.valueOf(weight).setScale(2), weightField.schema(), weightField.schema().getLogicalType()))
                .set("birthdate", birthdate)
                .build();
    }

    private GenericRecord aPetOwnerExample(int id, String name, List<Object> pets) {
        return PetOwner.newBuilder()
                .setId(id)
                .setName(name)
                .setPets(pets)
                .build();
    }

    // This is Avro's standard conversion from GenericRecord to bytes
    private static byte[] fromGenericRecordToEncodedBytes(GenericRecord datum, int schemaId) throws IOException {
        GenericDatumWriter<Object> w = new GenericDatumWriter<>(datum.getSchema());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(AvroSerializer.MAGIC_BYTE);
        outputStream.write(ByteBuffer.allocate(AvroSerializer.SCHEMA_ID_SIZE).putInt(schemaId).array());

        Encoder e = EncoderFactory.get().binaryEncoder(outputStream, null);

        w.write(datum, e);
        e.flush();

        return outputStream.toByteArray();
    }

}

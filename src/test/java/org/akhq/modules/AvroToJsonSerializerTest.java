package org.akhq.modules;

import org.akhq.Breed;
import org.akhq.Cat;
import org.akhq.Dog;
import org.akhq.utils.AvroToJsonSerializer;
import org.apache.avro.Conversion;
import org.apache.avro.Conversions;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AvroToJsonSerializerTest {

    private static final Conversion<BigDecimal> DECIMAL_CONVERSION = new Conversions.DecimalConversion();

    @Test
    public void serializeAvroToJsonWithDecimal() throws IOException {
        String expectedString = "{\"id\":10,\"name\":\"Tiger\",\"weight\":\"10.40\"}";
        GenericRecord dogExample = aDogExample(10, "Tiger", 10.40);
        assertEquals(AvroToJsonSerializer.toJson(dogExample), expectedString);
    }

    @Test
    public void serializeAvroToJsonWithoutDecimal() throws IOException {
        String expectedString = "{\"id\":10,\"name\":\"Tom\",\"breed\":\"SPHYNX\"}";
        GenericRecord catExample = aCatExample(10, "Tom", Breed.SPHYNX);
        assertEquals(AvroToJsonSerializer.toJson(catExample), expectedString);
    }

    private GenericRecord aCatExample(int id, String name, Breed breed) {
        return new GenericRecordBuilder(Cat.SCHEMA$)
                .set("id", id)
                .set("name", name)
                .set("breed", breed)
                .build();
    }

    private GenericRecord aDogExample(int id, String name, double weight) {
        Schema.Field weightField = Dog.SCHEMA$.getField("weight");
        return new GenericRecordBuilder(Dog.SCHEMA$)
                .set("id", id)
                .set("name", name)
                .set("weight", DECIMAL_CONVERSION.toBytes(BigDecimal.valueOf(weight).setScale(2), weightField.schema(), weightField.schema().getLogicalType()))
                .build();
    }

}

package org.akhq.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.avro.Schema;

import java.io.IOException;

public class AvroSchemaSerializer extends JsonSerializer<Schema> {
    @Override
    public void serialize(
        Schema value,
        JsonGenerator gen,
        SerializerProvider provider
    ) throws IOException {
        gen.writeString(value.toString());
    }

}

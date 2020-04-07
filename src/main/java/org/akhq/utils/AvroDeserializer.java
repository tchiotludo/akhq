package org.akhq.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.avro.Schema;

import java.io.IOException;

public class AvroDeserializer extends JsonDeserializer<Schema> {
    @Override
    public Schema deserialize(
        JsonParser p,
        DeserializationContext context
    ) throws IOException {
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(p.getValueAsString());
    }
}

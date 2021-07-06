package org.akhq.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;
import java.util.Map;
import java.util.TimeZone;

public class AvroToJsonSerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module())
        .setTimeZone(TimeZone.getDefault());

    public static String toJson(GenericRecord record) throws IOException {
        Map<String, Object> map = AvroDeserializer.recordDeserializer(record);

        return MAPPER.writeValueAsString(map);
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }
}

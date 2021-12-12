package org.akhq.utils;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import org.apache.avro.generic.GenericRecord;

@Singleton
public class AvroToJsonSerializer {
    private final ObjectMapper mapper;

    public AvroToJsonSerializer(@Value("${akhq.avro-serializer.json.serialization.inclusions}") @Nullable List<Include> jsonInclusions) {
        List<Include> inclusions = jsonInclusions != null ? jsonInclusions : Collections.emptyList();
        this.mapper = createObjectMapper(inclusions);
    }

    private ObjectMapper createObjectMapper(List<Include> jsonInclusions) {
        ObjectMapper objectMapper = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setTimeZone(TimeZone.getDefault());
        for (Include include : jsonInclusions) {
            objectMapper = objectMapper.setSerializationInclusion(include);
        }
        return objectMapper;
    }

    public String toJson(GenericRecord record) throws IOException {
        Map<String, Object> map = AvroDeserializer.recordDeserializer(record);
        return mapper.writeValueAsString(map);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}

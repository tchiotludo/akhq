package org.akhq.modules.schemaregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.akhq.configs.SchemaRegistryType;
import org.akhq.utils.Album;
import org.akhq.utils.ResourceTestUtil;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaSerializerTest {

    @Test
    public void serializeJsonStringWithMagicByteAndSchemaId() throws IOException {
        JsonSchema jsonSchema = createJsonSchema("json_schema/album.json");
        JsonSchemaSerializer jsonSchemaSerializer = JsonSchemaSerializer.newInstance(1, jsonSchema, SchemaRegistryType.CONFLUENT);
        Album objectSatisfyingJsonSchema = new Album("title", List.of("artist_1", "artist_2"), 1989, List.of("song_1", "song_2"));
        String recordAsJsonString = new ObjectMapper().writeValueAsString(objectSatisfyingJsonSchema);

        byte[] serialize = jsonSchemaSerializer.serialize(recordAsJsonString);

        assertEquals(SchemaRegistryType.CONFLUENT.getMagicByte(), serialize[0]);
    }

    @Test
    public void failsWhenObjectDoesNotAdhereToSchema() throws IOException {
        JsonSchema jsonSchema = createJsonSchema("json_schema/album.json");
        JsonSchemaSerializer jsonSchemaSerializer = JsonSchemaSerializer.newInstance(1, jsonSchema, SchemaRegistryType.CONFLUENT);

        JSONObject notSchemaValidObject = new JSONObject(Collections.singletonMap("any_property", "property value"));
        try {
            jsonSchemaSerializer.serialize(notSchemaValidObject.toString());
            fail("Exception should be thrown");
        } catch (Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
        }
    }

    private JsonSchema createJsonSchema(String resourcePath) throws IOException {
        String schemaAsString = ResourceTestUtil.resourceAsString(resourcePath);
        return new JsonSchema(schemaAsString);
    }

}
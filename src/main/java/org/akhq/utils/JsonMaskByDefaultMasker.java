package org.akhq.utils;

import com.google.gson.*;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.akhq.configs.DataMasking;
import org.akhq.configs.JsonMaskingFilter;
import org.akhq.models.Record;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "json_mask_by_default")
public class JsonMaskByDefaultMasker implements Masker {

    private final Map<String, List<String>> topicToKeysMap;
    private final String jsonMaskReplacement;
    private static final String NON_JSON_MESSAGE = "This record is unable to be masked as it is not a structured object. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data.";
    private static final String ERROR_MESSAGE = "An exception occurred during an attempt to mask this record. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data.";

    public JsonMaskByDefaultMasker(DataMasking dataMasking) {
        this.jsonMaskReplacement = dataMasking.getJsonMaskReplacement();
        this.topicToKeysMap = buildTopicKeysMap(dataMasking);
    }

    private Map<String, List<String>> buildTopicKeysMap(DataMasking dataMasking) {
        return dataMasking.getJsonFilters().stream()
            .collect(Collectors.toMap(
                JsonMaskingFilter::getTopic,
                JsonMaskingFilter::getKeys,
                (a, b) -> a,
                HashMap::new
            ));
    }

    public Record maskRecord(Record record) {
        if (!isJson(record)) {
            return createNonJsonRecord(record);
        }

        try {
            List<String> unmaskedKeys = getUnmaskedKeysForTopic(record.getTopic().getName());
            return applyMasking(record, unmaskedKeys);
        } catch (Exception e) {
            logMaskingError(record, e);
            return createErrorRecord(record);
        }
    }

    private List<String> getUnmaskedKeysForTopic(String topic) {
        return topicToKeysMap.getOrDefault(topic.toLowerCase(), Collections.emptyList());
    }

    private Record createNonJsonRecord(Record record) {
        record.setValue(NON_JSON_MESSAGE);
        return record;
    }

    private Record createErrorRecord(Record record) {
        record.setValue(ERROR_MESSAGE);
        return record;
    }

    private void logMaskingError(Record record, Exception e) {
        LOG.error("Error masking record at topic {}, partition {}, offset {} due to {}",
            record.getTopic(), record.getPartition(), record.getOffset(), e.getMessage());
    }

    @SneakyThrows
    private Record applyMasking(Record record, List<String> unmaskedKeys) {
        JsonObject root = JsonParser.parseString(record.getValue()).getAsJsonObject();
        maskJson(root, "", unmaskedKeys);
        record.setValue(root.toString());
        return record;
    }

    private void maskJson(JsonElement element, String path, List<String> unmaskedKeys) {
        if (element.isJsonObject()) {
            maskJsonObject(element.getAsJsonObject(), path, unmaskedKeys);
        } else if (element.isJsonArray()) {
            maskJsonArray(element.getAsJsonArray(), path, unmaskedKeys);
        }
    }

    private void maskJsonObject(JsonObject obj, String path, List<String> unmaskedKeys) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String newPath = path + entry.getKey();
            JsonElement value = entry.getValue();

            if (shouldMaskPrimitive(value, newPath, unmaskedKeys)) {
                entry.setValue(new JsonPrimitive(jsonMaskReplacement));
            } else if (isNestedStructure(value)) {
                maskJson(value, newPath + ".", unmaskedKeys);
            }
        }
    }

    private void maskJsonArray(JsonArray array, String path, List<String> unmaskedKeys) {
        boolean shouldMask = !unmaskedKeys.contains(path.substring(0, path.length() - 1));

        for (int i = 0; i < array.size(); i++) {
            JsonElement arrayElement = array.get(i);
            if (arrayElement.isJsonPrimitive() && shouldMask) {
                array.set(i, new JsonPrimitive(jsonMaskReplacement));
            } else if (isNestedStructure(arrayElement)) {
                maskJson(arrayElement, path, unmaskedKeys);
            }
        }
    }

    private boolean shouldMaskPrimitive(JsonElement value, String path, List<String> unmaskedKeys) {
        return value.isJsonPrimitive() && !unmaskedKeys.contains(path);
    }

    private boolean isNestedStructure(JsonElement value) {
        return value.isJsonObject() || value.isJsonArray();
    }
}

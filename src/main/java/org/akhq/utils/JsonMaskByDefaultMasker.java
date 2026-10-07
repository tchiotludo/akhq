package org.akhq.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.akhq.configs.DataMasking;
import org.akhq.models.Record;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "json_mask_by_default")
public class JsonMaskByDefaultMasker extends JsonMasker {

    private static final String NON_JSON_MESSAGE = "This record is unable to be masked as it is not a structured object. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data.";
    private static final String ERROR_MESSAGE = "An exception occurred during an attempt to mask this record. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data.";

    public JsonMaskByDefaultMasker(DataMasking dataMasking) {
        super(dataMasking);
    }

    public Record maskRecord(Record record) {
        if (!isJson(record)) {
            record.setValue(NON_JSON_MESSAGE);
            return record;
        }

        try {
            List<String> keysToUnmask = getKeysForTopic(record.getTopic().getName());

            if (keysToUnmask.contains("*")) {
                return record;
            }

            List<String> wildCardKeys = keysToUnmask.stream()
                .filter(key -> key.endsWith(".*"))
                .map(key -> key.substring(0, key.length() - 2))
                .toList();
            List<String> nonWildCardKeys = keysToUnmask.stream().filter(key -> !key.endsWith(".*")).toList();
            return applyMasking(record, nonWildCardKeys, wildCardKeys);

        } catch (Exception e) {
            LOG.error("Error masking record at topic {}, partition {}, offset {} due to {}",
                record.getTopic(), record.getPartition(), record.getOffset(), e.getMessage());
            record.setValue(ERROR_MESSAGE);
            return record;
        }
    }

    @SneakyThrows
    private Record applyMasking(
        Record record,
        List<String> keysToUnmask,
        List<String> wildcardKeysToUnmask
    ) {
        JsonElement root = JsonParser.parseString(record.getValue());
        maskJson(root, "", keysToUnmask, wildcardKeysToUnmask, false);
        record.setValue(root.toString());
        return record;
    }

    private void maskJson(
        JsonElement element,
        String path,
        List<String> keysToUnmask,
        List<String> wildcardKeysToUnmask,
        Boolean unmaskAll
    ) {
        if (element.isJsonObject()) {
            maskJsonObject(element.getAsJsonObject(), path, keysToUnmask, wildcardKeysToUnmask, unmaskAll);
        } else if (element.isJsonArray()) {
            maskJsonArray(element.getAsJsonArray(), path, keysToUnmask, wildcardKeysToUnmask, unmaskAll);
        }
    }

    private void maskJsonObject(
        JsonObject obj,
        String path,
        List<String> keysToUnmask,
        List<String> wildcardKeysToUnmask,
        Boolean unmaskAll
    ) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String newPath = path + entry.getKey();
            JsonElement value = entry.getValue();
            Boolean wildcardMatch = unmaskAll || wildcardKeysToUnmask.contains(newPath);

            if (shouldMaskPrimitive(value, newPath, keysToUnmask, wildcardMatch)) {
                entry.setValue(new JsonPrimitive(jsonMaskReplacement));
            } else if (isNestedStructure(value)) {
                maskJson(value, newPath + ".", keysToUnmask, wildcardKeysToUnmask, wildcardMatch);
            }
        }
    }

    private void maskJsonArray(
        JsonArray array,
        String path,
        List<String> keysToUnmask,
        List<String> wildcardKeysToUnmask,
        Boolean unmaskAll
    ) {
        String arrayPath = path.substring(0, path.length() - 1);
        boolean unmaskAllFromNow = unmaskAll || wildcardKeysToUnmask.contains(arrayPath);
        boolean shouldMask = !unmaskAllFromNow && !keysToUnmask.contains(arrayPath);

        for (int i = 0; i < array.size(); i++) {
            JsonElement arrayElement = array.get(i);
            if (arrayElement.isJsonPrimitive() && shouldMask) {
                array.set(i, new JsonPrimitive(jsonMaskReplacement));
            } else if (isNestedStructure(arrayElement)) {
                maskJson(arrayElement, path, keysToUnmask, wildcardKeysToUnmask, unmaskAllFromNow);
            }
        }
    }

    private boolean shouldMaskPrimitive(JsonElement value, String path, List<String> keysToUnmask, Boolean wildcardMatch) {
        return value.isJsonPrimitive() && !wildcardMatch && !keysToUnmask.contains(path);
    }

    private boolean isNestedStructure(JsonElement value) {
        return value.isJsonObject() || value.isJsonArray();
    }
}

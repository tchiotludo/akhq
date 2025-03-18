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
            return applyMasking(record, keysToUnmask);
        } catch (Exception e) {
            LOG.error("Error masking record at topic {}, partition {}, offset {} due to {}",
                record.getTopic(), record.getPartition(), record.getOffset(), e.getMessage());
            record.setValue(ERROR_MESSAGE);
            return record;
        }
    }

    @SneakyThrows
    private Record applyMasking(Record record, List<String> keysToUnmask) {
        JsonElement root = JsonParser.parseString(record.getValue());
        maskJson(root, "", keysToUnmask);
        record.setValue(root.toString());
        return record;
    }

    private void maskJson(JsonElement element, String path, List<String> keysToUnmask) {
        if (element.isJsonObject()) {
            maskJsonObject(element.getAsJsonObject(), path, keysToUnmask);
        } else if (element.isJsonArray()) {
            maskJsonArray(element.getAsJsonArray(), path, keysToUnmask);
        }
    }

    private void maskJsonObject(JsonObject obj, String path, List<String> keysToUnmask) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String newPath = path + entry.getKey();
            JsonElement value = entry.getValue();

            if (shouldMaskPrimitive(value, newPath, keysToUnmask)) {
                entry.setValue(new JsonPrimitive(jsonMaskReplacement));
            } else if (isNestedStructure(value)) {
                maskJson(value, newPath + ".", keysToUnmask);
            }
        }
    }

    private void maskJsonArray(JsonArray array, String path, List<String> keysToUnmask) {
        boolean shouldMask = !keysToUnmask.contains(path.substring(0, path.length() - 1));

        for (int i = 0; i < array.size(); i++) {
            JsonElement arrayElement = array.get(i);
            if (arrayElement.isJsonPrimitive() && shouldMask) {
                array.set(i, new JsonPrimitive(jsonMaskReplacement));
            } else if (isNestedStructure(arrayElement)) {
                maskJson(arrayElement, path, keysToUnmask);
            }
        }
    }

    private boolean shouldMaskPrimitive(JsonElement value, String path, List<String> keysToUnmask) {
        return value.isJsonPrimitive() && !keysToUnmask.contains(path);
    }

    private boolean isNestedStructure(JsonElement value) {
        return value.isJsonObject() || value.isJsonArray();
    }
}

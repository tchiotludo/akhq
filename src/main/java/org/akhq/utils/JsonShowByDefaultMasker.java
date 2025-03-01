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

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "json_show_by_default")
public class JsonShowByDefaultMasker extends JsonMasker {

    private static final String ERROR_MESSAGE = "Error masking record";

    public JsonShowByDefaultMasker(DataMasking dataMasking) {
        super(dataMasking);
    }

    @Override
    public Record maskRecord(Record record) {
        try {
            if (!isJson(record)) {
                return record;
            }
            String topic = record.getTopic().getName().toLowerCase();
            List<String> keysToMask = getKeysForTopic(topic);
            return keysToMask.isEmpty() ? record : applyMasking(record, keysToMask);
        } catch (Exception e) {
            LOG.error(ERROR_MESSAGE, e);
            return record;
        }
    }

    @SneakyThrows
    private Record applyMasking(Record record, List<String> keysToMask) {
        JsonElement root = JsonParser.parseString(record.getValue());
        String[][] pathArrays = keysToMask
            .stream()
            .map(key -> key.split("\\."))
            .toArray(String[][]::new);
        maskPaths(root, pathArrays);
        record.setValue(root.toString());
        return record;
    }

    private void maskPaths(JsonElement root, String[][] pathArrays) {
        for (String[] path : pathArrays) {
            maskJson(root, path, 0);
        }
    }

    private void maskJson(JsonElement element, String[] path, int index) {
        if (index == path.length) return;

        String currentKey = path[index];
        if (element.isJsonObject()) {
            handleJsonObject(element.getAsJsonObject(), path, index, currentKey);
        } else if (element.isJsonArray()) {
            handleJsonArray(element.getAsJsonArray(), path, index);
        }
    }

    private void handleJsonObject(JsonObject obj, String[] path, int index, String currentKey) {
        if (!obj.has(currentKey)) return;

        if (index == path.length - 1) {
            maskTargetElement(obj, currentKey);
        } else {
            maskJson(obj.get(currentKey), path, index + 1);
        }
    }

    private void handleJsonArray(JsonArray array, String[] path, int index) {
        for (int i = 0; i < array.size(); i++) {
            JsonElement arrayElement = array.get(i);
            if (arrayElement.isJsonObject()) {
                maskJson(arrayElement, path, index);
            }
        }
    }

    private void maskTargetElement(JsonObject obj, String currentKey) {
        JsonElement target = obj.get(currentKey);
        if (target.isJsonArray()) {
            maskArrayElement(target.getAsJsonArray());
        } else {
            obj.addProperty(currentKey, jsonMaskReplacement);
        }
    }

    private void maskArrayElement(JsonArray array) {
        for (int i = 0; i < array.size(); i++) {
            array.set(i, new JsonPrimitive(jsonMaskReplacement));
        }
    }
}

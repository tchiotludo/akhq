package org.akhq.utils;

import com.google.gson.*;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.akhq.configs.DataMasking;
import org.akhq.configs.JsonMaskingFilter;
import org.akhq.models.Record;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "json_show_by_default")
public class JsonShowByDefaultMasker implements Masker {

    private final Map<String, List<String>> topicToKeysMap;
    private final String jsonMaskReplacement;
    private static final String ERROR_MESSAGE = "Error masking record";

    public JsonShowByDefaultMasker(DataMasking dataMasking) {
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
        try {
            if (!isJson(record)) {
                return record;
            }
            return maskJsonRecord(record);
        } catch (Exception e) {
            LOG.error(ERROR_MESSAGE, e);
            return record;
        }
    }

    private Record maskJsonRecord(Record record) {
        String topic = record.getTopic().getName().toLowerCase();
        List<String> maskedKeys = topicToKeysMap.get(topic);
        return maskedKeys != null ? applyMasking(record, maskedKeys) : record;
    }

    @SneakyThrows
    private Record applyMasking(Record record, List<String> maskedKeys) {
        JsonObject root = JsonParser.parseString(record.getValue()).getAsJsonObject();
        String[][] pathArrays = preProcessPaths(maskedKeys);
        maskPaths(root, pathArrays);
        record.setValue(root.toString());
        return record;
    }

    private String[][] preProcessPaths(List<String> maskedKeys) {
        return maskedKeys.stream()
            .map(key -> key.split("\\."))
            .toArray(String[][]::new);
    }

    private void maskPaths(JsonObject root, String[][] pathArrays) {
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

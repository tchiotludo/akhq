package org.akhq.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import lombok.SneakyThrows;
import org.akhq.configs.DataMasking;
import org.akhq.configs.JsonMaskingFilter;
import org.akhq.models.Record;

import java.util.List;

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "json_show_by_default")
public class JsonShowByDefaultMasker implements Masker {

    private final List<JsonMaskingFilter> jsonMaskingFilters;
    private final String jsonMaskReplacement;

    public JsonShowByDefaultMasker(DataMasking dataMasking) {
        this.jsonMaskingFilters = dataMasking.getJsonFilters();
        this.jsonMaskReplacement = dataMasking.getJsonMaskReplacement();
    }

    public Record maskRecord(Record record) {
        try {
            if(isTombstone(record)) {
                return record;
            } else if(isJson(record)) {
                return jsonMaskingFilters
                    .stream()
                    .filter(jsonMaskingFilter -> record.getTopic().getName().equalsIgnoreCase(jsonMaskingFilter.getTopic()))
                    .findFirst()
                    .map(filter -> applyMasking(record, filter.getKeys()))
                    .orElse(record);
            } else {
                return record;
            }
        } catch (Exception e) {
            LOG.error("Error masking record", e);
        }
        return record;
    }

    @SneakyThrows
    private Record applyMasking(Record record, List<String> keys) {
        JsonObject jsonElement = JsonParser.parseString(record.getValue()).getAsJsonObject();
        for(String key : keys) {
            maskField(jsonElement, key.split("\\."), 0);
        }
        record.setValue(jsonElement.toString());
        return record;
    }

    private void maskField(JsonObject node, String[] keys, int index) {
        if (index == keys.length - 1) {
            if (node.has(keys[index])) {
                node.addProperty(keys[index], jsonMaskReplacement);
            }
        } else {
            JsonElement childNode = node.get(keys[index]);
            if (childNode != null && childNode.isJsonObject()) {
                maskField(childNode.getAsJsonObject(), keys, index + 1);
            }
        }
    }
}
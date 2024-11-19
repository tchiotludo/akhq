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
import java.util.Map;

@Singleton
@Requires(property = "akhq.security.data-masking.mode", value = "json_mask_by_default")
public class JsonMaskByDefaultMasker implements Masker {

    private final List<JsonMaskingFilter> jsonMaskingFilters;
    private final String jsonMaskReplacement;

    public JsonMaskByDefaultMasker(DataMasking dataMasking) {
        this.jsonMaskingFilters = dataMasking.getJsonFilters();
        this.jsonMaskReplacement = dataMasking.getJsonMaskReplacement();
    }

    public Record maskRecord(Record record) {
        try {
            if(isJson(record)) {
                return jsonMaskingFilters
                    .stream()
                    .filter(jsonMaskingFilter -> record.getTopic().getName().equalsIgnoreCase(jsonMaskingFilter.getTopic()))
                    .findFirst()
                    .map(filter -> applyMasking(record, filter.getKeys()))
                    .orElseGet(() -> applyMasking(record, List.of()));
            }
        } catch (Exception e) {
            LOG.error("Error masking record at topic {}, partition {}, offset {} due to {}", record.getTopic(), record.getPartition(), record.getOffset(), e.getMessage());
            record.setValue("An exception occurred during an attempt to mask this record. This record is unavailable to view due to safety measures from json_mask_by_default to not leak sensitive data. Please contact akhq administrator.");
        }
        return record;
    }

    @SneakyThrows
    private Record applyMasking(Record record, List<String> keys) {
        JsonObject jsonElement = JsonParser.parseString(record.getValue()).getAsJsonObject();
        maskAllExcept(jsonElement, keys);
        record.setValue(jsonElement.toString());
        return record;
    }

    private void maskAllExcept(JsonObject jsonElement, List<String> keys) {
        maskAllExcept("",  jsonElement, keys);
    }

    private void maskAllExcept(String currentKey, JsonObject node, List<String> keys) {
        if (node.isJsonObject()) {
            JsonObject objectNode = node.getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : objectNode.entrySet()) {
                if(entry.getValue().isJsonObject()) {
                    maskAllExcept(entry.getKey() + ".", entry.getValue().getAsJsonObject(), keys);
                } else {
                    if(!keys.contains(currentKey + entry.getKey())) {
                        objectNode.addProperty(entry.getKey(), jsonMaskReplacement);
                    }
                }
            }
        }
    }
}

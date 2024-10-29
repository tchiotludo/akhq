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
            if(record.isTombstone()) {
                return record;
            } else if(record.appearsToBeJson()) {
                jsonMaskingFilters
                    .stream()
                    .filter(jsonMaskingFilter -> record.getTopic().getName().equalsIgnoreCase(jsonMaskingFilter.getTopic()))
                    .findFirst()
                    .ifPresentOrElse(
                        filter -> applyMasking(record, filter.getKeys()),
                        () -> applyMasking(record, List.of())
                    );
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
        maskAllExcept(jsonElement, keys);
        record.setValue(jsonElement.toString());
        return record;
    }

    private void maskAllExcept(JsonObject node, List<String> keys) {
        if (node.isJsonObject()) {
            JsonObject objectNode = node.getAsJsonObject();
            for(Map.Entry<String, JsonElement> entry : objectNode.entrySet()) {
                if(entry.getValue().isJsonObject()) {
                    maskAllExcept(entry.getValue().getAsJsonObject(), keys);
                } else {
                    if(!keys.contains(entry.getKey())) {
                        objectNode.addProperty(entry.getKey(), jsonMaskReplacement);
                    }
                }
            }
        }
    }
}
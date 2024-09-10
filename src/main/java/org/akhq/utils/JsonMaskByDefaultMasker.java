package org.akhq.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.akhq.configs.JsonMaskingFilter;
import org.akhq.models.Record;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class JsonMaskByDefaultMasker implements Masker {

    private final List<JsonMaskingFilter> jsonMaskingFilters;
    private final String jsonMaskReplacement;

    public Record maskRecord(Record record) {
        try {
            if(record.getValue().trim().startsWith("{") && record.getValue().trim().endsWith("}")) {
                JsonMaskingFilter foundFilter = null;
                for (JsonMaskingFilter filter : jsonMaskingFilters) {
                    if (record.getTopic().getName().equalsIgnoreCase(filter.getTopic())) {
                        foundFilter = filter;
                    }
                }
                if (foundFilter != null) {
                    return applyMasking(record, foundFilter.getKeys());
                } else {
                    return applyMasking(record, List.of());
                }
            } else {
                return record;
            }
        } catch (Exception e) {
            LOG.error("Error masking record", e);
            return record;
        }
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
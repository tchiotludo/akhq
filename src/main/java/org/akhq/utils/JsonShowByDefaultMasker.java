package org.akhq.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.akhq.configs.JsonMaskingFilter;
import org.akhq.models.Record;

import java.util.List;

@RequiredArgsConstructor
public class JsonShowByDefaultMasker implements Masker {

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
                    return record;
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
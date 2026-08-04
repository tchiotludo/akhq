package org.akhq.utils;

import org.akhq.configs.DataMasking;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class JsonMasker implements Masker {
    private final List<Map.Entry<Pattern, List<String>>> topicPatternToKeys;
    protected final String jsonMaskReplacement;

    public JsonMasker(DataMasking dataMasking) {
        this.jsonMaskReplacement = dataMasking.getJsonMaskReplacement();
        this.topicPatternToKeys = buildTopicPatternToKeys(dataMasking);
    }

    private List<Map.Entry<Pattern, List<String>>> buildTopicPatternToKeys(DataMasking dataMasking) {
        return dataMasking.getJsonFilters().stream()
            .filter(filter -> {
                if (filter.getTopic() == null) {
                    LOG.warn("Ignoring json-filter '{}' because it has no topic", filter.getDescription());
                    return false;
                }
                return true;
            })
            .map(filter -> Map.entry(
                Pattern.compile(filter.getTopic(), Pattern.CASE_INSENSITIVE),
                filter.getKeys()
            ))
            .toList();
    }

    protected List<String> getKeysForTopic(String topic) {
        return topicPatternToKeys.stream()
            .filter(entry -> entry.getKey().matcher(topic).matches())
            .flatMap(entry -> entry.getValue().stream())
            .toList();
    }
}

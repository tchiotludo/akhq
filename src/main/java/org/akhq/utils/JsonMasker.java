package org.akhq.utils;

import org.akhq.configs.DataMasking;
import org.akhq.configs.JsonMaskingFilter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class JsonMasker implements Masker {
    private final Map<String, List<String>> topicToKeysMap;
    protected final String jsonMaskReplacement;

    public JsonMasker(DataMasking dataMasking) {
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

    protected List<String> getKeysForTopic(String topic) {
        return topicToKeysMap.getOrDefault(topic.toLowerCase(), Collections.emptyList());
    }
}

package org.akhq.utils;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import org.akhq.configs.DataMasking;

@Factory
public class MaskerFactory {

    @Bean
    public Masker createMaskingUtil(DataMasking dataMasking) {
        if(dataMasking == null) {
            return new NoOpMasker();
        }
        return switch(dataMasking.getMode()) {
            case REGEX -> new RegexMasker(dataMasking.getFilters());
            case JSON_MASK_BY_DEFAULT -> new JsonMaskByDefaultMasker(dataMasking.getJsonFilters(), dataMasking.getJsonMaskReplacement());
            case JSON_SHOW_BY_DEFAULT -> new JsonShowByDefaultMasker(dataMasking.getJsonFilters(), dataMasking.getJsonMaskReplacement());
            case NONE -> new NoOpMasker();
        };
    }
}

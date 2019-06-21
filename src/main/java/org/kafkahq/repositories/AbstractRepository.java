package org.kafkahq.repositories;

import org.kafkahq.modules.KafkaWrapper;

import java.util.Arrays;
import java.util.Optional;

abstract public class AbstractRepository {
    protected static KafkaWrapper kafkaWrapper;

    public static void setWrapper(KafkaWrapper kafkaWrapper) {
        AbstractRepository.kafkaWrapper = kafkaWrapper;
    }

    public static boolean isSearchMatch(Optional<String> search, String value) {
        if (!search.isPresent()) {
            return true;
        }

        String[] split = search.get().split(" ");

        long count = Arrays.stream(split)
            .filter(s -> value.toLowerCase().contains(s))
            .count();

        return count == split.length;
    }

    public static boolean isTopicMatchRegex(Optional<String> regex, String value){
        if(!regex.isPresent()){
            return true;
        }

        return value.matches(regex.get());
    }
}

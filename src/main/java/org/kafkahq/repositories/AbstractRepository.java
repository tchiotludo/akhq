package org.kafkahq.repositories;

import java.util.Arrays;
import java.util.Optional;

abstract public class AbstractRepository {
    public static boolean isSearchMatch(Optional<String> search, String value) {
        if (!search.isPresent()) {
            return true;
        }

        String[] split = search.get().split(" ");

        long count = Arrays.stream(split)
            .filter(s -> value.toLowerCase().contains(s.toLowerCase()))
            .count();

        return count == split.length;
    }

    public static boolean isTopicMatchRegex(Optional<String> regex, String topic){
        if(!regex.isPresent()){
            return true;
        }

        return topic.matches(regex.get());
    }
}

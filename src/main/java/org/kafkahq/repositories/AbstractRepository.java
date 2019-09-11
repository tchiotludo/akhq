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
}

package org.akhq.repositories;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

abstract public class AbstractRepository {
    public static boolean isSearchMatch(Optional<String> search, String value) {
        if (search.isEmpty()) {
            return true;
        }

        String[] split = search.get().split(" ");

        long count = Arrays.stream(split)
            .filter(s -> value.toLowerCase().contains(s.toLowerCase()))
            .count();

        return count == split.length;
    }

    public static boolean isMatchRegex(Optional<List<String>> regex, String item) {
        if (regex.isEmpty() || regex.get().isEmpty()) {
            return true;
        }

        for (String strRegex : regex.get()) {
            if (item.matches(strRegex)) {
                return true;
            }
        }
        return false;
    }
}

package org.akhq.search;

import jakarta.inject.Singleton;
import org.akhq.models.Record;
import org.akhq.repositories.RecordRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class RecordSearchFilter {
    public static final String SEARCH_SPLIT_REGEX = " (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    public boolean matchFilters(RecordRepository.BaseOptions options, Record record) {
        if (options.getSearch() != null) {
            return matchFilter(options.getSearch(), Arrays.asList(record.getKey(), record.getValue()));
        } else {
            if (options.getSearchByKey() != null) {
                if (!matchFilter(options.getSearchByKey(), Collections.singletonList(record.getKey()))) {
                    return false;
                }
            }

            if (options.getSearchByValue() != null) {
                if (!matchFilter(options.getSearchByValue(), Collections.singletonList(record.getValue()))) {
                    return false;
                }
            }

            if (options.getSearchByHeaderKey() != null) {
                if (!matchFilter(options.getSearchByHeaderKey(), record.getHeadersKeySet())) {
                    return false;
                }
            }

            if (options.getSearchByHeaderValue() != null) {
                if (!matchFilter(options.getSearchByHeaderValue(), record.getHeadersValues())) {
                    return false;
                }
            }

            if (options.getSearchByKeySubject() != null) {
                if (!matchFilter(options.getSearchByKeySubject(), Collections.singletonList(record.getKeySubject()))) {
                    return false;
                }
            }

            if (options.getSearchByValueSubject() != null) {
                return matchFilter(options.getSearchByValueSubject(), Collections.singletonList(record.getValueSubject()));
            }
        }
        return true;
    }

    public boolean matchFilters(RecordRepository.Options options, Record record) {
        if (!matchFilters((RecordRepository.BaseOptions) options, record)) {
            return false;
        }

        if (options.getEndTimestamp() != null) {
            return record.getTimestamp().toInstant().toEpochMilli() <= options.getEndTimestamp();
        }

        return true;
    }

    private boolean matchFilter(RecordRepository.Search searchFilter, Collection<String> stringsToSearch) {
        return switch (searchFilter.getSearchMatchType()) {
            case EQUALS -> equalsAll(searchFilter.getText(), stringsToSearch);
            case NOT_CONTAINS -> notContainsAll(searchFilter.getText(), stringsToSearch);
            default -> containsAll(searchFilter.getText(), stringsToSearch);
        };
    }

    private boolean containsAll(String search, Collection<String> in) {
        if (search.equals("null")) {
            return in
                .stream()
                .allMatch(Objects::isNull);
        }

        return in.parallelStream()
            .filter(Objects::nonNull)
            .anyMatch(s -> extractSearchPatterns(search)
                .stream()
                .anyMatch(s.toLowerCase()::contains));
    }

    private boolean equalsAll(String search, Collection<String> in) {
        if (search.equals("null")) {
            return in
                .stream()
                .allMatch(Objects::isNull);
        }

        return in.parallelStream().filter(Objects::nonNull)
            .anyMatch(s -> extractSearchPatterns(search).contains(s.toLowerCase()));
    }

    private boolean notContainsAll(String search, Collection<String> in) {
        if (search.equals("null")) {
            return in
                .stream()
                .noneMatch(Objects::isNull);
        }

        return in.parallelStream()
            .anyMatch(s -> s == null || extractSearchPatterns(search)
                .stream()
                .noneMatch(s.toLowerCase()::contains));
    }

    private List<String> extractSearchPatterns(String searchString) {
        return Arrays.stream(searchString.toLowerCase().split(SEARCH_SPLIT_REGEX, -1))
            .map(s -> {
                s = s.replaceAll("\\\\", "");
                return s.startsWith("\"") ? s.substring(1, s.length() - 1) : s;
            }).collect(Collectors.toList());
    }
}

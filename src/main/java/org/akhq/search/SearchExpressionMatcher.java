package org.akhq.search;

import com.google.re2j.Pattern;
import org.akhq.models.Record;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Evaluates a validated {@link SearchExpression} against records. Patterns are compiled once.
 * Conditions of a group, then groups, are combined strictly left to right with their joins.
 */
public class SearchExpressionMatcher {
    private final List<GroupMatcher> groups;

    private SearchExpressionMatcher(List<GroupMatcher> groups) {
        this.groups = groups;
    }

    public static SearchExpressionMatcher compile(SearchExpression expression) {
        return new SearchExpressionMatcher(expression.groups()
            .stream()
            .map(group -> new GroupMatcher(
                group.join(),
                group.conditions().stream().map(ConditionMatcher::new).toList()
            ))
            .toList());
    }

    public boolean matches(Record record) {
        boolean result = false;
        for (int i = 0; i < groups.size(); i++) {
            GroupMatcher group = groups.get(i);
            result = i == 0 ? group.matches(record) : combine(result, group.join, () -> group.matches(record));
        }
        return result;
    }

    private static boolean combine(boolean previous, SearchExpression.Join join, BooleanSupplier next) {
        return join == SearchExpression.Join.AND ? previous && next.getAsBoolean() : previous || next.getAsBoolean();
    }

    private record GroupMatcher(SearchExpression.Join join, List<ConditionMatcher> conditions) {
        boolean matches(Record record) {
            boolean result = false;
            for (int i = 0; i < conditions.size(); i++) {
                ConditionMatcher condition = conditions.get(i);
                result = i == 0 ? condition.matches(record) : combine(result, condition.join, () -> condition.matches(record));
            }
            return result;
        }
    }

    private static class ConditionMatcher {
        private final SearchExpression.Join join;
        private final SearchExpression.Field field;
        private final SearchExpression.Operator operator;
        private final String value;
        private final String lowerValue;
        private final Pattern pattern;

        ConditionMatcher(SearchExpression.Condition condition) {
            this.join = condition.join();
            this.field = condition.field();
            this.operator = condition.operator();
            this.value = condition.value();
            this.lowerValue = value.toLowerCase(Locale.ROOT);
            this.pattern = operator == SearchExpression.Operator.MATCHES_REGEX
                ? Pattern.compile(value, Pattern.CASE_INSENSITIVE)
                : null;
        }

        boolean matches(Record record) {
            Collection<String> candidates = candidates(record);

            return switch (operator) {
                case CONTAINS -> isNullLiteral() ? allNull(candidates) : any(candidates, s -> s.toLowerCase(Locale.ROOT).contains(lowerValue));
                case EQUALS -> isNullLiteral() ? allNull(candidates) : any(candidates, s -> s.equalsIgnoreCase(value));
                case NOT_CONTAINS -> isNullLiteral() ? !allNull(candidates) : !any(candidates, s -> s.toLowerCase(Locale.ROOT).contains(lowerValue));
                case MATCHES_REGEX -> any(candidates, s -> pattern.matcher(s).find());
            };
        }

        // Same convention as the legacy search: the literal "null" targets null fields
        private boolean isNullLiteral() {
            return value.equals("null");
        }

        private static boolean allNull(Collection<String> candidates) {
            return candidates.stream().allMatch(Objects::isNull);
        }

        private static boolean any(Collection<String> candidates, Predicate<String> predicate) {
            return candidates.stream().filter(Objects::nonNull).anyMatch(predicate);
        }

        private Collection<String> candidates(Record record) {
            return switch (field) {
                case VALUE -> Collections.singletonList(record.getValue());
                case KEY -> Collections.singletonList(record.getKey());
                case HEADER_KEY -> record.getHeadersKeySet();
                case HEADER_VALUE -> record.getHeadersValues();
                case KEY_SUBJECT -> Collections.singletonList(record.getKeySubject());
                case VALUE_SUBJECT -> Collections.singletonList(record.getValueSubject());
            };
        }
    }
}

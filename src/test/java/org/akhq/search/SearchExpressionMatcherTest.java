package org.akhq.search;

import org.akhq.configs.SearchExpressionConfig;
import org.akhq.models.KeyValue;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

import static org.akhq.search.SearchExpression.Field.*;
import static org.akhq.search.SearchExpression.Join.AND;
import static org.akhq.search.SearchExpression.Join.OR;
import static org.akhq.search.SearchExpression.Operator.*;
import static org.junit.jupiter.api.Assertions.*;

class SearchExpressionMatcherTest {
    private final SearchExpressionValidator validator = new SearchExpressionValidator(new SearchExpressionConfig());

    private static SearchExpression.Condition condition(SearchExpression.Join join, SearchExpression.Field field, SearchExpression.Operator operator, String value) {
        return new SearchExpression.Condition(join, field, operator, value);
    }

    private static SearchExpression.Group group(SearchExpression.Join join, SearchExpression.Condition... conditions) {
        return new SearchExpression.Group(join, List.of(conditions));
    }

    private static SearchExpression expression(SearchExpression.Group... groups) {
        return new SearchExpression(1, List.of(groups));
    }

    private static SearchExpression single(SearchExpression.Field field, SearchExpression.Operator operator, String value) {
        return expression(group(null, condition(null, field, operator, value)));
    }

    private boolean matches(SearchExpression expression, Record record) {
        validator.validate(expression);
        return SearchExpressionMatcher.compile(expression).matches(record);
    }

    private static Record record(String key, String value) {
        Record record = new Record();
        record.setKey(key);
        record.setValue(value);
        return record;
    }

    private static Record withSubjects(Record record, String keySubject, String valueSubject) throws Exception {
        set(record, "keySubject", keySubject);
        set(record, "valueSubject", valueSubject);
        return record;
    }

    private static void set(Record record, String name, Object value) throws Exception {
        Field field = Record.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(record, value);
    }

    @Test
    void containsIsCaseInsensitiveSubstring() {
        Record record = record("k", "Customer-123 created");

        assertTrue(matches(single(VALUE, CONTAINS, "customer-123"), record));
        assertFalse(matches(single(VALUE, CONTAINS, "customer-124"), record));
    }

    @Test
    void containsDoesNotSplitTerms() {
        assertFalse(matches(single(VALUE, CONTAINS, "customer created"), record("k", "customer-123 created")));
        assertTrue(matches(single(VALUE, CONTAINS, "customer-123 created"), record("k", "customer-123 created")));
    }

    @Test
    void equalsMatchesWholeFieldIgnoringCase() {
        Record record = record("Order.Created", "v");

        assertTrue(matches(single(KEY, EQUALS, "order.created"), record));
        assertFalse(matches(single(KEY, EQUALS, "order"), record));
    }

    @Test
    void notContainsIsNegationOfContains() {
        Record record = record("k", "customer-123");

        assertTrue(matches(single(VALUE, NOT_CONTAINS, "customer-124"), record));
        assertFalse(matches(single(VALUE, NOT_CONTAINS, "CUSTOMER"), record));
    }

    @Test
    void notContainsMatchesNullField() {
        assertTrue(matches(single(VALUE, NOT_CONTAINS, "anything"), record("k", null)));
    }

    @Test
    void regexFindsHitAnywhereCaseInsensitive() {
        Record record = record("k", "{\"id\":\"Customer-124\",\"status\":\"ok\"}");

        assertTrue(matches(single(VALUE, MATCHES_REGEX, "customer-12(3|4)"), record));
        assertTrue(matches(single(VALUE, MATCHES_REGEX, "\"status\":\"(ok|ko)\""), record));
        assertFalse(matches(single(VALUE, MATCHES_REGEX, "customer-12(5|6)"), record));
    }

    @Test
    void regexIsNotAnchored() {
        assertTrue(matches(single(VALUE, MATCHES_REGEX, "ell"), record("k", "hello")));
        assertFalse(matches(single(VALUE, MATCHES_REGEX, "^ell$"), record("k", "hello")));
    }

    @Test
    void nullLiteralTargetsNullFields() {
        assertTrue(matches(single(VALUE, EQUALS, "null"), record("k", null)));
        assertFalse(matches(single(VALUE, EQUALS, "null"), record("k", "x")));
        assertTrue(matches(single(VALUE, NOT_CONTAINS, "null"), record("k", "x")));
        assertFalse(matches(single(VALUE, NOT_CONTAINS, "null"), record("k", null)));
    }

    @Test
    void nullFieldDoesNotMatchContainsEqualsOrRegex() {
        Record record = record("k", null);

        assertFalse(matches(single(VALUE, CONTAINS, "x"), record));
        assertFalse(matches(single(VALUE, EQUALS, "x"), record));
        assertFalse(matches(single(VALUE, MATCHES_REGEX, ".*"), record));
    }

    @Test
    void headersMatchWhenAnyHeaderMatches() {
        Record record = record("k", "v");
        record.getHeaders().add(new KeyValue<>("trace-id", "abc"));
        record.getHeaders().add(new KeyValue<>("source", "orders"));

        assertTrue(matches(single(HEADER_KEY, EQUALS, "source"), record));
        assertTrue(matches(single(HEADER_VALUE, CONTAINS, "ORD"), record));
        assertFalse(matches(single(HEADER_VALUE, CONTAINS, "missing"), record));
        assertFalse(matches(single(HEADER_KEY, NOT_CONTAINS, "trace"), record));
    }

    @Test
    void subjectsAreMatched() throws Exception {
        Record record = withSubjects(record("k", "v"), "orders-key", "orders-value");

        assertTrue(matches(single(KEY_SUBJECT, EQUALS, "orders-key"), record));
        assertTrue(matches(single(VALUE_SUBJECT, MATCHES_REGEX, "^orders-"), record));
    }

    @Test
    void joinsInsideAGroupAreEvaluatedLeftToRight() {
        Record record = record("k", "alpha");

        // (false OR true) AND false -> false
        assertFalse(matches(expression(group(null,
            condition(null, VALUE, CONTAINS, "zzz"),
            condition(OR, VALUE, CONTAINS, "alpha"),
            condition(AND, VALUE, CONTAINS, "yyy")
        )), record));

        // (true OR false) AND true -> true
        assertTrue(matches(expression(group(null,
            condition(null, VALUE, CONTAINS, "alpha"),
            condition(OR, VALUE, CONTAINS, "zzz"),
            condition(AND, VALUE, CONTAINS, "lph")
        )), record));
    }

    @Test
    void groupsAreJoinedWithTheirJoin() {
        SearchExpression both = expression(
            group(null,
                condition(null, VALUE, MATCHES_REGEX, "customer-12(3|4)"),
                condition(OR, VALUE, CONTAINS, "customer-999")
            ),
            group(AND, condition(null, KEY, EQUALS, "order.created"))
        );

        assertTrue(matches(both, record("order.created", "customer-124")));
        assertFalse(matches(both, record("order.deleted", "customer-124")));
        assertFalse(matches(both, record("order.created", "customer-125")));

        SearchExpression either = expression(
            group(null, condition(null, KEY, EQUALS, "order.created")),
            group(OR, condition(null, VALUE, CONTAINS, "special"))
        );
        assertTrue(matches(either, record("order.deleted", "special case")));
        assertFalse(matches(either, record("order.deleted", "regular case")));
    }

    @Test
    void regexRunsInLinearTimeOnPathologicalPatterns() {
        SearchExpression expression = single(VALUE, MATCHES_REGEX, "(a+)+$");
        validator.validate(expression);
        SearchExpressionMatcher matcher = SearchExpressionMatcher.compile(expression);
        Record record = record("k", "a".repeat(50_000) + "!");

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertFalse(matcher.matches(record)));
    }
}

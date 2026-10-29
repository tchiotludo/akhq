package org.akhq.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akhq.configs.SearchExpressionConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchExpressionValidatorTest {
    private final SearchExpressionConfig config = new SearchExpressionConfig();
    private final SearchExpressionValidator validator = new SearchExpressionValidator(config);

    private static SearchExpression.Condition condition(SearchExpression.Join join, String value) {
        return new SearchExpression.Condition(join, SearchExpression.Field.VALUE, SearchExpression.Operator.CONTAINS, value);
    }

    private static SearchExpression.Condition regex(String value) {
        return new SearchExpression.Condition(null, SearchExpression.Field.VALUE, SearchExpression.Operator.MATCHES_REGEX, value);
    }

    private static SearchExpression oneGroup(SearchExpression.Condition... conditions) {
        return new SearchExpression(1, List.of(new SearchExpression.Group(null, List.of(conditions))));
    }

    private void assertInvalid(SearchExpression expression, String messagePart) {
        SearchExpressionException e = assertThrows(SearchExpressionException.class, () -> validator.validate(expression));
        assertTrue(e.getMessage().contains(messagePart), e.getMessage());
    }

    @Test
    void acceptsValidExpression() {
        assertDoesNotThrow(() -> validator.validate(oneGroup(condition(null, "a"))));
    }

    @Test
    void rejectsMissingExpressionOrUnsupportedVersion() {
        List<SearchExpression.Group> groups = oneGroup(condition(null, "a")).groups();

        assertInvalid(null, "required");
        assertInvalid(new SearchExpression(null, groups), "version");
        assertInvalid(new SearchExpression(2, groups), "version");
    }

    @Test
    void rejectsEmptyGroupsAndConditions() {
        assertInvalid(new SearchExpression(1, null), "list of groups");
        assertInvalid(new SearchExpression(1, List.of(new SearchExpression.Group(null, Collections.emptyList()))), "at least one condition");
    }

    @Test
    void enforcesJoinRules() {
        assertInvalid(new SearchExpression(1, List.of(new SearchExpression.Group(SearchExpression.Join.AND, List.of(condition(null, "a"))))), "must not have a join");
        assertInvalid(oneGroup(condition(SearchExpression.Join.OR, "a")), "must not have a join");
        assertInvalid(oneGroup(condition(null, "a"), condition(null, "b")), "must have a join");
        assertInvalid(new SearchExpression(1, List.of(
            new SearchExpression.Group(null, List.of(condition(null, "a"))),
            new SearchExpression.Group(null, List.of(condition(null, "b")))
        )), "must have a join");
    }

    @Test
    void enforcesLimits() {
        config.setMaxValueLength(5);
        assertInvalid(oneGroup(condition(null, "too long")), "too long");

        config.setMaxValueLength(1000);
        config.setMaxGroups(1);
        assertInvalid(new SearchExpression(1, List.of(
            new SearchExpression.Group(null, List.of(condition(null, "a"))),
            new SearchExpression.Group(SearchExpression.Join.AND, List.of(condition(null, "b")))
        )), "too many groups");

        config.setMaxGroups(10);
        config.setMaxConditions(1);
        assertInvalid(oneGroup(condition(null, "a"), condition(SearchExpression.Join.OR, "b")), "too many conditions");
    }

    @Test
    void rejectsEmptyValueExceptForEquals() {
        assertInvalid(oneGroup(condition(null, "")), "empty");
        assertDoesNotThrow(() -> validator.validate(oneGroup(
            new SearchExpression.Condition(null, SearchExpression.Field.VALUE, SearchExpression.Operator.EQUALS, "")
        )));
    }

    @Test
    void rejectsInvalidRegex() {
        assertInvalid(oneGroup(regex("(unclosed")), "Invalid regular expression");
    }

    @Test
    void rejectsRegexFeaturesThatCannotRunInLinearTime() {
        assertInvalid(oneGroup(regex("(a)\\1")), "Invalid regular expression");
        assertInvalid(oneGroup(regex("(?=a)b")), "Invalid regular expression");
    }

    @Test
    void deserializesTheDocumentedJsonShape() throws Exception {
        String json = """
            {
              "version": 1,
              "groups": [
                {
                  "conditions": [
                    { "field": "value", "operator": "matchesRegex", "value": "customer-12(3|4)" },
                    { "join": "OR", "field": "headerKey", "operator": "notContains", "value": "x" }
                  ]
                },
                { "join": "AND", "conditions": [ { "field": "key", "operator": "equals", "value": "order.created" } ] }
              ]
            }
            """;

        SearchExpression expression = new ObjectMapper().readValue(json, SearchExpression.class);

        assertDoesNotThrow(() -> validator.validate(expression));
        assertEquals(SearchExpression.Operator.MATCHES_REGEX, expression.groups().get(0).conditions().get(0).operator());
        assertEquals(SearchExpression.Field.HEADER_KEY, expression.groups().get(0).conditions().get(1).field());
        assertEquals(SearchExpression.Join.AND, expression.groups().get(1).join());
    }
}

package org.akhq.mcp.services;

import org.akhq.configs.SearchExpressionConfig;
import org.akhq.mcp.model.FindMessageInTopicArguments;
import org.akhq.search.SearchExpression;
import org.akhq.search.SearchExpressionValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AkhqTopicDataToolServiceExpressionTest {
    private final AkhqTopicDataToolService service = new AkhqTopicDataToolService(
        null, null, null, null, new SearchExpressionValidator(new SearchExpressionConfig())
    );

    private static SearchExpression expression(String regex) {
        return new SearchExpression(1, List.of(new SearchExpression.Group(null, List.of(
            new SearchExpression.Condition(null, SearchExpression.Field.VALUE, SearchExpression.Operator.MATCHES_REGEX, regex)
        ))));
    }

    private static FindMessageInTopicArguments arguments(String searchByValue, SearchExpression expression) {
        return new FindMessageInTopicArguments(
            "local", "topic", null, null, searchByValue, null, null, null, null, null,
            expression, null, null, null, null, null, null
        );
    }

    private String errorOf(FindMessageInTopicArguments arguments) {
        return assertThrows(IllegalArgumentException.class, () -> service.findMessageInTopic(arguments)).getMessage();
    }

    @Test
    void requiresExpressionOrLiteralFilter() {
        assertTrue(errorOf(arguments(null, null)).contains("`expression`"));
    }

    @Test
    void rejectsExpressionCombinedWithLiteralFilter() {
        assertTrue(errorOf(arguments("needle", expression("a"))).contains("cannot be combined"));
    }

    @Test
    void rejectsInvalidRegexWithAReadableMessage() {
        assertTrue(errorOf(arguments(null, expression("("))).startsWith("Invalid `expression`"));
    }

    @Test
    void rejectsEmptyExpression() {
        assertTrue(errorOf(arguments(null, new SearchExpression(1, List.of()))).contains("at least one group"));
    }
}

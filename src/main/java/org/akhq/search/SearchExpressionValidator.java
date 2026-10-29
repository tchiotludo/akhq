package org.akhq.search;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.akhq.configs.SearchExpressionConfig;

import java.util.List;

@Singleton
public class SearchExpressionValidator {
    private final SearchExpressionConfig config;

    @Inject
    public SearchExpressionValidator(SearchExpressionConfig config) {
        this.config = config;
    }

    /**
     * Validates the structure, the configured limits and the regular expressions of a search expression.
     *
     * @throws SearchExpressionException if the expression is not valid
     */
    public void validate(SearchExpression expression) {
        if (expression == null) {
            throw new SearchExpressionException("The search expression is required");
        }

        if (expression.version() == null || expression.version() != SearchExpression.CURRENT_VERSION) {
            throw new SearchExpressionException("Unsupported search expression version, expected " + SearchExpression.CURRENT_VERSION);
        }

        List<SearchExpression.Group> groups = expression.groups();
        if (groups == null || groups.isEmpty()) {
            throw new SearchExpressionException("The search expression must contain at least one group");
        }

        if (groups.size() > config.getMaxGroups()) {
            throw new SearchExpressionException("The search expression has too many groups, maximum is " + config.getMaxGroups());
        }

        int conditionCount = 0;
        for (int g = 0; g < groups.size(); g++) {
            SearchExpression.Group group = groups.get(g);
            if (group == null) {
                throw new SearchExpressionException("Group " + (g + 1) + " is missing");
            }

            validateJoin(group.join(), g == 0, "group " + (g + 1));

            List<SearchExpression.Condition> conditions = group.conditions();
            if (conditions == null || conditions.isEmpty()) {
                throw new SearchExpressionException("Group " + (g + 1) + " must contain at least one condition");
            }

            conditionCount += conditions.size();
            if (conditionCount > config.getMaxConditions()) {
                throw new SearchExpressionException("The search expression has too many conditions, maximum is " + config.getMaxConditions());
            }

            for (int c = 0; c < conditions.size(); c++) {
                validateCondition(conditions.get(c), c == 0, "condition " + (c + 1) + " of group " + (g + 1));
            }
        }
    }

    private void validateCondition(SearchExpression.Condition condition, boolean first, String label) {
        if (condition == null) {
            throw new SearchExpressionException("The " + label + " is missing");
        }

        validateJoin(condition.join(), first, label);

        if (condition.field() == null) {
            throw new SearchExpressionException("The " + label + " must have a valid field");
        }

        if (condition.operator() == null) {
            throw new SearchExpressionException("The " + label + " must have a valid operator");
        }

        String value = condition.value();
        if (value == null) {
            throw new SearchExpressionException("The " + label + " must have a value");
        }

        if (value.length() > config.getMaxValueLength()) {
            throw new SearchExpressionException("The value of the " + label + " is too long, maximum is " + config.getMaxValueLength() + " characters");
        }

        if (value.isEmpty() && condition.operator() != SearchExpression.Operator.EQUALS) {
            throw new SearchExpressionException("The value of the " + label + " must not be empty");
        }

        if (condition.operator() == SearchExpression.Operator.MATCHES_REGEX) {
            try {
                Pattern.compile(value, Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                throw new SearchExpressionException("Invalid regular expression in the " + label + ": " + e.getDescription());
            }
        }
    }

    private void validateJoin(SearchExpression.Join join, boolean first, String label) {
        if (first && join != null) {
            throw new SearchExpressionException("The " + label + " is the first one and must not have a join");
        }

        if (!first && join == null) {
            throw new SearchExpressionException("The " + label + " must have a join");
        }
    }
}

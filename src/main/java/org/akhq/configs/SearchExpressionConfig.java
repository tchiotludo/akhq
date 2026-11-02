package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("akhq.topic-data.search")
public class SearchExpressionConfig {
    /**
     * Maximum number of groups in a search expression.
     */
    private int maxGroups = 10;

    /**
     * Maximum number of conditions in a search expression, all groups included.
     */
    private int maxConditions = 50;

    /**
     * Maximum length of a condition value, regular expressions included.
     */
    private int maxValueLength = 1000;
}

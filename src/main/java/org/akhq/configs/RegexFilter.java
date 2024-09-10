package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
import lombok.Data;

@EachProperty("filters")
@Data
public class RegexFilter {
    String description;
    String searchRegex;
    String replacement;
}

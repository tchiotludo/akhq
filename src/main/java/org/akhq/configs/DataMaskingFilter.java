package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@EachProperty("filters")
@Data
@Serdeable
public class DataMaskingFilter {
    String description;
    String searchRegex;
    String replacement;
}

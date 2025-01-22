package org.akhq.configs;

import io.micronaut.context.annotation.EachProperty;
import lombok.Data;

import java.util.List;

@EachProperty("jsonfilters")
@Data
public class JsonMaskingFilter {
    String description = "UNKNOWN";
    String topic = "UNKNOWN";
    List<String> keys = List.of("UNKNOWN");
}

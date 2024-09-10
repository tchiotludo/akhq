package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.data-masking")
@Data
public class DataMasking {
   List<RegexFilter> filters = new ArrayList<>();
   DataMaskingMode mode = DataMaskingMode.REGEX; // set this by default to REGEX for backwards compatibility for current users who haven't defined this property.
   List<JsonMaskingFilter> jsonFilters = new ArrayList<>();
   String jsonMaskReplacement = "xxxx";
   boolean cachingEnabled = false;
}

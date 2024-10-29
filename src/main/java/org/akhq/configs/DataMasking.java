package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.data-masking")
@Data
public class DataMasking {
   List<RegexFilter> filters = new ArrayList<>(); // regex filters still use `filters` for backwards compatibility
   DataMaskingMode mode = DataMaskingMode.REGEX; // set this by default to REGEX for backwards compatibility for current regex filtering users who haven't defined this property.
   List<JsonMaskingFilter> jsonFilters = new ArrayList<>();
   String jsonMaskReplacement = "xxxx";
   boolean cachingEnabled = false;
}

package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.data-masking")
@Data
public class DataMasking {
   List<DataMaskingFilter> filters = new ArrayList<>();
}

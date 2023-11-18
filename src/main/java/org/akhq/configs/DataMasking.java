package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
//import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("akhq.security.data-masking")
@Data
//@Serdeable
public class DataMasking {
   List<DataMaskingFilter> filters = new ArrayList<>();
}

package org.akhq.configs;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.akhq.configs.DateTimeFormat.RELATIVE;

@Data
@Serdeable
@AllArgsConstructor
@NoArgsConstructor
public class UiOptionsTopicData {
    private String sort;
    private DateTimeFormat dateTimeFormat = RELATIVE;
}

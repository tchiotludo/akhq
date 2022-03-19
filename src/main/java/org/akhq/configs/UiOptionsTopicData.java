package org.akhq.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.akhq.configs.DateFormat.RELATIVE;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UiOptionsTopicData {
    private String sort;
    private DateFormat dateFormat = RELATIVE;
}

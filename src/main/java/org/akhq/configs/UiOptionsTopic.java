package org.akhq.configs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UiOptionsTopic {
    private String defaultView;
    private Boolean skipConsumerGroups;
    private Boolean skipLastRecord;
}

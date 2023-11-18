package org.akhq.configs;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
//@Serdeable
public class UiOptionsTopic {
    private String defaultView;
    private Boolean skipConsumerGroups;
    private Boolean skipLastRecord;
    private Boolean showAllConsumerGroups;
}

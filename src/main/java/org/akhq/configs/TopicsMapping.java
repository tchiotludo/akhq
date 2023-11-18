package org.akhq.configs;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//@Serdeable
public class TopicsMapping {
    String topicRegex;
    String descriptorFile;
    String descriptorFileBase64;
    String keyMessageType;
    String valueMessageType;
}

package org.akhq.configs;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
//@Serdeable
public class AvroTopicsMapping {
    String topicRegex;
    String keySchemaFile;
    String valueSchemaFile;
}

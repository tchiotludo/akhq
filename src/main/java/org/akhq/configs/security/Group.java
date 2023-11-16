package org.akhq.configs.security;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

import java.util.List;

@Data
@Serdeable
public class Group {
    private String role;
    private List<String> patterns = List.of(".*");
    private List<String> clusters = List.of(".*");
}

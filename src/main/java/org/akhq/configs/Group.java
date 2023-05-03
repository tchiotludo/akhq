package org.akhq.configs;

import lombok.Data;

import java.util.List;

@Data
public class Group {
    String role;
    List<String> patterns = List.of(".*");
    List<String> clusters = List.of(".*");
}

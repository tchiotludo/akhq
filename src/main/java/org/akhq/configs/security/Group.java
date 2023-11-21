package org.akhq.configs.security;

import lombok.*;

import java.util.List;

@Data
public class Group {
    private String role;
    private List<String> patterns = List.of(".*");
    private List<String> clusters = List.of(".*");
}

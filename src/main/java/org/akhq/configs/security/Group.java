package org.akhq.configs.security;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Group implements Serializable {
    private String role;
    private List<String> patterns = List.of(".*");
    private List<String> clusters = List.of(".*");
}

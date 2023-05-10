package org.akhq.configs.security;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Group implements Serializable {
    private String role;

    @JsonUnwrapped
    private Restriction restriction;

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class Restriction {
        private List<String> patterns = List.of(".*");
        private List<String> clusters = List.of(".*");
    }
}

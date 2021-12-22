package org.akhq.configs.newAcls;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Binding {
    private String permission;
    private List<String> clusters = List.of(".*");
    private List<String> patterns = List.of(".*");
}

package org.akhq.configs;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Group {
    String name;
    List<String> roles;
    Map<String, List<String>> attributes;
}

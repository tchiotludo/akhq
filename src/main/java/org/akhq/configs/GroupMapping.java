package org.akhq.configs;

import lombok.Data;

import java.util.List;

@Data
public class GroupMapping {
    String name;
    List<String> groups;
}

package org.akhq.configs;

import lombok.Data;

import java.util.List;

@Data
public class UserMapping {
    String username;
    List<String> groups;
}

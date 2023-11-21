package org.akhq.configs.security.ldap;

import lombok.Data;

import java.util.List;

@Data
public class GroupMapping {
    String name;
    List<String> groups;
}

package org.akhq.configs.security.ldap;

import lombok.Data;

import java.util.List;

@Data
public class UserMapping {
    String username;
    List<String> groups;
}

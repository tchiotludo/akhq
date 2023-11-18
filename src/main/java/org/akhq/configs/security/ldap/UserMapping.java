package org.akhq.configs.security.ldap;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.List;

@Data
//@Serdeable
public class UserMapping {
    String username;
    List<String> groups;
}

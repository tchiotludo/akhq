package org.akhq.configs.security.ldap;

//import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.List;

@Data
//@Serdeable
public class GroupMapping {
    String name;
    List<String> groups;
}

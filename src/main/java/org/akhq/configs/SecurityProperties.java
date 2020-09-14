package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import java.util.List;

@ConfigurationProperties("akhq.security")
@Data
public class SecurityProperties {
    private List<BasicAuth> basicAuth;
    private List<Group> groups;
    private String defaultGroup;
}

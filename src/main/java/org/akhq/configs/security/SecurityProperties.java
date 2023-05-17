package org.akhq.configs.security;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("akhq.security")
@Data
public class SecurityProperties {
    private List<BasicAuth> basicAuth = new ArrayList<>();
    private String defaultGroup;

    @MapFormat(keyFormat = StringConvention.RAW)
    private Map<String, List<Role>> roles;

    @MapFormat(keyFormat = StringConvention.RAW)
    private Map<String, List<Group>> groups;
}

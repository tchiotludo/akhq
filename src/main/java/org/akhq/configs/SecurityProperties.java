package org.akhq.configs;

import com.google.common.base.Strings;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.convert.format.MapFormat;
import lombok.Data;
import org.akhq.configs.newAcls.Binding;
import org.akhq.configs.newAcls.Permission;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("akhq.security")
@Data
public class SecurityProperties {
    private List<BasicAuth> basicAuth = new ArrayList<>();

    private List<String> defaultBindings;

    private Map<String, Permission> permissions;

    private Map<String, List<Binding>> bindings;
}

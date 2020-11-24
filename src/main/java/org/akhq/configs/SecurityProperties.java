package org.akhq.configs;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ConfigurationProperties("akhq.security")
@Data
public class SecurityProperties {
    private List<BasicAuth> basicAuth = new ArrayList<>();
    private List<Group> basicGroups = new ArrayList<>();
    private List<Group> groups = new ArrayList<>();
    private String defaultGroup;

    @PostConstruct
    public void init() {
        Map<String, Group> allGroups = basicGroups.stream()
                .collect(Collectors.toMap(group -> group.name, group -> group));
        groups.forEach(group -> allGroups.put(group.name, group));
        groups = new LinkedList<>(allGroups.values());
    }
}

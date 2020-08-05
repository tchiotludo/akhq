package org.akhq.controllers;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.security.utils.SecurityService;
import org.akhq.configs.BasicAuth;
import org.akhq.configs.LdapGroup;
import org.akhq.configs.LdapUser;
import org.akhq.modules.KafkaModule;
import org.akhq.utils.UserGroupUtils;
import org.akhq.utils.VersionProvider;
import org.sourcelab.kafka.connect.apiclient.KafkaConnectClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;

abstract public class AbstractController {
    @Value("${akhq.server.base-path}")
    protected String basePath;

    @Inject
    private VersionProvider versionProvider;

    @Inject
    private KafkaModule kafkaModule;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private UserGroupUtils userGroupUtils;

    @Value("${akhq.security.default-group}")
    private String defaultGroups;

    @Inject
    private List<BasicAuth> basicAuths;

    @Inject
    private List<LdapGroup> ldapAuths;

    @Inject
    private List<LdapUser> ldapUsers;

    @SuppressWarnings("unchecked")
    protected Map<String, Object> templateData(Optional<String> cluster, Object... values) {
        Map<String, Object> datas = CollectionUtils.mapOf(values);

        datas.put("tag", versionProvider.getTag());
        datas.put("clusters", this.kafkaModule.getClustersList());
        datas.put("basePath", getBasePath());
        datas.put("roles", getRights());

        cluster.ifPresent(s -> {
            datas.put("clusterId", s);
            datas.put("registryEnabled", this.kafkaModule.getRegistryRestClient(s) != null);

            Map<String, KafkaConnectClient> connectClientMap = this.kafkaModule.getConnectRestClient(s);
            if (connectClientMap != null) {
                Set<String> filteredConnects = filterConnectList(connectClientMap.keySet());
                datas.put("connectList", filteredConnects);
            }
        });

        if (applicationContext.containsBean(SecurityService.class)) {
            datas.put("loginEnabled", basicAuths.size() > 0 || ldapAuths.size() > 0 || ldapUsers.size() > 0);

            SecurityService securityService = applicationContext.getBean(SecurityService.class);
            securityService
                .getAuthentication()
                .ifPresent(authentication -> datas.put("username", authentication.getName()));
        } else {
            datas.put("loginEnabled", false);
        }

        return datas;
    }

    private Set<String> filterConnectList(Set<String> keySet) {
        keySet.removeIf(key -> key.equals(""));
        return keySet;
    }

    protected String getBasePath() {
        return basePath.replaceAll("/$","");
    }

    protected URI uri(String path) throws URISyntaxException {
        return new URI((this.basePath != null ? this.basePath : "") + path);
    }

    private static List<String> expandRoles(List<String> roles) {
        return roles
            .stream()
            .map(s -> {
                ArrayList<String> rolesExpanded = new ArrayList<>();

                ArrayList<String> split = new ArrayList<>(Arrays.asList(s.split("/")));

                while (split.size() > 0) {
                    rolesExpanded.add(String.join("/", split));
                    split.remove(split.size() - 1);
                }

                return rolesExpanded;
            })
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    protected List<String> getRights() {
        if (!applicationContext.containsBean(SecurityService.class)) {
            return expandRoles(this.userGroupUtils.getUserRoles(Collections.singletonList(this.defaultGroups)));
        }

        SecurityService securityService = applicationContext.getBean(SecurityService.class);

        return expandRoles(
            securityService
                .getAuthentication()
                .map(authentication -> (List<String>) authentication.getAttributes().get("roles"))
                .orElseGet(() -> this.userGroupUtils.getUserRoles(Collections.singletonList(this.defaultGroups)))
        );
    }
}

package org.akhq.security.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.impl.compression.GzipCompressionAlgorithm;
import io.micronaut.http.BasicHttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.AbstractSecurityRule;
import io.micronaut.security.rules.SecuredAnnotationRule;
import io.micronaut.security.rules.SecurityRuleResult;
import io.micronaut.security.token.RolesFinder;
import io.micronaut.web.router.MethodBasedRouteMatch;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.akhq.configs.security.Group;
import org.akhq.configs.security.Role;
import org.akhq.configs.security.SecurityProperties;
import org.akhq.models.security.ClaimProvider;
import org.akhq.models.security.ClaimProviderType;
import org.akhq.models.security.ClaimRequest;
import org.akhq.models.security.ClaimResponse;
import org.akhq.security.annotation.AKHQSecured;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AKHQSecurityRule extends AbstractSecurityRule<HttpRequest<?>> {
    public static final String REJECTED_RESOURCE = "akhq.rejected.resource";
    public static final String REJECTED_ACTION = "akhq.rejected.action";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final GzipCompressionAlgorithm gzip = new GzipCompressionAlgorithm();

    /**
     * @param rolesFinder Roles Parser
     */
    public AKHQSecurityRule(RolesFinder rolesFinder) {
        super(rolesFinder);
    }

    @Inject
    private SecurityProperties securityProperties;
    @Inject
    private ClaimProvider claimProvider;

    @Override
    public Publisher<SecurityRuleResult> check(HttpRequest<?> request, Authentication authentication) {
        var routeMatchInfo = BasicHttpAttributes.getRouteMatchInfo(request);
        if (routeMatchInfo.isEmpty() || !(routeMatchInfo.get() instanceof MethodBasedRouteMatch<?, ?> methodRoute)) {
            return Mono.just(SecurityRuleResult.UNKNOWN);
        }

        if (!methodRoute.hasAnnotation(AKHQSecured.class)) {
            return Mono.just(SecurityRuleResult.UNKNOWN);
        }

        Optional<Role.Resource> optionalResource = methodRoute.getValue(AKHQSecured.class, "resource", Role.Resource.class);
        Optional<Role.Action> optionalAction = methodRoute.getValue(AKHQSecured.class, "action", Role.Action.class);
        if (optionalResource.isEmpty() || optionalAction.isEmpty()) {
            return Mono.just(SecurityRuleResult.UNKNOWN);
        }

        if (!methodRoute.getVariableValues().containsKey("cluster")) {
            log.warn("Route matched AKHQSecured but no `cluster` provided");
            return Mono.just(SecurityRuleResult.REJECTED);
        }
        String cluster = methodRoute.getVariableValues().get("cluster").toString();

        // No authentication information provided or no existing default group, we reject the request
        if (authentication == null && (securityProperties.getDefaultGroup() == null
            || securityProperties.getGroups().get(securityProperties.getDefaultGroup()) == null)) {
            log.warn("No authentication information provided");
            return Mono.just(SecurityRuleResult.REJECTED);
        }

        List<Group> userGroups = new ArrayList<>();

        if (authentication != null) {
            // Add user groups from the user token
            userGroups = unrollGroups(authentication, claimProvider).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        }

        // Add default group anyway
        if (securityProperties.getGroups().get(securityProperties.getDefaultGroup()) != null) {
            userGroups.addAll(securityProperties.getGroups().get(securityProperties.getDefaultGroup()));
        }

        boolean allowed = userGroups.stream()
            // Keep only bindings matching on cluster name
            .filter(binding -> binding.getClusters().stream()
                    .anyMatch(regex -> Pattern.matches(regex, cluster))
            )
            // Map to roles
            .map(binding -> securityProperties.getRoles().entrySet().stream()
                .filter(entry -> entry.getKey().equals(binding.getRole()))
                .flatMap(rb -> rb.getValue().stream())
                .collect(Collectors.toList()))
            // Flatten roles
            .flatMap(Collection::stream)
            // Match on Resource & Action
            .anyMatch(role -> role.getResources().contains(optionalResource.get())
                && role.getActions().contains(optionalAction.get()));

        if (allowed)
            return Mono.just(SecurityRuleResult.ALLOWED);

        request.setAttribute(REJECTED_RESOURCE, optionalResource.get().toString());
        request.setAttribute(REJECTED_ACTION, optionalAction.get().toString());
        return Mono.just(SecurityRuleResult.REJECTED);
    }

    public static Map<String, List<Group>> unrollGroups(Authentication authentication, ClaimProvider claimProvider) {
        /* For OIDC providers with useOidcGroupsInToken, the "groups" attribute contains a list of group names.
         * For other providers and OIDC without useOidcGroupsInToken, the "groups" attribute contains a map of role
         * names to lists of groups. This method creates a map of role names to lists of groups no matter the origin
         * of the "groups" attribute. */
        Object decompressedGroups = decompressGroups(authentication);
        if (decompressedGroups instanceof Map<?, ?>) {
            return toMapOfGroupLists((Map<String, List<?>>) decompressedGroups);
        }
        final String providerName = (String) authentication.getAttributes().get("provider_name");
        if (claimProvider == null || providerName == null || !(decompressedGroups instanceof List<?>)) {
            throw new RuntimeException("No ClaimProvider, or no providerName, or wrong group format");
        }
        return getClaimProviderGroups(providerName, (List<String>) decompressedGroups, authentication, claimProvider);
    }

    private static Map<String, List<Group>> toMapOfGroupLists(Map<String, List<?>> map) {
        Map<String, List<Group>> newMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<?>> entry : map.entrySet()) {
            newMap.put(entry.getKey(), toListOfGroups(entry.getValue()));
        }
        return newMap;
    }

    private static List<Group> toListOfGroups(List<?> list) {
        return list.stream()
                   .map(gb -> new ObjectMapper().convertValue(gb, Group.class))
                   .collect(Collectors.toList());
    }

    private static Object decompressGroups(Authentication authentication) {
        try {
            String base64CompressedGroups = ((String) authentication.getAttributes().get("groups"));
            String compressedGroups = new String(gzip.decompress(Base64.getDecoder().decode(base64CompressedGroups)));
            return mapper.readValue(compressedGroups, Object.class);
        } catch (Exception e) {
            log.trace("JWT payload is not compressed, returning groups directly");
            return authentication.getAttributes().get("groups");
        }
    }

    private static Map<String, List<Group>> getClaimProviderGroups(String providerName, List<String> claimGroups, Authentication authentication, ClaimProvider claimProvider) {
        Map<String, List<Group>> groups = new HashMap<>();
        ClaimRequest request = ClaimRequest.builder()
                                           .providerType(ClaimProviderType.valueOf((String) authentication.getAttributes().get("provider_type")))
                                           .providerName(providerName)
                                           .username(authentication.getName())
                                           .groups(claimGroups)
                                           .build();
        try {
            ClaimResponse claim = claimProvider.generateClaim(request);
            groups.putAll(claim.getGroups());
        } catch (Exception e) {
            log.warn("Exception from ClaimProvider " + claimProvider.getClass() + ": " + e.getMessage());
        }
        return groups;
    }

    public static final Integer ORDER = SecuredAnnotationRule.ORDER - 100;

    public int getOrder() {
        return ORDER;
    }
}
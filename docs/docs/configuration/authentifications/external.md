
# External roles and attributes mapping

If you managed which topics (or any other resource) in an external system, you have access to 2 more implementations mechanisms to map your authenticated user (from either Local, Header, LDAP or OIDC) into AKHQ roles and attributes:

If you use this mechanism, keep in mind it will take the local user's groups for local Auth, and the external groups for Header/LDAP/OIDC (ie. this will NOT do the mapping between Header/LDAP/OIDC and local groups)

**Default configuration-based**
This is the current implementation and the default one (doesn't break compatibility)
````yaml
akhq:
  security:
    default-group: no-roles
    groups:
      reader:
        roles:
          - topic/read
        attributes:
          topics-filter-regexp: [".*"]
      no-roles:
        roles: []
    ldap: # LDAP users/groups to AKHQ groups mapping
    oidc: # OIDC users/groups to AKHQ groups mapping
    header-auth: # header authentication users/groups to AKHQ groups mapping
````

**REST API**
````yaml
akhq:
  security:
    default-group: no-roles
    rest:
      enabled: true
      url: https://external.service/get-roles-and-attributes
    groups: # anything set here will not be used
````

In this mode, AKHQ will send to the ``akhq.security.rest.url`` endpoint a POST request with the following JSON :

````json
{
  "providerType": "LDAP or OIDC or BASIC_AUTH or HEADER",
  "providerName": "OIDC provider name (OIDC only)",
  "username": "user",
  "groups": ["LDAP-GROUP-1", "LDAP-GROUP-2", "LDAP-GROUP-3"]
}
````
and expect the following JSON as response :
````json
{
  "roles": ["topic/read", "topic/write", "..."],
  "topicsFilterRegexp": [".*"],
  "connectsFilterRegexp": [".*"],
  "consumerGroupsFilterRegexp": [".*"]
}
````

**Groovy API**
````yaml
akhq:
  security:
    default-group: no-roles
    groovy:
      enabled: true
      file: |
        package org.akhq.utils;
        class GroovyCustomClaimProvider implements ClaimProvider {
            @Override
            ClaimResponse generateClaim(ClaimRequest request) {
                ClaimResponse response = ClaimResponse.builder().build()
                response.roles = ["topic/read"]
                response.topicsFilterRegexp: [".*"]
                response.connectsFilterRegexp: [".*"]
                response.consumerGroupsFilterRegexp: [".*"]
                return response
            }
        }
    groups: # anything set here will not be used
````
``akhq.security.groovy.file`` must be a groovy class that implements the interface ClaimProvider :
````java
package org.akhq.utils;
public interface ClaimProvider {
    ClaimResponse generateClaim(ClaimRequest request);
}

enum ClaimProviderType {
  BASIC_AUTH,
  LDAP,
  OIDC
}

public class ClaimRequest {
  ClaimProvider.ProviderType providerType;
  String providerName;
  String username;
  List<String> groups;
}

public class ClaimResponse {
    private List<String> roles;
    private List<String> topicsFilterRegexp;
    private List<String> connectsFilterRegexp;
    private List<String> consumerGroupsFilterRegexp;
}
````

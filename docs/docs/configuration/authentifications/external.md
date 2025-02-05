
# External roles and attributes mapping

If you manage topics (or any other resource) permissions in an external system, you have access to 2 more implementation mechanisms to map your authenticated user (from either Local, Header, LDAP or OIDC) into AKHQ roles and attributes. If you use this approach, keep in mind it will take the local user's groups for local Auth, and the external groups for Header/LDAP/OIDC (ie. this will NOT do the mapping between Header/LDAP/OIDC and local groups).

**Default configuration-based**
This is the current implementation and the default one (doesn't break compatibility)
````yaml
akhq:
  security:
    default-group: admin
    groups:
      reader:
        - role: reader
        # patterns: [ ".*" ]
        # clusters: [ ".*" ]
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
  "groups": {
    "topic-writer-clusterA-projectA": [
      {
        "role": "topic-reader",
        "patterns": [
          "pub.*"
        ]
      }, {
        "role": "topic-writer",
        "patterns": [
          "projectA.*"
        ],
        "clusters": [
          "clusterA.*"
        ]
      }
    ],
    "acl-reader-clusterA": [
      {
        "role": "acl-reader",
        "clusters": [
          "clusterA.*"
        ]
      }
    ]
  }
}
````

If you want to send a static authentication token to the external service where it might be public, you can extend the configuration for the rest interface as follows:
````yaml
akhq:
  security:
    rest:
      enabled: true
      url: https://external.service/get-roles-and-attributes
      headers:
        - name: Authorization
          value: Bearer your-token
````

::: warning
The response must contain the `Content-Type: application/json` header to prevent any issue when reading the response.
:::

**Groovy API**
````yaml
akhq:
  security:
    default-group: no-roles
    groovy:
      enabled: true
      file: |
        package org.akhq.models.security;
        class GroovyCustomClaimProvider implements ClaimProvider {
            @Override
            ClaimResponse generateClaim(ClaimRequest request) {
                String filterRegexp =  request.groups.collect {
                  '^' + it + '\\..*'
                }.join('|')
                def groups = [
                  "reader": [
                    new org.akhq.configs.security.Group(role: "reader", patterns: [filterRegexp]),
                  ]
                ]
                return ClaimResponse.builder().groups(groups).build();
            }
        }
    groups: # anything set here will not be used
````
``akhq.security.groovy.file`` must be a groovy class that implements the interface ClaimProvider :
````java
package org.akhq.models.securitys;
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
  private Map<String, List<Group>> groups;
}
````

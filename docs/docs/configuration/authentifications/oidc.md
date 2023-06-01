
# OIDC
To enable OIDC in the application, you'll first have to enable OIDC in micronaut:

```yaml
micronaut:
  security:
    oauth2:
      enabled: true
      clients:
        google:
          client-id: "<client-id>"
          client-secret: "<client-secret>"
          openid:
            issuer: "<issuer-url>"
```

To further tell AKHQ to display OIDC options on the login page and customize claim mapping, configure OIDC in the AKHQ config:

```yaml
akhq:
  security:
    roles:
      topic-reader:
        - resources: [ "TOPIC", "TOPIC_DATA" ]
          actions: [ "READ" ]
        - resources: [ "TOPIC" ]
          actions: [ "READ_CONFIG" ]
      topic-writer:
        - resources: [ "TOPIC", "TOPIC_DATA" ]
          actions: [ "CREATE", "UPDATE" ]
        - resources: [ "TOPIC" ]
          actions: [ "ALTER_CONFIG" ]
    groups:
      topic-reader-pub:
        - role: topic-reader
          patterns: [ "pub.*" ]
      topic-writer-clusterA-projectA:
        - role: topic-reader
          patterns: [ "projectA.*" ]
        - role: topic-writer
          patterns: [ "projectA.*" ]
          clusters: [ "clusterA.*" ]
      acl-reader-clusterA:
        - role: acl-reader
          clusters: [ "clusterA.*" ]
    oidc:
      enabled: true
      providers:
        google:
          label: "Login with Google"
          username-field: preferred_username
          # specifies the field name in the oidc claim containing the use assigned role (eg. in keycloak this would be the Token Claim Name you set in your Client Role Mapper)
          groups-field: roles
          default-group: topic-reader
          groups:
            # the name of the user role set in your oidc provider and associated with your user (eg. in keycloak this would be a client role)
            - name: mathematicians
              groups:
                # the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)
                - topic-reader-pub
            - name: scientists
              groups:
                - topic-writer-clusterA-projectA
                - acl-reader-clusterA
          users:
            - username: franz
              groups:
                - topic-writer-clusterA-projectA
                - acl-reader-clusterA
```

The username field can be any string field, the roles field has to be a JSON array. The mapping is performed on the OIDC _ID token_.

## Direct OIDC mapping

If you want to manage AKHQ roles an attributes directly with the OIDC provider, you can use the following configuration:
```yaml
akhq:
  security:
    oidc:
      enabled: true
      providers:
        google:
          label: "Login with Google"
          username-field: preferred_username
          use-oidc-claim: true
````

In this scenario, you need to make the OIDC provider return a JWT which have the following fields:
````json
{
  // Standard claims
  "exp": 1635868816,
  "iat": 1635868516,
  "preferred_username": "json",
  ...
  "scope": "openid email profile",
  // Mandatory AKHQ claims
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
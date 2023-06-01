# LDAP

Configure how the ldap groups will be matched in AKHQ groups
* `akhq.security.ldap.groups`: Ldap groups list
  * `- name: ldap-group-name`: Ldap group name (same name as in ldap)
    * `groups`: AKHQ group list to be used for current ldap group

Example using [online ldap test server](https://www.forumsys.com/tutorials/integration-how-to/ldap/online-ldap-test-server/)

Configure ldap connection in micronaut
```yaml
micronaut:
  security:
    enabled: true
    ldap:
      default:
        enabled: true
        context:
          server: 'ldap://ldap.forumsys.com:389'
          managerDn: 'cn=read-only-admin,dc=example,dc=com'
          managerPassword: 'password'
        search:
          base: "dc=example,dc=com"
        groups:
          enabled: true
          base: "dc=example,dc=com"
```

If you want to enable anonymous auth to your LDAP server you can pass :
```yaml
managerDn: ''
managerPassword: ''
```

In Case your LDAP groups do not use the default UID for group membership, you can solve this using

```yaml
micronaut:
  security:
    enabled: true
    ldap:
      default:
        search:
          base: "OU=UserOU,dc=example,dc=com"
          attributes:
            - "cn"
        groups:
          enabled: true
          base: "OU=GroupsOU,dc=example,dc=com"
          filter: "member={0}"
```
Replace
```yaml
attributes:
  - "cn"
```
with your group membership attribute

Configure AKHQ groups and Ldap groups and users
```yaml
micronaut:
  security:
    enabled: true
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
    ldap:
      groups:
        - name: mathematicians
          groups:
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


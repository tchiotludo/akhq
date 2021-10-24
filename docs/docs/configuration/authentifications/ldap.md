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
    groups:
      topic-reader:
        name: topic-reader # Group name
        roles:  # roles for the group
          - topic/read
        attributes:
          # List of Regexp to filter topic available for group
          # Single line String also allowed
          # topics-filter-regexp: "^(projectA_topic|projectB_.*)$"
          topics-filter-regexp:
            - "^projectA_topic$" # Individual topic
            - "^projectB_.*$" # Topic group
          connects-filter-regexp:
            - "^test.*$"
          consumer-groups-filter-regexp:
            - "consumer.*"
      topic-writer:
        name: topic-writer # Group name
        roles:
          - topic/read
          - topic/insert
          - topic/delete
          - topic/config/update
        attributes:
          topics-filter-regexp:
            - "test.*"
          connects-filter-regexp:
            - "^test.*$"
          consumer-groups-filter-regexp:
            - "consumer.*"
    ldap:
      groups:
        - name: mathematicians
          groups:
            - topic-reader
        - name: scientists
          groups:
            - topic-reader
            - topic-writer
      users:
        - username: franz
          groups:
            - topic-reader
            - topic-writer

```


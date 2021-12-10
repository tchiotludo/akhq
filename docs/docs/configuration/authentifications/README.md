# Authentifications

* `akhq.security.default-group`: Default group for all the user even unlogged user.
  By default, the default group is `admin` and allow you all read / write access on the whole app.

By default, security & roles is disabled and anonymous user have full access, i.e. `micronaut.security.enabled: false`.
To enable security & roles set `micronaut.security.enabled: true` and configure desired type of authentication (basic auth, LDAP, etc.).

If you need a read-only application, simply add this to your configuration files :
```yaml
akhq:
  security:
    default-group: reader
```

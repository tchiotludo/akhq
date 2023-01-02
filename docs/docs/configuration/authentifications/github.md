
# GitHub SSO / OAuth2
To enable GitHub SSO in the application, you'll first have to enable OAuth2 in micronaut:

```yaml
micronaut:
  security:
    enabled: true
    oauth2:
      enabled: true
      clients:
        github:
          client-id: "<client-id>"
          client-secret: "<client-secret>"
          scopes:
            - user:email
            - read:user
          authorization:
            url: https://github.com/login/oauth/authorize
          token:
            url: https://github.com/login/oauth/access_token
            auth-method: client-secret-post
```

To further tell AKHQ to display GitHub SSO options on the login page and customize claim mapping, configure Oauth in the AKHQ config:

```yaml
akhq:
  security:
    default-group: no-roles
    oauth2:
      enabled: true
      providers:
        github:
          label: "Login with GitHub"
          username-field: login
          users:
            - username: franz
              groups:
                # the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)
                - topic-reader
                - topic-writer
```

The username field can be any string field, the roles field has to be a JSON array.

## References
https://micronaut-projects.github.io/micronaut-security/latest/guide/#oauth2-configuration

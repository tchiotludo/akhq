
# Header configuration (reverse proxy)

To enable Header authentication in the application, you'll have to configure the header that will resolve users & groups:

```yaml
akhq:
  security:
    # Header configuration (reverse proxy)
    header-auth:
      user-header: x-akhq-user # mandatory (the header name that will contain username)
      groups-header: x-akhq-group # optional (the header name that will contain groups separated by groups-header-separator)
      groups-header-separator: , # optional (separator, defaults to ',')
      ip-patterns: [0.0.0.0] # optional (Java regular expressions for matching trusted IP addresses, '0.0.0.0' matches all addresses)
      default-group: topic-reader
      groups: # optional
        # the name of the user group read from header
        - name: header-admin-group
          groups:
            # the corresponding akhq groups (eg. topic-reader/writer or akhq default groups like admin/reader/no-role)
            - admin
      users: # optional
        - username: header-user # username matching the `user-header` value
          groups: # list of groups / additional groups
            - topic-writer
        - username: header-admin
          groups:
            - admin
```

* `user-header` is mandatory in order to map the user with `users` list or to display the user on the ui if no `users` is provided.
* `groups-header` is optional and can be used in order to inject a list of groups for all the users. This list will be merged with `groups` for the current users.
* `groups-header-separator` is optional and can be used to customize group separator used when parsing `groups-header` header, defaults to `,`.
* `ip-patterns` limits the IP addresses that header authentication will accept, given as a list of Java regular expressions, omit or set to `[0.0.0.0]` to allow all addresses
* `default-group` default AKHQ group, used when no groups were read from `groups-header`
* `groups` maps external group names read from headers to AKHQ groups.
* `users` assigns additional AKHQ groups to users.

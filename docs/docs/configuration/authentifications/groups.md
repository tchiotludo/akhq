# Groups

Groups allow you to limit user

Define groups with specific roles for your users
* `akhq.security.default-group`: Default group for all the user even unlogged user

* `akhq.security.groups`: Groups map definition
  * `key:` a uniq key used as name if not specified
    * `  name: group-name` Group identifier
    * `roles`: Roles list for the group
    * `attributes.topics-filter-regexp`: Regexp list to filter topics available for current group
    * `attributes.connects-filter-regexp`: Regexp list to filter Connect tasks available for current group
    * `attributes.consumer-groups-filter-regexp`: Regexp list to filter Consumer Groups available for current group
    * `attributes.acls-filter-regexp`: Regexp list to filter acls available for current group

::: warning
`topics-filter-regexp`, `connects-filter-regexp`, `consumer-groups-filter-regexp` and `acls-filter-regexp` are only used when listing resources.
If you have `topics/create` or `connect/create` roles and you try to create a resource that doesn't follow the regexp, that resource **WILL** be created.
:::

3 defaults group are available :
- `admin` with all right
- `reader` with only read access on all AKHQ
- `no-roles` without any roles, that force user to login

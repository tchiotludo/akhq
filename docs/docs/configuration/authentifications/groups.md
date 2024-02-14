# Groups

Groups allow you to limit user

::: warning
With PR #1472 AKHQ introduced a new, better group management system. It was merged into main codebase after
release 0.24.0. If you are using v0.24.0 or below, please refer to documentation in `docs/` subdirectory
as shipped with your release.
:::

Define groups with specific roles for your users
* `akhq.security.default-group`: Default group for all the user even unlogged user

* `akhq.security.groups`: Groups map definition
  * `key:` a uniq key used as name if not specified
    * A list of role/patterns/names/clusters association
      * `role`: name of an existing role
      * `patterns`: list of regular expression that resources from the given role must match at least once get access (default `.*`)
      * `names`: list of names that resources from the given role must match exactly, if both patterns and names are set
         then akhq uses a logic `or`, to be use alone the `patterns` must be set to an empty list
      * `clusters`: list of regular expression that cluster must match at least once to get access (default `.*`)

::: warning
Please also set the `micronaut.security.token.jwt.signatures.secret.generator.secret` if you set a group.
If the secret is not set, the API will not enforce the group role, and the restriction is in the UI only.
:::

3 defaults group are available :
- `admin` with all right and no patterns/clusters restrictions
- `reader` with only read access on all AKHQ and no patterns/clusters restrictions
- `no-roles` without any roles, that force user to login

Here is an example of a `reader` group definition based on the default reader role with access on all the resources prefixed with `pub` and located the on `public` cluster
```yaml
    groups:
      reader:
        - role: reader
          patterns: [ "pub.*" ]
          clusters: [ "public" ]
```

Hereafter is the same example including all topics matching the pattern `pub.*` and one single topic with name `subscriber`.

```yaml
    groups:
      reader:
        - role: reader
          patterns: [ "pub.*" ]
          names: ["subscriber"]
          clusters: [ "public" ]
```

## Roles

Roles are based on Resource and Action association. A role can target one or several Resource and allow one or several Action.
The resources and actions list + possible associations between them are detailed in the table below.
You can still associate a resource with a non-supported action from the table. It will just be ignored

<div style="text-align: center;">

|                | TOPIC | TOPIC_DATA | CONSUMER_GROUP | CONNECT_CLUSTER | CONNECTOR | SCHEMA | NODE | ACL | KSQLDB |
|----------------|-------|------------|----------------|-----------------|-----------|--------|------|-----|--------|
| READ           | X     | X          | X              | X               | X         | X      | X    | X   | X      |
| CREATE         | X     | X          |                |                 | X         | X      |      |     |        |
| UPDATE         | X     | X          |                |                 |           | X      |      |     |        |
| DELETE         | X     | X          |                |                 | X         | X      |      |     |        |
| UPDATE_OFFSET  |       |            | X              |                 |           |        |      |     |        |
| DELETE_OFFSET  |       |            | X              |                 |           |        |      |     |        |
| READ_CONFIG    | X     |            |                |                 |           |        | X    |     |        |
| ALTER_CONFIG   | X     |            |                |                 |           |        | X    |     |        |
| DELETE_VERSION |       |            |                |                 |           | X      |      |     |        |
| UPDATE_STATE   |       |            |                |                 | X         |        |      |     |        |
| EXECUTE        |       |            |                |                 |           |        |      |     | X      |

</div>

A default roles list is predefined in `akhq.security.roles` but you can override it.
A role contains:
* `key:` a uniq key used as name
  * A list of resources/actions associations
    * `resources:` List of resources (ex: ```[ "TOPIC", "TOPIC_DATA"]```)
    * `actions:` Actions allowed on the previous resources (ex: ```[ "READ", "CREATE"]```)

The default configuration provides a topic-admin role defined as follows:
```yaml
topic-admin:
  - resources: [ "TOPIC", "TOPIC_DATA" ]
    actions: [ "READ", "CREATE", "DELETE" ]
  - resources: [ "TOPIC" ]
    actions: [ "UPDATE", "READ_CONFIG", "ALTER_CONFIG" ]

```

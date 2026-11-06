# Minimal Required ACLs for AKHQ

To run AKHQ against a secured Kafka cluster, the principal (user) used by AKHQ needs the following minimum set of permissions.
In this example, we assume the user is `User:ANONYMOUS`.

## Cluster Permissions
The following permission is required for AKHQ to discover the cluster and its brokers:
- Operation: `Describe`
- Resource: `Cluster`

## Topic Permissions
To view topics, their configurations, and read messages, the following permissions are needed for all topics (`*`):
- Operation: `Describe`
- Operation: `Read`
- Operation: `DescribeConfigs`
- Resource: `Topic` (all topics: `*`)

## Consumer Group Permissions
To view consumer groups and their offsets, the following permissions are needed:
- Operation: `Describe`
- Operation: `Read`
- Resource: `Group` (all groups: `*`)
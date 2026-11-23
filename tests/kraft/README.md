# KRaft controller visibility checks

The Nodes page keeps broker operations separate from metadata quorum voters. It
queries `GET /api/{cluster}/node/quorum` in parallel with the broker list and shows
controller IDs, their roles, the quorum leader, available endpoints and replication
lag. `Refresh nodes` performs a new quorum query, including after an election.

The existing broker list and broker log/configuration APIs remain unchanged.
The broker table has no controller-leader flag. Both tables show `Broker` and/or
`Controller` badges for the same node ID, so a combined node is recognisable in
both views. Active status appears only in the controller table, with `Active`,
`Not active`, or `Unknown`. The compatibility controller ID from broker metadata
is never used for this status; it remains in the existing API.

The summary counts visible brokers, controller voters, and nodes present in both
results. These are observed roles, not a claim about configured `process.roles`
or process health. A fenced/offline broker role can disappear from broker
discovery while its controller voter remains in the quorum. Unknown permissions,
failed discovery, and an unavailable/hidden leader must not be reported as a
negative role or healthy standby. A zero offset, lag, or node ID is still valid.

| Layout | Brokers view | Controllers view |
| --- | --- | --- |
| Dedicated | Broker badges | Controller badges; one Active voter |
| Combined | Broker + Controller badges | The same IDs and badges; one Active voter |
| Mixed | Both badges on shared IDs | Both badges on shared IDs; Controller on other voters |
| Quorum unavailable | Brokers retained; controller role unknown | Specific failure message |
| Broker discovery fails | Broker discovery error | Voters retained; broker role unknown |
| Leader hidden/unknown | Roles retained | Unknown active status; no invented inactive voters |

## Java checks

```sh
./gradlew test --tests org.akhq.repositories.MetadataQuorumRepositoryTest \
  --tests org.akhq.controllers.NodeControllerQuorumTest
```

These tests use the project's existing test task and runtime dependencies. That
task also starts the shared Kafka, Schema Registry, Connect and ksqlDB fixture.
Run `./gradlew check` for the complete project checks.

The integration check is skipped unless `AKHQ_KRAFT_BOOTSTRAP` is set. The unit
checks cover voter IDs without endpoints, IPv6 endpoints, observer exclusion,
leader changes, unknown offsets, timeouts, missing Kafka permissions and filtering
node IDs according to AKHQ permissions.

## Apache Kafka 3.9 integration check

The Compose file creates a separate test network with three controller-only nodes
(IDs 100–102) and three brokers (IDs 1–3). Client ports bind to host loopback only.
It does not connect to an existing cluster or modify existing containers.

```sh
docker compose -f tests/kraft/docker-compose.yml up -d
docker compose -f tests/kraft/docker-compose.yml exec -T broker1 \
  /opt/kafka/bin/kafka-metadata-quorum.sh \
  --bootstrap-server broker1:9092 describe --status

AKHQ_KRAFT_BOOTSTRAP=localhost:19092 ./gradlew test \
  --tests org.akhq.controllers.MetadataQuorumIntegrationTest
```

Run Java on the Docker host, or use a Java container on the Compose network with
`broker1:9092` as bootstrap. A Windows JVM may not reach a WSL/remote Docker daemon
through `localhost`. The test starts AKHQ on an ephemeral loopback HTTP port and
compares the new API's voters and leader with Kafka's Admin API, while confirming
that the existing broker API still returns the three brokers separately.

For an election check, stop only the test controller identified as leader by the
CLI, wait for a new leader and repeat the HTTP integration check. Restart the
stopped test controller afterwards. Voters remain members of a static quorum even
when stopped; membership and missing timestamps are not health checks.

After testing, stop the isolated cluster:

```sh
docker compose -f tests/kraft/docker-compose.yml down
```

## UI checks

```sh
cd client
npm ci
npm test -- --run src/containers/Node/NodeList/MetadataQuorum.test.jsx \
  src/containers/Node/NodeList/NodesList.test.jsx src/utils/endpoints.test.jsx
npm run build
```

These checks cover controller-only and combined roles, optional endpoints and
timestamps, broker discovery failures, failed quorum queries and stale responses
after changing clusters. Dynamic quorum endpoint conversion is covered by a unit
test; the provided live cluster uses a static Kafka 3.9.1 quorum.

## Additional combined and mixed live clusters

`docker-compose-layouts.yml` adds two isolated profiles using the same Apache
Kafka 3.9.1 image. It does not change the dedicated cluster above.

```sh
docker compose -f tests/kraft/docker-compose-layouts.yml --profile combined --profile mixed up -d
```

| Profile | Broker IDs | Controller IDs | Host bootstrap ports |
| --- | --- | --- | --- |
| combined | 11, 12, 13 | 11, 12, 13 | localhost:19192, localhost:29192, localhost:39192 |
| mixed | 21, 24, 25 | 21, 22, 23 | localhost:19292, localhost:29292, localhost:39292 |

Combined IDs 11–13 have `process.roles=broker,controller`. In mixed mode only ID
21 has both roles; 22–23 are controller-only and 24–25 are broker-only.

When AKHQ runs in Docker, attach it to `akhq-kraft-layouts_combined` and/or
`akhq-kraft-layouts_mixed`, and configure separate connections with internal
bootstrap addresses `combined1:9092` and `mixed-combined:9092`. The host ports bind
to loopback. All three controllers and brokers should be ready before role and
RF=3 producer/consumer checks.

After testing, stop only these additional clusters with:

```sh
docker compose -f tests/kraft/docker-compose-layouts.yml --profile combined --profile mixed down
```

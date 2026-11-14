# Kafka Compatibility Matrix

This page documents the tested compatibility between **AKHQ (latest)** and various versions of **Apache Kafka**, **Zookeeper**, and **Confluent Platform**.
All entries below were verified by running Kafka, Zookeeper, and AKHQ via Docker, creating topics, producing and consuming messages, and validating AKHQ UI functionality.

This table is intended as a practical reference for users deploying AKHQ against older or newer Kafka clusters.


## Summary

* All Kafka versions **2.5 through 3.6** were tested.
* All versions successfully connected to AKHQ, listed topics, created new topics, produced and consumed messages.
* From Kafka **3.0+**, the ZooKeeper CLI interaction (`--zookeeper`) is intentionally removed and must be replaced with `--bootstrap-server`.
* No critical failures were observed.


## Compatibility Table

| Kafka Version | Zookeeper Version | Confluent Platform Version | AKHQ UI Status | CLI (Producer / Consumer) | Topic CRUD | Notes                                 |
|--------------:| ----------------: | -------------------------: | -------------- | ------------------------- | ---------- | ------------------------------------- |
|       **2.5** |               3.5 |                        5.5 | OK             | OK                        | OK         | Uses old ZK-based CLI (`--zookeeper`) |
|       **2.6** |               3.5 |                        5.5 | OK             | OK                        | OK         | —                                     |
|       **2.7** |               3.5 |                        5.5 | OK             | OK                        | OK         | —                                     |
|       **2.8** |               3.6 |                        6.2 | OK             | OK                        | OK         | —                                     |
|       **3.0** |               3.6 |                        7.0 | OK             | OK                        | OK         | `--zookeeper` no longer supported     |
|       **3.1** |               3.6 |                        7.1 | OK             | OK                        | OK         | Same as above                         |
|       **3.2** |               3.6 |                        7.2 | OK             | OK                        | OK         | —                                     |
|       **3.3** |               3.7 |                        7.3 | OK             | OK                        | OK         | —                                     |
|  **3.4**(LTS) |               3.8 |                        7.4 | OK             | OK                        | OK         | —                                     |
|       **3.5** |               3.8 |                        7.5 | OK             | OK                        | OK         | —                                     |
|  **3.6**(LTS) |               3.8 |                        7.6 | OK             | OK                        | OK         | Slower UI load depends on host        |

## Test Methodology

Each version was validated using the following process:

1. **Kafka + Zookeeper + AKHQ containers** started via Docker.
2. Waited until brokers reached stable state and registered in Zookeeper (or KRaft controller for newer versions).
3. Executed:

  * `kafka-topics` list
  * topic creation
  * producing messages
  * consuming messages
4. Verified:

  * AKHQ UI loads
  * Topics list shows real-time updates
  * Message browsing
  * Consumer group visibility
5. Captured any warnings (e.g., `LEADER_NOT_AVAILABLE`) and confirmed they resolved after metadata propagation.



## Notes on Newer Kafka Versions (3.0+)

Kafka 3.0 removed the Zookeeper-based CLI flags:

*  `--zookeeper`
*  Use `--bootstrap-server` instead

This is not an AKHQ limitation but a Kafka CLI change.
AKHQ works normally with both Zookeeper and KRaft-based clusters as long as Kafka exposes its public listener correctly.



## Conclusion

AKHQ remains compatible across a wide range of Kafka and Confluent versions, including legacy clusters and the newer 3.x releases.
Users upgrading Kafka should experience no UI or operational regressions when using the latest AKHQ release.


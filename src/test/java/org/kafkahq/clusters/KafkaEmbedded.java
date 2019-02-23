package org.kafkahq.clusters;


import com.google.common.io.Files;
import kafka.server.KafkaConfig;
import kafka.server.KafkaConfig$;
import kafka.server.KafkaServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.utils.Time;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Runs an in-memory, "embedded" instance of a Kafka broker, which listens at `127.0.0.1:9092` by
 * default.
 * <p>
 * Requires a running ZooKeeper instance to connect to.  By default, it expects a ZooKeeper instance
 * running at `127.0.0.1:2181`.  You can specify a different ZooKeeper instance by setting the
 * `zookeeper.connect` parameter in the broker's configuration.
 */
@Slf4j
public class KafkaEmbedded {
    private static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    private final Properties effectiveConfig;
    private final File logDir;
    private final File tmpFolder;
    private final KafkaServer kafka;

    /**
     * Creates and starts an embedded Kafka broker.
     *
     * @param config Broker configuration settings.  Used to modify, for example, on which port the
     *               broker should listen to.  Note that you cannot change some settings such as
     *               `log.dirs`, `port`.
     */
    @SuppressWarnings("UnstableApiUsage")
    public KafkaEmbedded(final Properties config) throws IOException {
        tmpFolder = Files.createTempDir();
        logDir = Files.createTempDir();
        effectiveConfig = effectiveConfigFrom(config);
        final boolean loggingEnabled = true;

        final KafkaConfig kafkaConfig = new KafkaConfig(effectiveConfig, loggingEnabled);
        log.debug(
            "Starting embedded Kafka broker (with log.dirs={} and ZK ensemble at {}) ...",
            logDir,
            zookeeperConnect()
        );

        kafka = new KafkaServer(
            kafkaConfig,
            Time.SYSTEM,
            scala.Option.empty(),
            scala.collection.immutable.List.empty()
        );
        kafka.startup();

        log.debug("Startup of embedded Kafka broker at {} completed (with ZK ensemble at {}) ...", brokerList(), zookeeperConnect());
    }

    private Properties effectiveConfigFrom(final Properties initialConfig) throws IOException {
        final Properties effectiveConfig = new Properties();
        effectiveConfig.put(KafkaConfig$.MODULE$.BrokerIdProp(), 0);
        effectiveConfig.put(KafkaConfig$.MODULE$.HostNameProp(), "127.0.0.1");
        effectiveConfig.put(KafkaConfig$.MODULE$.PortProp(), "9092");
        effectiveConfig.put(KafkaConfig$.MODULE$.NumPartitionsProp(), 1);
        effectiveConfig.put(KafkaConfig$.MODULE$.AutoCreateTopicsEnableProp(), true);
        effectiveConfig.put(KafkaConfig$.MODULE$.MessageMaxBytesProp(), 1000000);
        effectiveConfig.put(KafkaConfig$.MODULE$.ControlledShutdownEnableProp(), true);

        effectiveConfig.putAll(initialConfig);
        effectiveConfig.setProperty(KafkaConfig$.MODULE$.LogDirProp(), logDir.getAbsolutePath());
        return effectiveConfig;
    }

    /**
     * This broker's `metadata.broker.list` value.  Example: `127.0.0.1:9092`.
     * <p>
     * You can use this to tell Kafka producers and consumers how to connect to this instance.
     */
    public String brokerList() {
        return String.join(
            ":",
            kafka.config().hostName(),
            Integer.toString(kafka.boundPort(ListenerName.forSecurityProtocol(SecurityProtocol.PLAINTEXT)))
        );
    }


    /**
     * The ZooKeeper connection string aka `zookeeper.connect`.
     */
    public String zookeeperConnect() {
        return effectiveConfig.getProperty("zookeeper.connect", DEFAULT_ZK_CONNECT);
    }

    /**
     * Stop the broker.
     */
    public void stop() {
        log.debug("Shutting down embedded Kafka broker at {} (with ZK ensemble at {}) ...",
            brokerList(), zookeeperConnect()
        );
        kafka.shutdown();
        kafka.awaitShutdown();
        log.debug("Removing temp folder {} with logs.dir at {} ...", tmpFolder, logDir);
        tmpFolder.delete();
        logDir.delete();
        log.debug("Shutdown of embedded Kafka broker at {} completed (with ZK ensemble at {}) ...",
            brokerList(), zookeeperConnect()
        );
    }
}
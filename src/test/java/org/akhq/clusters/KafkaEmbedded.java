package org.akhq.clusters;

import com.google.common.io.Files;
import kafka.server.KafkaConfig;
import kafka.server.KafkaConfig$;
import kafka.server.KafkaServer;
import kafka.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.network.ListenerName;
import org.apache.kafka.common.utils.Time;

import java.io.File;
import java.util.Properties;

@Slf4j
public class KafkaEmbedded {
    private static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";
    private final Properties effectiveConfig;
    private final File logDir;
    private final File tmpFolder;
    private final KafkaServer kafka;

    @SuppressWarnings("UnstableApiUsage")
    public KafkaEmbedded(final Properties config) {
        tmpFolder = Files.createTempDir();
        logDir = Files.createTempDir();
        effectiveConfig = effectiveConfigFrom(config);

        final KafkaConfig kafkaConfig = new KafkaConfig(effectiveConfig, true);
        log.debug(
            "Starting embedded Kafka broker (with log.dirs={} and ZK ensemble at {}) ...",
            logDir,
            zookeeperConnect()
        );

        kafka = TestUtils.createServer(kafkaConfig, Time.SYSTEM);
        kafka.startup();

        log.debug("Startup of embedded Kafka broker at {} completed (with ZK ensemble at {}) ...", brokerList(), zookeeperConnect());
    }

    private Properties effectiveConfigFrom(final Properties initialConfig) {
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

    public String brokerList() {
        final Object listenerConfig = effectiveConfig.get(KafkaConfig$.MODULE$.InterBrokerListenerNameProp());
        return kafka.config().hostName() + ":" + kafka.boundPort(
            new ListenerName(listenerConfig != null ? listenerConfig.toString() : "PLAINTEXT"));
    }

    public String zookeeperConnect() {
        return effectiveConfig.getProperty("zookeeper.connect", DEFAULT_ZK_CONNECT);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
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

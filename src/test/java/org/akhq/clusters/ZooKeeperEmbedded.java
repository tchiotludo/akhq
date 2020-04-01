package org.akhq.clusters;


import lombok.extern.slf4j.Slf4j;
import org.apache.curator.test.TestingServer;

import java.io.IOException;

/**
 * Runs an in-memory, "embedded" instance of a ZooKeeper server.
 *
 * The ZooKeeper server instance is automatically started when you create a new instance of this class.
 */
@Slf4j
public class ZooKeeperEmbedded {
    private final TestingServer server;
    static {
        System.setProperty("zookeeper.admin.enableServer", "false");
    }

    /**
     * Creates and starts a ZooKeeper instance.
     */
    public ZooKeeperEmbedded() throws Exception {
        log.debug("Starting embedded ZooKeeper server...");
        this.server = new TestingServer();
        log.debug("Embedded ZooKeeper server at {} uses the temp directory at {}", server.getConnectString(), server.getTempDirectory());
    }

    public void stop() throws IOException {
        log.debug("Shutting down embedded ZooKeeper server at {} ...", server.getConnectString());
        server.close();
        log.debug("Shutdown of embedded ZooKeeper server at {} completed", server.getConnectString());
    }

    /**
     * The ZooKeeper connection string aka `zookeeper.connect` in `hostnameOrIp:port` format.
     * Example: `127.0.0.1:2181`.
     *
     * You can use this to e.g. tell Kafka brokers how to connect to this instance.
     */
    public String connectString() {
        return server.getConnectString();
    }

    /**
     * The hostname of the ZooKeeper instance.  Example: `127.0.0.1`
     */
    public String hostname() {
        // "server:1:2:3" -> "server:1:2"
        return connectString().substring(0, connectString().lastIndexOf(':'));
    }

}

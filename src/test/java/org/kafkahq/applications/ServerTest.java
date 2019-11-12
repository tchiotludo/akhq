package org.kafkahq.applications;

import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;
import org.kafkahq.AbstractTest;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerTest extends AbstractTest {
    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void testItWorks() {
        assertTrue(embeddedServer.isRunning());
    }

}

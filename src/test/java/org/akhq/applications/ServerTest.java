package org.akhq.applications;

import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;

import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerTest extends AbstractTest {
    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void testItWorks() {
        assertTrue(embeddedServer.isRunning());
    }

}

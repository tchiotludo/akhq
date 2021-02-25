package org.akhq.applications;

import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.akhq.AbstractTest;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerTest extends AbstractTest {
    @Inject
    EmbeddedServer embeddedServer;

    @Test
    @Tag("verified")
    void testItWorks() {
        assertTrue(embeddedServer.isRunning());
    }

}

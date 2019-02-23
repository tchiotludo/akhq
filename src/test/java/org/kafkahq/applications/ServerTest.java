package org.kafkahq.applications;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class ServerTest {

    @Inject
    EmbeddedServer embeddedServer;

    @Test
    void testItWorks() {
        assertTrue(embeddedServer.isRunning());
    }

}

package org.akhq.configs.security;

import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GcpIamAuthTest {

    @Test
    void testGcpLoginCallbackHandlerAvailability() throws ClassNotFoundException {
        Class<?> clazz = Class.forName("com.google.cloud.hosted.kafka.auth.GcpLoginCallbackHandler");
        assertTrue(AuthenticateCallbackHandler.class.isAssignableFrom(clazz));
    }
}

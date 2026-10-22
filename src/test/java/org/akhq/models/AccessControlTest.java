package org.akhq.models;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessControlTest {
    @Test
    void shouldRoundTripUtf8Principals() {
        assertRoundTrip("User:alice");
        assertRoundTrip("User:张伟");
        assertRoundTrip("User:Renée");
        assertRoundTrip("User:प्रिय");
        assertRoundTrip("User:service/account+ops");
        assertRoundTrip("");
    }

    @Test
    void shouldPreserveAsciiPrincipalEncoding() {
        String principal = "User:CN=akhq,O=example";

        assertEquals("VXNlcjpDTj1ha2hxLE89ZXhhbXBsZQ==", AccessControl.encodePrincipal(principal));
        assertEquals(
            AccessControl.encodePrincipal(principal),
            Base64.getEncoder().encodeToString(principal.getBytes(StandardCharsets.US_ASCII))
        );
    }

    @Test
    void shouldDecodeUrlEncodedPrincipalPathSegments() {
        assertEquals("User:प्रिय", AccessControl.decodePrincipal("VXNlcjrgpKrgpY3gpLDgpL%2FgpK8%3D"));
        assertEquals(
            "User:CN=张伟,OU=开发部,O=例公司,C=CN",
            AccessControl.decodePrincipal("VXNlcjpDTj3lvKDkvJ8sT1U95byA5Y%2BR6YOoLE895L6L5YWs5Y%2B4LEM9Q04%3D")
        );
    }

    private void assertRoundTrip(String principal) {
        assertEquals(principal, AccessControl.decodePrincipal(AccessControl.encodePrincipal(principal)));
    }
}

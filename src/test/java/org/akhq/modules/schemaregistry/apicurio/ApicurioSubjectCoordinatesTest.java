package org.akhq.modules.schemaregistry.apicurio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApicurioSubjectCoordinatesTest {

    @Test
    void parseAndFormatWithoutConfiguredGroup() {
        ApicurioSubjectCoordinates defaultCoords = ApicurioSubjectCoordinates.parse("orders-value", null);
        assertEquals("default", defaultCoords.groupId());
        assertEquals("orders-value", defaultCoords.artifactId());
        assertEquals("orders-value", ApicurioSubjectCoordinates.format(defaultCoords.groupId(), defaultCoords.artifactId(), null));
        assertEquals("orders-value", ApicurioSubjectCoordinates.format(null, "orders-value", null));

        ApicurioSubjectCoordinates customCoords = ApicurioSubjectCoordinates.parse("com.acme.orders:orders-value", null);
        assertEquals("com.acme.orders", customCoords.groupId());
        assertEquals("orders-value", customCoords.artifactId());
        assertEquals("com.acme.orders:orders-value", ApicurioSubjectCoordinates.format("com.acme.orders", "orders-value", null));
    }

    @Test
    void parseExplicitDefaultPrefix() {
        ApicurioSubjectCoordinates coords = ApicurioSubjectCoordinates.parse("default:orders-value", null);
        assertEquals("default", coords.groupId());
        assertEquals("orders-value", coords.artifactId());
        assertEquals("orders-value", ApicurioSubjectCoordinates.format("default", "orders-value", null));
    }

    @Test
    void parseAndFormatWithConfiguredGroup() {
        ApicurioSubjectCoordinates implicitCoords = ApicurioSubjectCoordinates.parse("orders-value", "com.acme");
        assertEquals("com.acme", implicitCoords.groupId());
        assertEquals("orders-value", implicitCoords.artifactId());
        assertEquals("orders-value", ApicurioSubjectCoordinates.format("com.acme", "orders-value", "com.acme"));

        ApicurioSubjectCoordinates otherGroupCoords = ApicurioSubjectCoordinates.parse("other:payments-value", "com.acme");
        assertEquals("other", otherGroupCoords.groupId());
        assertEquals("payments-value", otherGroupCoords.artifactId());
        assertEquals("other:payments-value", ApicurioSubjectCoordinates.format("other", "payments-value", "com.acme"));
        assertEquals("default:legacy-value", ApicurioSubjectCoordinates.format("default", "legacy-value", "com.acme"));
    }

    @Test
    void parseRejectsBlankSubject() {
        assertThrows(IllegalArgumentException.class, () -> ApicurioSubjectCoordinates.parse(null, null));
        assertThrows(IllegalArgumentException.class, () -> ApicurioSubjectCoordinates.parse("   ", null));
    }

    @Test
    void parseHandlesLeadingOrTrailingColonAsArtifactId() {
        ApicurioSubjectCoordinates leading = ApicurioSubjectCoordinates.parse(":odd-artifact", null);
        assertEquals("default", leading.groupId());
        assertEquals(":odd-artifact", leading.artifactId());

        ApicurioSubjectCoordinates trailing = ApicurioSubjectCoordinates.parse("odd-artifact:", null);
        assertEquals("default", trailing.groupId());
        assertEquals("odd-artifact:", trailing.artifactId());
    }
}

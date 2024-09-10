package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.akhq.utils.MaskerTestHelper.sampleRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest(environments = "no-op-data-masking")
public class NoOpMaskerTest {

    @Inject
    Masker masker;

    @Test
    void shouldUseNoOpMasker() {
        assertInstanceOf(NoOpMasker.class, masker);
    }

    @Test
    void shouldDoNothing() {
        Record record = sampleRecord("some-topic", "some-key",
            """
                {
                    "specialId": "MySpecialId",
                    "firstName": "Test",
                    "lastName": "Testington",
                    "age": 100,
                    "status": "ACTIVE",
                    "metadata": {
                        "comments": "Some comment",
                        "trusted": true,
                        "expired": false,
                        "rating": "A"
                    }
                }
                """);
        Record maskedRecord = masker.maskRecord(record);
        assertEquals(
            record,
            maskedRecord
        );
    }
}

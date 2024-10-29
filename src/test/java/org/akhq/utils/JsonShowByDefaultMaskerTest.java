package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest(environments = "json-show-by-default-data-masking")
class JsonShowByDefaultMaskerTest extends MaskerTestHelper {

    @Inject
    Masker masker;

    @Test
    void shouldUseJsonShowByDefaultMasker() {
        assertInstanceOf(JsonShowByDefaultMasker.class, masker);
    }

    @Test
    void shouldMaskRecordValue() {
        Record record = sampleRecord(
            "my-special-topic",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"specialId\":\"xxxx\",\"firstName\":\"Test\",\"lastName\":\"Testington\",\"age\":100,\"status\":\"xxxx\",\"metadata\":{\"comments\":\"Some comment\",\"trusted\":\"xxxx\",\"expired\":false,\"rating\":\"xxxx\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    void shouldDoNothingForUndefinedTopic() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            sampleValue(),
            maskedRecord.getValue()
        );
    }

    @Test
    void forTombstoneShouldReturnItself() {
        Record record = sampleRecord(
            "another-topic",
            "some-key",
            null
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            record,
            maskedRecord
        );
    }

    @Test
    void forNonJsonValueShouldReturnItself() {
        Record record = sampleRecord(
            "another-topic",
            "some-key",
            "not a valid json"
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            record,
            maskedRecord
        );
    }

    private String sampleValue() {
        return """
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
            """;
    }
}

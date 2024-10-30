package org.akhq.utils;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.akhq.models.Record;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@MicronautTest(environments = "json-mask-by-default-data-masking")
class JsonMaskByDefaultMaskerTest extends MaskerTestHelper {

    @Inject
    Masker masker;

    @Test
    void shouldUseJsonMaskByDefaultMasker() {
        assertInstanceOf(JsonMaskByDefaultMasker.class, masker);
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
            "{\"specialId\":\"MySpecialId\",\"firstName\":\"xxxx\",\"lastName\":\"xxxx\",\"age\":\"xxxx\",\"status\":\"ACTIVE\",\"metadata\":{\"comments\":\"xxxx\",\"trusted\":true,\"expired\":\"xxxx\",\"rating\":\"A\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    void forUndefinedTopicShouldDefaultMaskAllValues() {
        Record record = sampleRecord(
            "different-topic",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"specialId\":\"xxxx\",\"firstName\":\"xxxx\",\"lastName\":\"xxxx\",\"age\":\"xxxx\",\"status\":\"xxxx\",\"metadata\":{\"comments\":\"xxxx\",\"trusted\":\"xxxx\",\"expired\":\"xxxx\",\"rating\":\"xxxx\"}}",
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

    @Test
    void forNonJsonValueThatLooksLikeJsonValueShouldReturnItself() {
        Record record = sampleRecord(
            "some-other-topic",
            "some-key",
            "{not a valid json}"
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

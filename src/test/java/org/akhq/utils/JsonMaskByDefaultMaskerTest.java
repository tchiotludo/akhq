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
            "users",
            "some-key",
            sampleValue()
        );

        Record maskedRecord = masker.maskRecord(record);

        assertEquals(
            "{\"specialId\":123,\"status\":\"ACTIVE\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"United Kingdom\"},\"metadata\":{\"trusted\":true,\"rating\":\"10\",\"notes\":\"xxxx\"}}",
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
            "{\"specialId\":\"xxxx\",\"status\":\"xxxx\",\"name\":\"xxxx\",\"dateOfBirth\":\"xxxx\",\"address\":{\"firstLine\":\"xxxx\",\"town\":\"xxxx\",\"country\":\"xxxx\"},\"metadata\":{\"trusted\":\"xxxx\",\"rating\":\"xxxx\",\"notes\":\"xxxx\"}}",
            maskedRecord.getValue()
        );
    }

    @Test
    void forTombstoneShouldReturnItself() {
        Record record = sampleRecord(
            "users",
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
            "users",
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
            "users",
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
               "specialId": 123,
               "status": "ACTIVE",
               "name": "John Smith",
               "dateOfBirth": "01-01-1991",
               "address": {
                 "firstLine": "123 Example Avenue",
                 "town": "Faketown",
                 "country": "United Kingdom"
               },
               "metadata": {
                 "trusted": true,
                 "rating": "10",
                 "notes": "All in good order"
               }
            }
            """;
    }
}
